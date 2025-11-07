import {Component, OnInit, OnDestroy, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RentalService} from '../rental.service';
import { Subscription } from 'rxjs';
import {CarDetailComponent} from '../car-detail/car-detail.component';
import {Cardetail} from '../cardetail';
import {WebSocketService} from '../websocket.service';

@Component({
  selector: 'app-car',
  standalone: true,
  imports: [CommonModule, CarDetailComponent],
  template: `
    <section>
          <form>
            <input type="text" placeholder="Filter by brand" #filter>
            <button class="primary" type="button" (click)="filterResults(filter.value)">Search</button>
          </form>
        </section>
        <section class="results">
        <app-car-detail *ngFor="let cardetail of filteredCarList" [cardetail]="cardetail"></app-car-detail>
    </section>
      `,
      styleUrls: ['./car.component.css'],
})
export class CarComponent implements OnInit {
  readonly baseUrl = 'https://angular.dev/assets/images/tutorials/common';
  cardetailList: Cardetail[] = [];
  filteredCarList: Cardetail[] = [];
  rentalService: RentalService = inject(RentalService);
  webSocketService: WebSocketService = inject(WebSocketService);
  private subscription!: Subscription;
  private wsSubscription!: Subscription;

  ngOnInit() {
    this.subscription = this.rentalService.getAllCars().subscribe(cars => { 
      this.cardetailList = cars; 
      this.filteredCarList = cars; 
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
      this.filteredCarList = this.cardetailList;
      return;
    }
    this.filteredCarList = this.cardetailList.filter(
      cardetail => cardetail?.brand.toLowerCase().includes(text.toLowerCase())
    );
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    this.wsSubscription?.unsubscribe();
    this.webSocketService.disconnect();
  }
}


