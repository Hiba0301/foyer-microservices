import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { Universite } from '../models/universite';
import { UniversiteService } from './universite.service';

describe('UniversiteService', () => {
  const baseUrl = `${environment.apiUrl}/api/universites`;
  const universite: Universite = { idUniversite: 1, nomUniversite: 'ESPRIT', adresse: 'Ariana', foyer: null };
  let service: UniversiteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UniversiteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getAll() fait un GET sur /getAll', () => {
    service.getAll().subscribe((liste) => expect(liste).toEqual([universite]));

    const req = httpMock.expectOne(`${baseUrl}/getAll`);
    expect(req.request.method).toBe('GET');
    req.flush([universite]);
  });

  it('getById() passe idUniversite en paramètre', () => {
    service.getById(1).subscribe((u) => expect(u).toEqual(universite));

    const req = httpMock.expectOne(`${baseUrl}/get?idUniversite=1`);
    expect(req.request.method).toBe('GET');
    req.flush(universite);
  });

  it("add() fait un POST avec l'université dans le corps", () => {
    const nouvelle: Universite = { nomUniversite: 'INSAT', adresse: 'Tunis' };
    service.add(nouvelle).subscribe((u) => expect(u.idUniversite).toBe(2));

    const req = httpMock.expectOne(`${baseUrl}/add`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(nouvelle);
    req.flush({ ...nouvelle, idUniversite: 2 });
  });

  it("update() fait un PUT avec l'université dans le corps", () => {
    service.update(universite).subscribe((u) => expect(u).toEqual(universite));

    const req = httpMock.expectOne(`${baseUrl}/update`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(universite);
    req.flush(universite);
  });

  it("delete() fait un DELETE avec l'id dans l'URL", () => {
    service.delete(1).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/delete/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('affecterFoyer() fait un PUT avec idFoyer et nomUniversite', () => {
    service.affecterFoyer(3, 'ESPRIT').subscribe();

    const req = httpMock.expectOne(`${baseUrl}/affecter-foyer-universite?idFoyer=3&nomUniversite=ESPRIT`);
    expect(req.request.method).toBe('PUT');
    req.flush(universite);
  });

  it('desaffecterFoyer() fait un PUT avec idUniversite', () => {
    service.desaffecterFoyer(1).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/desaffecter-foyer-universite?idUniversite=1`);
    expect(req.request.method).toBe('PUT');
    req.flush(universite);
  });
});
