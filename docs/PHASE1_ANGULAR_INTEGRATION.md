# Phase 1 CQRS: Intégration Angular Frontend

**Guide complet**: Comment l'interface Angular actuelle s'intègre avec l'architecture event-driven Phase 1.

---

## 1. Architecture End-to-End

```
┌──────────────────────────────────────┐
│   Angular Frontend (car-rental)      │
│   - CarsListComponent                │
│   - CarRentalComponent               │
│   - ValidateRentalComponent          │
└──────────────┬───────────────────────┘
               │
         ┌─────┴─────┐
         │           │
    ┌────▼──┐    ┌───▼──────┐
    │ REST  │    │ WebSocket│
    │ (REST)│    │ (gRPC)   │
    │ Calls │    │ Streaming│
    └────┬──┘    └───┬──────┘
         │           │
    ┌────▼───────────▼────────────────────────┐
    │  carRental Service (Spring Boot)        │
    │  ├─ AuctionEventPublisher               │
    │  │  └─ Publishes event to Redis         │
    │  ├─ AuctionEventConsumer                │
    │  │  └─ Polls and processes (internal)   │
    │  └─ REST Controllers                    │
    │     └─ Return DTOs to Angular           │
    └────┬───────────────────────────────────┘
         │
    ┌────▼───────────────────────────────────┐
    │  Redis + PostgreSQL                    │
    │  ├─ auction:events:queue (Redis)      │
    │  ├─ processed_events (PostgreSQL)      │
    │  └─ All other domain tables            │
    └────────────────────────────────────────┘
```

**Key Point**: Angular doesn't see events. Events are internal implementation detail.

---

## 2. Current Angular Flows (Unchanged)

### Flow 1: Display Catalog (GET /offers)

```typescript
// car-rental-angular/src/app/services/rental.service.ts
export class RentalService {
    constructor(private http: HttpClient) { }
    
    getOffers(): Observable<OfferDTO[]> {
        // Makes HTTP GET to backend
        return this.http.get<OfferDTO[]>('/api/offers');
    }
}

// Backend (carRental Service)
@RestController
@RequestMapping("/api")
public class CarRentalRestService {
    
    @GetMapping("/offers")
    public List<OfferDTO> getOffers() {
        // No events involved - just SELECT from database
        List<CarModelJPA> models = carModelService.getAllModels();
        return models.stream()
            .map(this::toOfferDTO)
            .collect(toList());
    }
    
    private OfferDTO toOfferDTO(CarModelJPA model) {
        return new OfferDTO(
            model.getId(),
            model.getBrand(),
            model.getModel(),
            model.getLowestPrice(),
            model.getHighestPrice(),
            "/assets/images/" + model.getBrand().toLowerCase() + ".jpg"
        );
    }
}

// Angular Template
// car-rental-angular/src/app/components/cars-list/cars-list.component.html
<div class="offers-grid">
    <mat-card *ngFor="let offer of offers$ | async" class="offer-card">
        <img [src]="offer.imagePath" />
        <h2>{{offer.brand}} {{offer.model}}</h2>
        <p class="price">€{{offer.lowestPrice}}/day</p>
        <button (click)="selectCar(offer.id)">View Details</button>
    </mat-card>
</div>

// TypeScript
export class CarsListComponent implements OnInit {
    offers$: Observable<OfferDTO[]>;
    
    constructor(private rentalService: RentalService) { }
    
    ngOnInit() {
        this.offers$ = this.rentalService.getOffers();
    }
    
    selectCar(offerId: string) {
        // Navigate to detail page
    }
}
```

**Status**: ✅ **No changes needed** - Events not involved.

---

### Flow 2: Participate in Auction (POST /auction/participate)

