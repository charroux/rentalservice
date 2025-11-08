import {Component, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute} from '@angular/router';
import {RentalService} from '../rental.service';
import {Cardetail} from '../cardetail';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';

@Component({
  selector: 'app-details',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  template: `
  <article>
      <img
        class="listing-photo"
        [src]="cardetail?.photo"
        alt="Exterior photo of {{ cardetail?.brand }}"
        crossorigin
      />
      <section class="listing-description">
        <h2 class="listing-heading">{{ cardetail?.brand }}</h2>
      </section>
      <section class="listing-features">
        <h2 class="section-heading">About this car</h2>
        <ul>
          <li>Rental price: {{ cardetail?.rentalPrice }} €/day</li>
        </ul>
      </section>
      <section class="listing-apply">
        <h2 class="section-heading">Apply now to rent it</h2>
        <form [formGroup]="applyForm" (submit)="submitApplication()">
          <label for="first-name">First Name</label>
          <input id="first-name" type="text" formControlName="firstName" />
          <label for="last-name">Last Name</label>
          <input id="last-name" type="text" formControlName="lastName" />
          <label for="email">Email</label>
          <input id="email" type="email" formControlName="email" />
          <label for="begin-date">Begin Date</label>
          <input id="begin-date" type="text" formControlName="beginDate" />
          <label for="end-date">End Date</label>
          <input id="end-date" type="text" formControlName="endDate" />
          <button type="submit" class="primary">Apply now</button>
        </form>
      </section>
    </article>
  `,
  styleUrls: ['./details.component.css'],
  styles: ``
})
export class DetailsComponent {
  route: ActivatedRoute = inject(ActivatedRoute);
  rentalService = inject(RentalService);
  cardetail: Cardetail | undefined;
  applyForm = new FormGroup({
    firstName: new FormControl(''),
    lastName: new FormControl(''),
    email: new FormControl(''),
    beginDate: new FormControl(''),
    endDate: new FormControl(''),
  });

  constructor() {
    const cardetailPlateNumber = String(this.route.snapshot.params['plateNumber']);
    this.rentalService.getAllCarsByPlateNumber(cardetailPlateNumber).subscribe(cardetail => {
      this.cardetail = cardetail;
    });
  }

  submitApplication() {
    this.rentalService.submitApplication(
      this.applyForm.value.firstName ?? '',
      this.applyForm.value.lastName ?? '',
      this.applyForm.value.email ?? '',
      this.applyForm.value.beginDate ?? '',
      this.applyForm.value.endDate ?? '',
      this.cardetail?.plateNumber ?? ''
    );
  }
}
