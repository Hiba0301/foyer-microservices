/** Modèle de lecture CQRS (table foyer_view de foyer-service). */
export interface FoyerView {
  foyerId: string;
  nomFoyer: string;
  capaciteFoyer: number;
  dateCreation: string;
  derniereModification: string;
  nombreModifications: number;
}

/** Événement lu dans l'event store (Axon Server). */
export interface EvenementFoyer {
  sequence: number;
  type: string;
  date: string;
  donnees: Record<string, unknown>;
}