```typescript
// car-rental-angular/src/app/components/car-rental/car-rental.component.ts
export class CarRentalComponent implements OnInit {
    
    carDetail$: Observable<CarDetailDTO>;
    participatingInAuction = false;
    
    constructor(
        private rentalService: RentalService,
        private websocketService: WebSocketService
    ) { }
    
    ngOnInit() {
        const carModelId = this.route.snapshot.paramMap.get('id');
        this.carDetail$ = this.rentalService.getCarDetail(carModelId);
    }
    
    // User clicks "Participate in Auction"
    participateInAuction() {
        const carModelId = this.carDetail$.value.carModelId;
        
        this.participatingInAuction = true;
        
        // Make HTTP POST to backend
        this.rentalService.participateInAuction({
            carModelId: carModelId,
            carRentalCompanyId: 'COMPANY-1'  // Hardcoded for MVP
        }).subscribe({
            next: (result: AuctionResultDTO) => {
                // Auction completed
                this.participatingInAuction = false;
                this.winningCar = result.winningCar;  // Car with discount
                
                // Navigate to rental form
                this.router.navigate(['/validate', result.rentalId]);
            },
            error: (error) => {
                this.participatingInAuction = false;
                alert('Auction failed: ' + error.message);
            }
        });
    }
}

// Backend (carRental Service)
@RestController
@RequestMapping("/api")
public class CarRentalRestService {
    
    @PostMapping("/auction/participate")
    public AuctionResultDTO participateInAuction(
            @RequestBody AuctionRequestDTO request) {
        
        // Step 1: Call gRPC auction server (existing logic)
        Auction auction = auctionServiceClient.startAuction(
            request.getCarModelId(),
            request.getCarRentalCompanyId()
        );
        
        // Step 2: Create Car entity (existing logic)
        String plateNumber = generatePlateNumber();
        Car car = new Car();
        car.setCarModelId(request.getCarModelId());
        car.setPlateNumber(plateNumber);
        car.setRentalPrice(auction.getWinningBid());
        car.setFinalPrice(applyDiscount(auction.getWinningBid()));
        carRepository.save(car);
        
        // Step 3: PUBLISH EVENT to Redis (NEW - Phase 1)
        AuctionWonEvent event = new AuctionWonEvent(
            UUID.randomUUID().toString(),
            auction.getId(),
            request.getCarRentalCompanyId(),
            car.getCarModelId(),
            plateNumber,
            auction.getWinningBid()
        );
        
        auctionEventPublisher.publishAuctionWon(
            objectMapper.writeValueAsString(event)
        );
        
        // Step 4: Return result to Angular (existing logic)
        AuctionResultDTO result = new AuctionResultDTO();
        result.setRentalId(generateRentalId());
        result.setWinningCar(toCarDTO(car));
        result.setDiscountedPrice(car.getFinalPrice());
        
        return result;
    }
}

// Rental Service (for other services)
// The AuctionEventConsumer now processes the event asynchronously
// (No impact on Angular - it receives response immediately)
```

**Event Flow (Invisible to Angular)**:
```
1. Angular → POST /auction/participate
2. Backend publishes AuctionWonEvent to Redis
3. Response returned immediately to Angular
4. (Async) AuctionEventConsumer polls Redis
5. (Async) AuctionEventConsumer records in processed_events
6. (Async) In Phase 2: Other services process event
   - InsuranceService: Create quote
   - AnalyticsService: Update metrics
```

**Status**: ✅ **Compatible** - Angular sees no latency from events.

---

### Flow 3: Submit Rental Form (POST /cars/{plateNumber})

