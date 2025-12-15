import { Injectable, inject, signal } from '@angular/core';
import {Cardetail, Offer, AuctionResult} from './cardetail';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RentalService {

  readonly url = `${environment.apiUrl}/cars`;
  private http = inject(HttpClient);
  private cars = signal<Cardetail[]>([])


  constructor() { 
  }

  getAllCars(): Observable<Cardetail[]> {
    return this.http.get<Cardetail[]>(this.url).pipe(
      tap(cars => this.cars.set(cars)),
      catchError(error => {
        console.error('Error fetching cars:', error);
        return of([]);
      })
    );
  }

  // Nouvelle méthode utilisant l'endpoint /offers
  getOffers(): Observable<Offer[]> {
    const offersUrl = `${environment.apiUrl}/offers`;
    return this.http.get<Offer[]>(offersUrl).pipe(
      tap(offers => {
        console.log('Offers fetched:', offers);
        offers.forEach(offer => {
          console.log(`Offer ${offer.brand} ${offer.model} - Price: ${offer.rentalPrice}€/day`);
        });
      }),
      catchError(error => {
        console.error('Error fetching offers:', error);
        return of([]);
      })
    );
  }

  // Ancienne méthode - conservée pour compatibilité
  getAllCarModels(): Observable<Cardetail[]> {
    const carModelsUrl = `${environment.apiUrl}/car-models`;
    return this.http.get<Cardetail[]>(carModelsUrl).pipe(
      tap(carModels => {
        console.log('Car models fetched:', carModels);
        carModels.forEach(car => {
          console.log(`Car ${car.brand} ${car.model} - Price: ${car.rentalPrice}€/day`);
        });
      }),
      catchError(error => {
        console.error('Error fetching car models:', error);
        return of([]);
      })
    );
  }

  async getCardetailById(id: number): Promise<Cardetail> {
    const carModels = await this.getAllCarModels().toPromise();
    const cardetail = carModels?.find(car => car.id === id);
    if (!cardetail) {
      throw new Error(`Car model with id ${id} not found`);
    }
    return cardetail;
  }

  getCarMargin(carModelId: number, auctionFinalPrice: number): Observable<{hasMargin: boolean, margin: number}> {
    const marginUrl = `${environment.apiUrl}/car-margin/${carModelId}`;
    return this.http.get<{hasMargin: boolean, margin: number}>(marginUrl, {
      params: { auctionPrice: auctionFinalPrice.toString() }
    }).pipe(
      catchError(error => {
        console.error('Error fetching car margin:', error);
        return of({hasMargin: false, margin: 0});
      })
    );
  }  

  getAllCarsByPlateNumber(plateNumber: string): Observable<Cardetail> {
    return this.http.get<Cardetail>(`${this.url}/${plateNumber}`).pipe(
      catchError(error => {
        console.error('Error fetching car by plate number:', error);
        throw error;
      })
    );
  }

  participateInAuction(carModelId: number): Observable<AuctionResult> {
    const auctionUrl = `${environment.apiUrl}/auction/participate`;
    const body = { carModelId: carModelId };
    
    console.log('Participating in auction:', body);
    
    return this.http.post<AuctionResult>(auctionUrl, body).pipe(
      tap(response => {
        console.log('Auction response:', response);
        console.log(`Final price: ${response.finalCustomerPrice}€ (Original: ${response.originalPrice}€, Discount: ${response.discountAmount}€)`);
      }),
      catchError(error => {
        console.error('Error participating in auction:', error);
        throw error;
      })
    );
  }

  submitApplication(firstName: string, lastName: string, email: string, beginDate: string, endDate: string, plateNumber: string): void {
    console.log(
      `Homes application received: firstName: ${firstName}, lastName: ${lastName}, email: ${email}, beginDate: ${beginDate}, endDate: ${endDate}.`,
    );
  
    this.http.post(this.url + "/" + plateNumber, null, {
      params: { firstName: firstName, lastName: lastName, email: email, beginDate: beginDate, endDate: endDate },
      observe: 'response'
    }).subscribe(res => {
      console.log('Updated config:', res);
      console.log('Response status:', res.status);
    });
  }

  confirmRental(auctionResult: AuctionResult, offer: Offer, customerInfo: any): Observable<string> {
    const confirmUrl = `${environment.apiUrl}/rental/confirm`;
    const body = {
      plateNumber: auctionResult.plateNumber,
      brand: offer.brand,
      model: offer.model,
      carModelId: offer.carModelId,
      finalPrice: auctionResult.finalCustomerPrice,
      originalPrice: auctionResult.originalPrice,
      discountAmount: auctionResult.discountAmount,
      discountApplied: auctionResult.discountApplied,
      customerInfo: customerInfo
    };
    
    console.log('Confirming rental:', body);
    
    return this.http.post<string>(confirmUrl, body, {
      headers: new HttpHeaders({ 'Content-Type': 'application/json' })
    }).pipe(
      tap(response => {
        console.log('Rental confirmed:', response);
      }),
      catchError(error => {
        console.error('Error confirming rental:', error);
        throw error;
      })
    );
  }
}
