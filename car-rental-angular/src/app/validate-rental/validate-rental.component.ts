import {Component, inject, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {RentalService} from '../rental.service';
import {Cardetail, Offer, AuctionResult} from '../cardetail';

@Component({
  selector: 'app-validate-rental',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article>
      <img
        class="listing-photo"
        [src]="offer?.photo || cardetail?.photo"
        alt="Exterior photo of {{ offer?.brand || cardetail?.brand }}"
        crossorigin
      />
      <section class="listing-description">
        <h2 class="listing-heading">{{ offer?.brand || cardetail?.brand }} {{ offer?.model || cardetail?.model }}</h2>
        <p class="plate-number" *ngIf="auctionResult?.plateNumber">
          🎉 Car assigned: {{ auctionResult?.plateNumber }}
        </p>
      </section>
      
      <section class="pricing-details" *ngIf="auctionResult">
        <h2 class="section-heading">Auction Results</h2>
        <div class="price-breakdown">
          <div class="price-row">
            <span>Original Rental Price:</span>
            <span class="price original">{{ auctionResult?.originalPrice }}€/day</span>
          </div>
          <div class="price-row" *ngIf="auctionResult?.discountApplied">
            <span>Discount Applied:</span>
            <span class="price discount">-{{ auctionResult?.discountAmount }}€/day</span>
          </div>
          <div class="price-row final">
            <span><strong>Final Customer Price:</strong></span>
            <span class="price final"><strong>{{ auctionResult?.finalCustomerPrice }}€/day</strong></span>
          </div>
        </div>

        <div class="discount-applied" *ngIf="auctionResult?.discountApplied">
          <h3>🎁 Discount Applied!</h3>
          <p class="discount-message">
            Congratulations! We were able to apply a {{ auctionResult?.discountAmount }}€/day discount 
            based on our auction results and available margin.
          </p>
          <div class="savings-highlight">
            <span class="original-price">Was: {{ auctionResult?.originalPrice }}€/day</span>
            <span class="final-price">Now: {{ auctionResult?.finalCustomerPrice }}€/day</span>
            <span class="savings">You save {{ auctionResult?.discountAmount }}€/day!</span>
          </div>
        </div>

        <div class="no-discount" *ngIf="!auctionResult?.discountApplied">
          <h3>Final Rental Price</h3>
          <p>{{ auctionResult?.finalCustomerPrice }}€/day</p>
          <p class="no-discount-message">
            Our auction provided the best possible price for this vehicle.
          </p>
        </div>

        <div class="customer-info" *ngIf="customerInfo">
          <h3>Customer Information</h3>
          <p><strong>Name:</strong> {{ customerInfo.firstName }} {{ customerInfo.lastName }}</p>
          <p><strong>Email:</strong> {{ customerInfo.email }}</p>
        </div>

        <div class="action-buttons">
          <button class="primary" (click)="confirmRental()">
            Confirm Rental at {{ auctionResult?.finalCustomerPrice }}€/day
          </button>
          <button class="secondary" (click)="goBack()">
            Go Back to Car Selection
          </button>
        </div>
      </section>
    </article>
  `,
  styleUrls: ['./validate-rental.component.css']
})
export class ValidateRentalComponent implements OnInit {
  route: ActivatedRoute = inject(ActivatedRoute);
  router: Router = inject(Router);
  rentalService = inject(RentalService);
  
  offer: Offer | undefined;
  auctionResult: AuctionResult | undefined;
  customerInfo: any;
  // Conservé pour compatibilité template
  cardetail: Cardetail | undefined;
  assignedCar: any;

  ngOnInit() {
    const carModelId = parseInt(this.route.snapshot.params['id'], 10);
    const plateNumber = this.route.snapshot.params['plateNumber'];
    
    // Récupérer les données transmises par le router
    const navigationState = history.state;
    if (navigationState && navigationState.auctionResult) {
      this.auctionResult = navigationState.auctionResult;
      this.offer = navigationState.offer;
      this.customerInfo = navigationState.customerInfo;
      
      // Créer cardetail pour compatibilité template
      if (this.offer) {
        this.cardetail = {
          id: this.offer.carModelId,
          brand: this.offer.brand,
          model: this.offer.model,
          photo: this.offer.photo,
          rentalPrice: this.offer.rentalPrice
        };
      }
      
      // Créer assignedCar pour compatibilité template
      if (this.auctionResult) {
        this.assignedCar = {
          plateNumber: this.auctionResult.plateNumber,
          finalPrice: this.auctionResult.finalCustomerPrice
        };
      }
    } else {
      // Fallback : essayer de récupérer les données depuis le serveur
      console.log('No navigation state found, redirecting to home');
      this.router.navigate(['/']);
    }
  }

  confirmRental() {
    if (this.auctionResult && this.offer && this.customerInfo) {
      console.log(`Rental confirmed at ${this.auctionResult.finalCustomerPrice}€/day for car ${this.auctionResult.plateNumber}`);
      console.log('Customer:', this.customerInfo);
      
      const plateNumber = this.auctionResult.plateNumber;
      const finalPrice = this.auctionResult.finalCustomerPrice;
      
      // Appel API pour sauvegarder la confirmation de location et publier l'événement Kafka
      this.rentalService.confirmRental(this.auctionResult, this.offer, this.customerInfo).subscribe({
        next: (response) => {
          console.log('Rental confirmation response:', response);
          alert(`Rental confirmed!\nCar: ${plateNumber}\nFinal price: ${finalPrice}€/day`);
          // Rediriger vers la page d'accueil
          this.router.navigate(['/']);
        },
        error: (error) => {
          console.error('Error confirming rental:', error);
          alert('Error confirming rental. Please try again.');
        }
      });
    } else {
      console.error('Missing required data for rental confirmation');
      alert('Error: Missing rental information');
    }
  }

  goBack() {
    // Retourner à la liste des voitures
    this.router.navigate(['/']);
  }
}