```typescript
// car-rental-angular/src/app/components/validate-rental/validate-rental.component.ts
export class ValidateRentalComponent implements OnInit {
    
    rentalForm: FormGroup;
    carDetail: CarDetailDTO;
    
    ngOnInit() {
        const plateNumber = this.route.snapshot.paramMap.get('plateNumber');
        
        // Load car details
        this.rentalService.getCarByPlateNumber(plateNumber)
            .subscribe(car => this.carDetail = car);
    }
    
    submitRental() {
        const plateNumber = this.carDetail.plateNumber;
        
        const rentalData = {
            customerName: this.rentalForm.value.customerName,
            customerEmail: this.rentalForm.value.email,
            startDate: this.rentalForm.value.startDate,
            endDate: this.rentalForm.value.endDate,
            insuranceAccepted: this.rentalForm.value.insurance
        };
        
        // Submit rental
        this.rentalService.submitRental(plateNumber, rentalData)
            .subscribe({
                next: (contract: RentalContractDTO) => {
                    alert('✓ Rental confirmed! Contract: ' + contract.rentalId);
                    this.router.navigate(['/confirmation', contract.rentalId]);
                },
                error: (error) => {
                    alert('❌ Rental submission failed: ' + error.message);
                }
            });
    }
}

// Backend (carRental Service)
@RestController
@RequestMapping("/api")
public class CarRentalRestService {
    
    @PostMapping("/cars/{plateNumber}")
    public RentalContractDTO submitRental(
            @PathVariable String plateNumber,
            @RequestBody RentalSubmissionDTO submission) {
        
        // Step 1: Load car (existing logic)
        Car car = carRepository.findByPlateNumber(plateNumber)
            .orElseThrow(() -> new NotFoundException("Car not found"));
        
        // Step 2: Create rental contract (existing logic)
        RentalContract contract = new RentalContract();
        contract.setCustomerName(submission.getCustomerName());
        contract.setCustomerEmail(submission.getCustomerEmail());
        contract.setCarId(car.getId());
        contract.setStartDate(submission.getStartDate());
        contract.setEndDate(submission.getEndDate());
        contract.setStatus("CONFIRMED");
        
        rentalContractRepository.save(contract);
        
        // Step 3: Update car status (existing logic)
        car.setStatus("RENTED");
        carRepository.save(car);
        
        // Step 4: Return contract DTO to Angular
        return toContractDTO(contract);
    }
}

// ✅ No events needed here - this is traditional CRUD
// (Events only for inter-service communication, not for single service CRUD)
```

**Status**: ✅ **No changes needed** - Events not involved.

---

## 3. How Events Help (Phase 2+)

### Scenario: Add InsuranceService Integration

**Phase 1 (NOW)**: No insurance service visible to Angular

```typescript
// car-rental-angular/src/app/components/validate-rental/validate-rental.component.ts
// Insurance option hidden or hardcoded
```

**Phase 2 (Future)**: After event flow stabilizes, add insurance quotes

```typescript
// car-rental-angular/src/app/services/rental.service.ts
export class RentalService {
    
    // New method: Get insurance quote for car
    getInsuranceQuote(carBrand: string, carModel: string): Observable<InsuranceQuoteDTO> {
        return this.http.get<InsuranceQuoteDTO>(
            `/api/insurance/quote?carBrand=${carBrand}&carModel=${carModel}`
        );
    }
}

// car-rental-angular/src/app/components/validate-rental/validate-rental.component.ts
ngOnInit() {
    // Load insurance quote (created by InsuranceEventConsumer)
    this.rentalService.getInsuranceQuote(
        this.carDetail.brand,
        this.carDetail.model
    ).subscribe(quote => {
        this.insuranceQuote = quote;
        // Display: "Comprehensive coverage: €80/day"
    });
}

submitRental() {
    // Include insurance in submission
    const rentalData = {
        ...this.rentalForm.value,
        insuranceAccepted: this.rentalForm.value.insurance,
        insuranceQuoteId: this.insuranceQuote.id  // NEW - from event
    };
    
    this.rentalService.submitRental(
        this.carDetail.plateNumber,
        rentalData
    ).subscribe(...);
}
```

**How it Works (Phase 2)**:
1. User clicks "Participate in Auction"
2. Backend publishes AuctionWonEvent
3. InsuranceService consumes event (via InsuranceEventConsumer)
4. Creates InsuranceQuote in its database
5. **Later**: Angular makes GET /api/insurance/quote request
6. Backend returns quote from InsuranceService database
7. User sees insurance option in form

