import { Injectable, inject, signal } from '@angular/core';
import {Cardetail} from './cardetail';
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

  getAllCarsByPlateNumber(plateNumber: string): Observable<Cardetail> {
    return this.http.get<Cardetail>(`${this.url}/${plateNumber}`).pipe(
      catchError(error => {
        console.error('Error fetching car by plate number:', error);
        throw error;
      })
    );
  }

  participateInAuction(brand: string, model: string, companyId: string = 'DEFAULT_COMPANY'): Observable<Cardetail> {
    const auctionUrl = `${environment.apiUrl}/auction/${brand}/${model}`;
    return this.http.post<Cardetail>(auctionUrl, null, {
      params: { companyId: companyId }
    }).pipe(
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
}
