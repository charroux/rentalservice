import { Injectable, inject, signal } from '@angular/core';
import {Cardetail} from './cardetail';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RentalService {

  //url: string = 'http://localhost:3000/cars';
  readonly url = 'http://localhost:8080/cars';
  private http = inject(HttpClient);
  private cars = signal<Cardetail[]>([])


  constructor() { 
  }

  getAllCars(): Observable<Cardetail[]> {
    return this.http.get<Cardetail[]>(this.url).pipe(
      tap(cars => this.cars.set(cars)),
    );
  }  

  getAllCarsByPlateNumber(plateNumber: string): Observable<Cardetail> {
    return this.http.get<Cardetail>(`${this.url}/${plateNumber}`);
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