**Key**: Events are **asynchronous** and **invisible**. Angular just sees new API endpoints.

---

## 4. WebSocket Integration (Real-time Auction Updates)

**Existing**: Auction updates via gRPC streaming

```typescript
// car-rental-angular/src/app/services/websocket.service.ts
export class WebSocketService {
    
    subscribeToAuctionUpdates(auctionId: string): Observable<BidUpdateDTO> {
        // Uses gRPC streaming from auctionServiceServer
        // Real-time bid updates every 500ms
    }
}

// car-rental-angular/src/app/components/car-rental/car-rental.component.html
<div *ngIf="(auctionUpdates$ | async) as update" class="auction-status">
    <h3>Current Bid: €{{update.currentBid}}</h3>
    <p>Time Left: {{update.secondsRemaining}}s</p>
    <progress [value]="update.secondsRemaining" max="5"></progress>
</div>
```

**Status**: ✅ **No changes** - gRPC streaming independent of events.

---

## 5. Future: Real-time Event Notifications (Phase 3+)

**Future Use Case**: Notify user when insurance quote is ready

```typescript
// Phase 3: WebSocket for event notifications
export class NotificationService {
    
    // Listen for insurance quote notifications
    subscribeToInsuranceUpdates(): Observable<InsuranceNotificationDTO> {
        return new Observable(observer => {
            const stompClient = new Stomp.Client({
                brokerURL: 'ws://localhost:8080/ws'
            });
            
            stompClient.onConnect = () => {
                stompClient.subscribe('/topic/insurance-quotes', message => {
                    const notification = JSON.parse(message.body);
                    observer.next(notification);
                });
            };
            
            stompClient.activate();
        });
    }
}

// Backend (NotificationService - Phase 3)
@Component
public class InsuranceNotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final InsuranceEventConsumer insuranceEventConsumer;
    
    public void onInsuranceQuoteCreated(InsuranceQuote quote) {
        // Send WebSocket notification to connected Angular clients
        messagingTemplate.convertAndSend("/topic/insurance-quotes", 
            new InsuranceNotificationDTO(
                quote.getCustomerId(),
                quote.getDailyPremium(),
                quote.getCarBrand()
            )
        );
    }
}

// Angular Component
export class ValidateRentalComponent {
    
    ngOnInit() {
        // Subscribe to real-time insurance updates
        this.notificationService.subscribeToInsuranceUpdates()
            .subscribe(notification => {
                this.showInsuranceQuote(notification);
            });
    }
}
```

**Status**: ✅ **Future enhancement** - Not needed for Phase 1.

---

## 6. Architecture Diagram (Angular ↔ Event System)

