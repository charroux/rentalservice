import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {catchError, defaultIfEmpty, filter, map, Observable, of, switchMap, take, timer} from 'rxjs';
import {environment} from '../../../environments/environment';
import {InsuranceOffer, RentalView} from './insurance.models';

@Injectable({providedIn: 'root'})
export class InsuranceService {
  private readonly http = inject(HttpClient);

  getOffer(rentalId: string): Observable<InsuranceOffer> {
    return this.http.get<RentalView>(
      `${environment.rentalQueryApiUrl}/rental-views/${rentalId}`
    ).pipe(
      filter(view => !!view.insurance),
      map(view => ({
        rentalId: view.rentalId,
        plateNumber: view.rental?.plateNumber ?? view.insurance!.plateNumber,
        dailyPremium: view.insurance!.dailyPremium,
        status: view.insurance!.status
      }))
    );
  }

  accept(rentalId: string): Observable<InsuranceOffer> {
    return this.http.post<InsuranceOffer>(
      `${environment.insuranceApiUrl}/insurance/offers/${rentalId}/accept`,
      null
    ).pipe(
      switchMap(commandResult => timer(0, 500).pipe(
        take(11),
        switchMap(() => this.getOffer(rentalId).pipe(catchError(() => of(null)))),
        filter((offer): offer is InsuranceOffer => offer?.status === 'ACCEPTED'),
        take(1),
        defaultIfEmpty(commandResult)
      ))
    );
  }
}
