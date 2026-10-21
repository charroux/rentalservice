export interface InsuranceOffer {
  rentalId: string;
  plateNumber: string;
  dailyPremium: number;
  status: 'PROPOSED' | 'ACCEPTED';
}

export interface RentalView {
  rentalId: string;
  projectionState: 'PARTIAL' | 'COMPLETE';
  rental?: {
    plateNumber: string;
    carBrand: string;
    carModel: string;
    finalPrice: number;
    originalPrice: number;
    discount: number;
  };
  insurance?: {
    plateNumber: string;
    dailyPremium: number;
    status: 'PROPOSED' | 'ACCEPTED';
  };
  updatedAt: string;
}
