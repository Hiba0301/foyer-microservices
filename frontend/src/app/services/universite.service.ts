import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Universite } from '../models/universite';

@Injectable({ providedIn: 'root' })
export class UniversiteService {
  private readonly http = inject(HttpClient);
  // chemin corrigé dans universite-service ("univeristes" -> "universites")
  private readonly baseUrl = `${environment.apiUrl}/api/universites`;

  getAll(): Observable<Universite[]> {
    return this.http.get<Universite[]>(`${this.baseUrl}/getAll`);
  }

  getById(idUniversite: number): Observable<Universite> {
    const params = new HttpParams().set('idUniversite', idUniversite);
    return this.http.get<Universite>(`${this.baseUrl}/get`, { params });
  }

  add(universite: Universite): Observable<Universite> {
    return this.http.post<Universite>(`${this.baseUrl}/add`, universite);
  }

  update(universite: Universite): Observable<Universite> {
    return this.http.put<Universite>(`${this.baseUrl}/update`, universite);
  }

  delete(idUniversite: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/delete/${idUniversite}`);
  }

  affecterFoyer(idFoyer: number, nomUniversite: string): Observable<Universite> {
    const params = new HttpParams().set('idFoyer', idFoyer).set('nomUniversite', nomUniversite);
    return this.http.put<Universite>(`${this.baseUrl}/affecter-foyer-universite`, null, { params });
  }

  desaffecterFoyer(idUniversite: number): Observable<Universite> {
    const params = new HttpParams().set('idUniversite', idUniversite);
    return this.http.put<Universite>(`${this.baseUrl}/desaffecter-foyer-universite`, null, { params });
  }
}
