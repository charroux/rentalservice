import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {environment} from '../../../environments/environment';
import {InsuranceOffer} from './insurance.models';

@Injectable({providedIn: 'root'})
export class InsuranceService {
  private readonly http = inject(HttpClient);

  getOffer(rentalId: string): Observable<InsuranceOffer> {
    return this.http.get<InsuranceOffer>(
      `${environment.insuranceApiUrl}/insurance/offers/${rentalId}`
    );
  }

  accept(rentalId: string): Observable<InsuranceOffer> {
    return this.http.post<InsuranceOffer>(
      `${environment.insuranceApiUrl}/insurance/offers/${rentalId}/accept`,
      null
    );
  }
}
