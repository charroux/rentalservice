export interface InsuranceOffer {
  rentalId: string;
  plateNumber: string;
  dailyPremium: number;
  status: 'PROPOSED' | 'ACCEPTED';
}
