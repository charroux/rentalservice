import {AuctionResult, Offer} from '../cardetail';

export interface RentalExtensionContext {
  auctionResult: AuctionResult;
  offer?: Offer;
  customerInfo?: unknown;
}

export interface RentalExtensionComponent {
  context: RentalExtensionContext;
}
