import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { Foyer } from '../models/foyer';
import { FoyerService } from './foyer.service';

describe('FoyerService', () => {
  const baseUrl = `${environment.apiUrl}/api/foyers`;
  const foyer: Foyer = { idFoyer: 1, nomFoyer: 'Foyer El Amal', capaciteFoyer: 300 };
  let service: FoyerService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FoyerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getAll() fait un GET sur /getAll', () => {
    service.getAll().subscribe((foyers) => expect(foyers).toEqual([foyer]));

    const req = httpMock.expectOne(`${baseUrl}/getAll`);
    expect(req.request.method).toBe('GET');
    req.flush([foyer]);
  });

  it('getById() passe idFoyer en paramètre', () => {
    service.getById(1).subscribe((f) => expect(f).toEqual(foyer));

    const req = httpMock.expectOne(`${baseUrl}/get?idFoyer=1`);
    expect(req.request.method).toBe('GET');
    req.flush(foyer);
  });

  it('add() fait un POST avec le foyer dans le corps', () => {
    const nouveau: Foyer = { nomFoyer: 'Nouveau', capaciteFoyer: 50 };
    service.add(nouveau).subscribe((f) => expect(f.idFoyer).toBe(2));

    const req = httpMock.expectOne(`${baseUrl}/add`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(nouveau);
    req.flush({ ...nouveau, idFoyer: 2 });
  });

  it('update() fait un PUT avec le foyer dans le corps', () => {
    service.update(foyer).subscribe((f) => expect(f).toEqual(foyer));

    const req = httpMock.expectOne(`${baseUrl}/update`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(foyer);
    req.flush(foyer);
  });

  it("delete() fait un DELETE avec l'id dans l'URL", () => {
    service.delete(1).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/delete/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it("addAndAffecter() fait un POST avec idUniversite en paramètre", () => {
    service.addAndAffecter(foyer, 5).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/ajouter-affecter?idUniversite=5`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(foyer);
    req.flush(foyer);
  });

  it("propage l'erreur HTTP au composant", () => {
    let status = 0;
    service.getAll().subscribe({ error: (err) => (status = err.status) });

    httpMock.expectOne(`${baseUrl}/getAll`).flush('Erreur', { status: 500, statusText: 'Server Error' });
    expect(status).toBe(500);
  });
});
