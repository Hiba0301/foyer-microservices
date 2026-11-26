import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Bloc } from '../models/bloc';

@Injectable({ providedIn: 'root' })
export class BlocService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/blocs`;

  getAll(): Observable<Bloc[]> {
    return this.http.get<Bloc[]>(`${this.baseUrl}/getAll`);
  }

  add(nomBloc: string, capaciteBloc: number): Observable<Bloc> {
    return this.http.post<Bloc>(`${this.baseUrl}/add`, { idBloc: 0, nomBloc, capaciteBloc });
  }

  delete(idBloc: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/delete/${idBloc}`);
  }

  affecter(idBloc: number, idFoyer: number): Observable<Bloc> {
    const params = new HttpParams().set('idBloc', idBloc).set('idFoyer', idFoyer);
    return this.http.put<Bloc>(`${this.baseUrl}/affecter-bloc-foyer`, null, { params });
  }

  desaffecter(idBloc: number): Observable<Bloc> {
    const params = new HttpParams().set('idBloc', idBloc);
    return this.http.put<Bloc>(`${this.baseUrl}/desaffecter-bloc-foyer`, null, { params });
  }
}
