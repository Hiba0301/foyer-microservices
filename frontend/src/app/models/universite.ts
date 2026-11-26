import { Foyer } from './foyer';

/** Miroir de l'entité Java Universite. */
export interface Universite {
  idUniversite?: number;
  nomUniversite: string;
  adresse: string;
  foyer?: Foyer | null;
}
