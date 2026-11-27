import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { UniversiteList } from './universite-list';

describe('UniversiteList', () => {
  const api = environment.apiUrl;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UniversiteList],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche les universités avec le badge dégradé quand foyer-service est indisponible', () => {
    const fixture = TestBed.createComponent(UniversiteList);
    fixture.detectChanges();

    httpMock.expectOne(`${api}/api/universites/getAll`).flush([
      {
        idUniversite: 1,
        nomUniversite: 'ESPRIT',
        adresse: 'Ariana',
        foyer: { idFoyer: 1, nomFoyer: 'Foyer indisponible', capaciteFoyer: 0, disponible: false },
      },
    ]);
    httpMock
      .expectOne(`${api}/api/foyers/getAll`)
      .flush({ status: 503 }, { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('.alert, .erreur')).toBeNull();
    const badge = page.querySelector('.badge-degrade');
    expect(badge?.textContent?.trim()).toBe('Foyer indisponible');
    expect(badge?.getAttribute('title')).toContain('fallback');
  });
});
