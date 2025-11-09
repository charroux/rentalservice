import {Component, inject, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {RentalService} from '../rental.service';
import {Cardetail, Offer, AuctionResult} from '../cardetail';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';

@Component({
  selector: 'app-car-rental',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  template: `
  <article>
      <img
        class="listing-photo"
        [src]="cardetail?.photo || 'https://via.placeholder.com/600x400?text=' + cardetail?.brand + '+' + cardetail?.model"
        alt="Exterior photo of {{ cardetail?.brand }}"
        crossorigin
      />
      <section class="listing-description">
        <h2 class="listing-heading">{{ cardetail?.brand }} {{ cardetail?.model }}</h2>
        <p class="rental-price">Initial Price: <strong>{{ cardetail?.rentalPrice || cardetail?.highestPrice || 0 }}€/day</strong></p>
      </section>
      <section class="listing-features">
        <h2 class="section-heading">About this car model</h2>
        <ul>
          <li>Brand: {{ cardetail?.brand }}</li>
          <li>Model: {{ cardetail?.model }}</li>
          <li>Base rental price: {{ cardetail?.rentalPrice || cardetail?.highestPrice || 0 }}€/day</li>
          <li>Price range: {{ cardetail?.lowestPrice || 0 }}€ - {{ cardetail?.highestPrice || 0 }}€/day</li>
          <li>✨ Participate in auction to get your specific car</li>
        </ul>
      </section>
      <section class="listing-apply">
        <h2 class="section-heading">Participate in auction for this car</h2>
        <form [formGroup]="applyForm" (submit)="submitApplication()">
          <label for="first-name">First Name</label>
          <input id="first-name" type="text" formControlName="firstName" />
          <label for="last-name">Last Name</label>
          <input id="last-name" type="text" formControlName="lastName" />
          <label for="email">Email</label>
          <input id="email" type="email" formControlName="email" />
          <button type="submit" class="primary">Participate in Auction</button>
        </form>
      </section>
    </article>
  `,
  styleUrls: ['./car-rental.component.css']
})
export class CarRentalComponent implements OnInit {
  route: ActivatedRoute = inject(ActivatedRoute);
  router: Router = inject(Router);
  rentalService = inject(RentalService);
  offer: Offer | undefined;
  // Conservé pour rétro-compatibilité avec le template existant
  cardetail: Cardetail | undefined;
  applyForm = new FormGroup({
    firstName: new FormControl(''),
    lastName: new FormControl(''),
    email: new FormControl(''),
  });

  ngOnInit() {
    const carModelId = parseInt(this.route.snapshot.params['id'], 10);
    
    // Charger l'offre correspondante depuis le serveur
    this.rentalService.getOffers().subscribe(offers => {
      this.offer = offers.find(offer => offer.carModelId === carModelId);
      
      // Pour la compatibilité du template, créer un objet cardetail basé sur l'offre
      if (this.offer) {
        this.cardetail = {
          id: this.offer.carModelId,
          brand: this.offer.brand,
          model: this.offer.model,
          photo: this.offer.photo,
          rentalPrice: this.offer.rentalPrice,
          // Valeurs par défaut pour les champs manquants
          lowestPrice: this.offer.rentalPrice * 0.8, // estimation
          highestPrice: this.offer.rentalPrice * 1.2 // estimation
        };
      }
    });
  }

  submitApplication() {
    if (this.offer && this.cardetail) {
      console.log('Starting auction for car model:', this.offer.brand, this.offer.model);
      
      // Utiliser la nouvelle API d'enchères qui prend seulement carModelId
      this.rentalService.participateInAuction(this.offer.carModelId).subscribe({
        next: (auctionResult: AuctionResult) => {
          console.log('Auction participation response:', auctionResult);
          console.log(`Final price: ${auctionResult.finalCustomerPrice}€ (Original: ${auctionResult.originalPrice}€)`);
          
          if (auctionResult.discountApplied) {
            console.log(`Discount applied: ${auctionResult.discountAmount}€`);
          }
          
          // Rediriger vers la page de validation avec les résultats de l'enchère
          this.router.navigate(['/validate', this.offer!.carModelId, auctionResult.plateNumber], {
            state: {
              auctionResult: auctionResult,
              offer: this.offer,
              customerInfo: {
                firstName: this.applyForm.value.firstName,
                lastName: this.applyForm.value.lastName,
                email: this.applyForm.value.email
              }
            }
          });
        },
        error: (error) => {
          console.error('Error participating in auction:', error);
          alert('Erreur lors de la participation à l\'enchère. Veuillez réessayer.');
        }
      });
    }
  }
}
