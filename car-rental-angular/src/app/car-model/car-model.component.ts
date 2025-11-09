import {Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Cardetail, Offer} from '../cardetail';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-car-model',
  standalone: true,
  imports: [RouterLink, CommonModule],
  template: `
    <section class="listing">
          <img
            class="listing-photo"
            [src]="displayData?.photo || 'https://via.placeholder.com/400x250?text=Car+Image'"
            alt="Exterior photo of {{ displayData.brand }}"
            crossorigin
          />
          <h2 class="listing-heading">{{ displayData.brand }}</h2>
          <p class="listing-model">{{ displayData.model }}</p>
          <p class="listing-price">{{ getRentalPrice() }}€/day</p>
          <a [routerLink]="['/details', getModelId()]">Choose this model</a>
        </section>
      `,
      styleUrls: ['./car-model.component.css'],
    })
export class CarModelComponent {
  @Input() cardetail?: Cardetail;
  @Input() offer?: Offer;

  get displayData(): Offer | Cardetail {
    return this.offer || this.cardetail!;
  }

  getRentalPrice(): number {
    if (this.offer) {
      return this.offer.rentalPrice;
    }
    if (this.cardetail) {
      return this.cardetail.rentalPrice || this.cardetail.highestPrice || 0;
    }
    return 0;
  }

  getModelId(): number {
    if (this.offer) {
      return this.offer.carModelId;
    }
    if (this.cardetail && this.cardetail.id !== undefined) {
      return this.cardetail.id;
    }
    return 0;
  }
}
