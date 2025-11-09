export interface Cardetail {
    id?: number;            // ID pour les car models
    plateNumber?: string;   // Pour les voitures spécifiques après enchères
    brand: string;
    model: string;
    rentalPrice?: number;   // Prix de location en €/jour (calculé côté frontend)
    highestPrice?: number;  // Prix le plus élevé du modèle
    lowestPrice?: number;   // Prix le plus bas du modèle
    photo?: string;         // URL de la photo (optionnelle)
}

// Nouvelle interface pour les offres (remplace progressivement Cardetail)
export interface Offer {
    carModelId: number;
    brand: string;
    model: string;
    photo?: string;
    rentalPrice: number;    // Prix affiché à l'utilisateur (basé sur highestPrice)
}

// Interface pour le résultat d'enchère
export interface AuctionResult {
    plateNumber: string;
    finalCustomerPrice: number;
    originalPrice: number;
    discountAmount: number;
    discountApplied: boolean;
}
