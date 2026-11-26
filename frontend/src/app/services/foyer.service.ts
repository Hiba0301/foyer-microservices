import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Foyer } from '../models/foyer';

@Injectable({ providedIn: 'root' })
export class FoyerService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/foyers`;

  getAll(): Observable<Foyer[]> {
    return this.http.get<Foyer[]>(`${this.baseUrl}/getAll`);
  }

  getById(idFoyer: number): Observable<Foyer> {
    const params = new HttpParams().set('idFoyer', idFoyer);
    return this.http.get<Foyer>(`${this.baseUrl}/get`, { params });
  }

  add(foyer: Foyer): Observable<Foyer> {
    return this.http.post<Foyer>(`${this.baseUrl}/add`, foyer);
  }

  update(foyer: Foyer): Observable<Foyer> {
    return this.http.put<Foyer>(`${this.baseUrl}/update`, foyer);
  }

  delete(idFoyer: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/delete/${idFoyer}`);
  }

  /** Crée le foyer puis l'affecte directement à l'université. */
  addAndAffecter(foyer: Foyer, idUniversite: number): Observable<Foyer> {
    const params = new HttpParams().set('idUniversite', idUniversite);
    return this.http.post<Foyer>(`${this.baseUrl}/ajouter-affecter`, foyer, { params });
  }
}
