/** Miroir de l'entité Java Foyer (foyer-service). */
export interface Foyer {
  idFoyer?: number;
  nomFoyer: string;
  capaciteFoyer: number;
  /** false quand universite-service a renvoyé une valeur de secours (fallback Resilience4j). */
  disponible?: boolean;
}
