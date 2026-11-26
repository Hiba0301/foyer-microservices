import { Foyer } from './foyer';

/** Réponse de bloc-service : le bloc et son foyer (lu dans foyer-service). */
export interface Bloc {
  idBloc: number;
  nomBloc: string;
  capaciteBloc: number;
  foyer?: Foyer | null;
}
