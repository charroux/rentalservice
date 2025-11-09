import {Component, OnInit, OnDestroy, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RentalService} from '../rental.service';
import { Subscription } from 'rxjs';
import {CarModelComponent} from '../car-model/car-model.component';
import {Cardetail, Offer} from '../cardetail';
import {WebSocketService} from '../websocket.service';

@Component({
  selector: 'app-cars-list',
  standalone: true,
  imports: [CommonModule, CarModelComponent],
  template: `
    <section>
          <form>
            <input type="text" placeholder="Filter by brand" #filter>
            <button class="primary" type="button" (click)="filterResults(filter.value)">Search</button>
          </form>
        </section>
        <section class="results">
        <app-car-model *ngFor="let offer of filteredOfferList" [offer]="offer"></app-car-model>
    </section>
      `,
      styleUrls: ['./cars-list.component.css'],
})
export class CarsListComponent implements OnInit {
  readonly baseUrl = 'https://angular.dev/assets/images/tutorials/common';
  offerList: Offer[] = [];
  filteredOfferList: Offer[] = [];
  // Conservé pour compatibilité WebSocket
  cardetailList: Cardetail[] = [];
  filteredCarList: Cardetail[] = [];
  rentalService: RentalService = inject(RentalService);
  webSocketService: WebSocketService = inject(WebSocketService);
  private subscription!: Subscription;
  private wsSubscription!: Subscription;

  ngOnInit() {
    // Charger les offres avec prix pour l'affichage
    this.subscription = this.rentalService.getOffers().subscribe(offers => { 
      console.log('Offers received:', offers);
      this.offerList = offers; 
      this.filteredOfferList = offers; 
    });

    // Connect to WebSocket
    this.webSocketService.connect();

    // Subscribe to real-time updates
    this.wsSubscription = this.webSocketService.getPlateNumberUpdates().subscribe(update => {
      if (update) {
        // Update the car in the lists if it exists
        this.updateCarInLists(update);
      }
    });
  }

  private updateCarInLists(updatedCar: Cardetail) {
    // Update in main list
    const mainIndex = this.cardetailList.findIndex(car => car.plateNumber === updatedCar.plateNumber);
    if (mainIndex !== -1) {
      this.cardetailList[mainIndex] = { ...this.cardetailList[mainIndex], ...updatedCar };
    }

    // Update in filtered list
    const filteredIndex = this.filteredCarList.findIndex(car => car.plateNumber === updatedCar.plateNumber);
    if (filteredIndex !== -1) {
      this.filteredCarList[filteredIndex] = { ...this.filteredCarList[filteredIndex], ...updatedCar };
    }
  }
  
  filterResults(text: string) {
    console.log(text);
    if (!text) {
      this.filteredOfferList = this.offerList;
      return;
    }
    this.filteredOfferList = this.offerList.filter(
      offer => offer?.brand.toLowerCase().includes(text.toLowerCase())
    );
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    this.wsSubscription?.unsubscribe();
    this.webSocketService.disconnect();
  }
}