```
┌────────────────────────────────────────────────────────────────┐
│                     ANGULAR FRONTEND                           │
│  (User Browser)                                                │
│                                                                │
│  ┌──────────────────────┐   ┌──────────────────────────┐      │
│  │ CarsListComponent    │   │ CarRentalComponent       │      │
│  │ (GET /offers)        │   │ (POST /auction/part...)  │      │
│  └──────────────────────┘   └──────────────────────────┘      │
│           │                               │                    │
└───────────┼───────────────────────────────┼────────────────────┘
            │                               │
            │  REST/HTTP                    │
            ▼                               ▼
┌────────────────────────────────────────────────────────────────┐
│                CARENTAL SERVICE (Spring Boot)                  │
│                                                                │
│  REST Controllers                                              │
│  ├─ GET /offers → select * from CarModelJPA                   │
│  ├─ POST /auction/participate → gRPC call                     │
│  └─ POST /cars/{plate} → create RentalContract               │
│           │                                                    │
│  ┌────────▼─────────────────────────────────────────┐         │
│  │  AuctionEventPublisher (Phase 1 NEW)             │         │
│  │  ├─ Serialize event to JSON                      │         │
│  │  └─ Redis LPUSH → auction:events:queue           │         │
│  └────────┬─────────────────────────────────────────┘         │
│           │                                                    │
│  ┌────────▼─────────────────────────────────────────┐         │
│  │  AuctionEventConsumer (Phase 1 NEW)              │         │
│  │  @Scheduled(1000ms)                              │         │
│  │  ├─ Redis LPOP                                   │         │
│  │  ├─ Check idempotence (ProcessedEvent table)     │         │
│  │  ├─ Process event (log for Phase 1)              │         │
│  │  └─ UPDATE processed_events                      │         │
│  └────────────────────────────────────────────────────┘         │
│           │                                                    │
└───────────┼────────────────────────────────────────────────────┘
            │
            │  (Events invisible to Angular)
            │
┌───────────▼────────────────────────────────────────────────────┐
│               EXTERNAL SERVICES (Future)                       │
│                                                                │
│  ┌────────────────────────┐  ┌─────────────────────────┐      │
│  │ InsuranceService       │  │ AnalyticsService        │      │
│  │ (Phase 2+)             │  │ (Phase 2+)              │      │
│  │                        │  │                         │      │
│  │ InsuranceEventConsumer │  │ AnalyticsEventConsumer  │      │
│  │ - Poll Redis queue     │  │ - Poll Redis queue      │      │
│  │ - Create quotes        │  │ - Accumulate metrics    │      │
│  └────────────────────────┘  └─────────────────────────┘      │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

**Key Insight**: Events flow **horizontally** between services, not vertically to Angular.

---

## 7. Deployment Architecture (Docker Compose)

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: car_rental
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  # Phase 1: Only carRental service
  car-rental:
    build:
      context: ./carRental
      dockerfile: Dockerfile
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/car_rental
      SPRING_REDIS_HOST: redis
      AUCTION_SERVICE_HOST: auction-service
      AUCTION_SERVICE_PORT: 50051
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
      - auction-service
  
  # gRPC Auction Service (existing)
  auction-service:
    build:
      context: ./auctionServiceServer
      dockerfile: Dockerfile
    ports:
      - "50051:50051"
  
  # Phase 2: Add insurance service
  # insurance-service:
  #   build:
  #     context: ./insurance-service
  #     dockerfile: Dockerfile
  #   environment:
  #     SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/car_rental
  #     SPRING_REDIS_HOST: redis
  #   ports:
  #     - "8081:8081"
  #   depends_on:
  #     - postgres
  #     - redis
  
  # Angular frontend
  angular-app:
    build:
      context: ./car-rental-angular
      dockerfile: Dockerfile
    ports:
      - "4200:4200"
    depends_on:
      - car-rental

volumes:
  postgres_data:
```

**Scale Up (Phase 2+)**: Just uncomment `insurance-service` section and rebuild.

---

## 8. Summary: Angular Integration Status

| Aspect | Phase 1 | Phase 2+ |
|--------|---------|---------|
| **Catalog Display** | ✅ Unchanged | ✅ Same |
| **Auction Participation** | ✅ Works | ✅ Faster (events async) |
| **Rental Form** | ✅ Unchanged | ✅ Same |
| **Insurance Quotes** | ❌ Not available | ✅ From InsuranceService |
| **Real-time Updates** | ✅ gRPC streaming | ✅ + WebSocket notifications |
| **User Experience** | ✅ Normal | ✅ Better (populated quotes) |

**Bottom Line**: 
- ✅ Phase 1 is **fully compatible** with Angular frontend
- ✅ Angular needs **zero changes** for Phase 1
- ✅ Phase 2 will **enhance** Angular with new features (insurance, analytics)
- ✅ Events are **internal implementation detail**

---

**End-to-End Viability**: ✅ **CONFIRMED**
