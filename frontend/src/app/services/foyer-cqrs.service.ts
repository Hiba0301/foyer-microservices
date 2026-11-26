import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { EvenementFoyer, FoyerView } from '../models/foyer-view';

/** API CQRS de foyer-service : les écritures (commandes) et les lectures (requêtes) sont séparées. */
@Injectable({ providedIn: 'root' })
export class FoyerCqrsService {
  private readonly http = inject(HttpClient);
  private readonly commandsUrl = `${environment.apiUrl}/api/foyers/cqrs/commands`;
  private readonly queriesUrl = `${environment.apiUrl}/api/foyers/cqrs/queries`;

  // ---- Commandes (écriture)
  creer(nomFoyer: string, capaciteFoyer: number): Observable<{ foyerId: string; statut: string }> {
    return this.http.post<{ foyerId: string; statut: string }>(this.commandsUrl, { nomFoyer, capaciteFoyer });
  }

  modifierCapacite(foyerId: string, valeur: number): Observable<unknown> {
    const params = new HttpParams().set('valeur', valeur);
    return this.http.put(`${this.commandsUrl}/${foyerId}/capacite`, null, { params });
  }

  supprimer(foyerId: string): Observable<unknown> {
    return this.http.delete(`${this.commandsUrl}/${foyerId}`);
  }

  // ---- Requêtes (lecture)
  lister(capaciteMin = 0): Observable<FoyerView[]> {
    const params = new HttpParams().set('capaciteMin', capaciteMin);
    return this.http.get<FoyerView[]>(this.queriesUrl, { params });
  }

  historique(foyerId: string): Observable<EvenementFoyer[]> {
    return this.http.get<EvenementFoyer[]>(`${this.queriesUrl}/${foyerId}/evenements`);
  }
}
