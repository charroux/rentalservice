import {Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Cardetail} from '../cardetail';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-car-detail',
  standalone: true,
  imports: [RouterLink, CommonModule],
  template: `
    <section class="listing">
          <img
            class="listing-photo"
            [src]="cardetail.photo"
            alt="Exterior photo of {{ cardetail.brand }}"
            crossorigin
          />
          <h2 class="listing-heading">{{ cardetail.brand }}</h2>
          <p class="listing-location">{{ cardetail.rentalPrice }} €/day</p>
          <p class="plate-number" [class.updated]="isUpdated">Plate: {{ cardetail.plateNumber }}</p>
          <a [routerLink]="['/details', cardetail.plateNumber]">Learn More</a>
        </section>
      `,
      styleUrls: ['./car-detail.component.css'],
    })
export class CarDetailComponent {
  @Input() set cardetail(value: Cardetail) {
    if (this._cardetail && this._cardetail.plateNumber === value.plateNumber) {
      this.isUpdated = true;
      setTimeout(() => this.isUpdated = false, 1000);
    }
    this._cardetail = value;
  }
  get cardetail(): Cardetail {
    return this._cardetail;
  }
  private _cardetail!: Cardetail;
  isUpdated = false;
}
