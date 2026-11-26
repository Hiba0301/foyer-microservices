import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Foyer } from '../../models/foyer';
import { Universite } from '../../models/universite';
import { FoyerService } from '../../services/foyer.service';
import { UniversiteService } from '../../services/universite.service';
import { messageErreur } from '../../shared/http-error';

@Component({
  selector: 'app-universite-list',
  imports: [RouterLink],
  templateUrl: './universite-list.html',
})
export class UniversiteList implements OnInit {
  private readonly universiteService = inject(UniversiteService);
  private readonly foyerService = inject(FoyerService);

  protected readonly universites = signal<Universite[]>([]);
  protected readonly foyers = signal<Foyer[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly succes = signal<string | null>(null);

  /** Foyers qui ne sont affectés à aucune université. */
  protected readonly foyersLibres = computed(() => {
    const pris = new Set(this.universites().map((u) => u.foyer?.idFoyer));
    return this.foyers().filter((f) => !pris.has(f.idFoyer));
  });

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    forkJoin({ universites: this.universiteService.getAll(), foyers: this.foyerService.getAll() }).subscribe({
      next: ({ universites, foyers }) => {
        this.universites.set(universites);
        this.foyers.set(foyers);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Chargement des universités'));
        this.chargement.set(false);
      },
    });
  }

  affecter(universite: Universite, idFoyer: string): void {
    if (!idFoyer) {
      this.erreur.set('Choisissez un foyer à affecter.');
      return;
    }
    this.erreur.set(null);
    this.universiteService.affecterFoyer(Number(idFoyer), universite.nomUniversite).subscribe({
      next: (u) => {
        this.succes.set(`Foyer « ${u.foyer?.nomFoyer} » affecté à « ${u.nomUniversite} ».`);
        this.charger();
      },
      error: (err) => this.erreur.set(messageErreur(err, "Affectation du foyer")),
    });
  }

  desaffecter(universite: Universite): void {
    this.erreur.set(null);
    this.universiteService.desaffecterFoyer(universite.idUniversite!).subscribe({
      next: () => {
        this.succes.set(`Foyer désaffecté de « ${universite.nomUniversite} ».`);
        this.charger();
      },
      error: (err) => this.erreur.set(messageErreur(err, 'Désaffectation du foyer')),
    });
  }

  supprimer(universite: Universite): void {
    if (!confirm(`Supprimer l'université « ${universite.nomUniversite} » ?`)) {
      return;
    }
    this.erreur.set(null);
    this.universiteService.delete(universite.idUniversite!).subscribe({
      next: () => {
        this.succes.set(`Université « ${universite.nomUniversite} » supprimée.`);
        this.charger();
      },
      error: (err) => this.erreur.set(messageErreur(err, "Suppression de l'université")),
    });
  }
}
