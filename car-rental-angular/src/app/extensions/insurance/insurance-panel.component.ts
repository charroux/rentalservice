import {CommonModule} from '@angular/common';
import {Component, inject, Input, OnInit} from '@angular/core';
import {catchError, filter, finalize, of, switchMap, take, timer} from 'rxjs';
import {
  RentalExtensionComponent,
  RentalExtensionContext
} from '../../extension-platform/rental-extension';
import {InsuranceOffer} from './insurance.models';
import {InsuranceService} from './insurance.service';

@Component({
  selector: 'app-insurance-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="insurance-offer">
      <h3>Optional complementary insurance</h3>
      <p *ngIf="loading">Calculating your insurance offer…</p>
      <ng-container *ngIf="offer">
        <p>Additional coverage: <strong>{{ offer.dailyPremium }}€/day</strong></p>
        <button *ngIf="offer.status === 'PROPOSED'" (click)="accept()">
          Add complementary insurance
        </button>
        <p *ngIf="offer.status === 'ACCEPTED'">✅ Complementary insurance added</p>
      </ng-container>
      <p *ngIf="!loading && !offer">No insurance offer is currently available.</p>
    </section>
  `,
  styles: [`
    .insurance-offer {
      margin: 1.5rem 0;
      padding: 1rem;
      border-left: 4px solid #1976d2;
      background: #f5f9ff;
    }
    button {
      padding: 0.75rem 1rem;
      border: 0;
      border-radius: 4px;
      color: white;
      background: #1976d2;
      cursor: pointer;
    }
  `]
})
export class InsurancePanelComponent implements RentalExtensionComponent, OnInit {
  @Input({required: true}) context!: RentalExtensionContext;

  private readonly insurance = inject(InsuranceService);
  offer: InsuranceOffer | undefined;
  loading = false;

  ngOnInit(): void {
    this.loading = true;
    timer(0, 500).pipe(
      take(11),
      switchMap(() => this.insurance.getOffer(this.context.auctionResult.rentalId).pipe(
        catchError(() => of(null))
      )),
      filter((offer): offer is InsuranceOffer => offer !== null),
      take(1),
      finalize(() => this.loading = false)
    ).subscribe(offer => this.offer = offer);
  }

  accept(): void {
    this.insurance.accept(this.context.auctionResult.rentalId).subscribe({
      next: offer => this.offer = offer,
      error: error => {
        console.error('Unable to accept insurance offer:', error);
        alert('The insurance offer could not be accepted.');
      }
    });
  }
}
