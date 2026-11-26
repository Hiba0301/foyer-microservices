import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Foyer } from '../../models/foyer';
import { Universite } from '../../models/universite';
import { FoyerService } from '../../services/foyer.service';
import { UniversiteService } from '../../services/universite.service';
import { messageErreur } from '../../shared/http-error';

@Component({
  selector: 'app-foyer-list',
  imports: [RouterLink],
  templateUrl: './foyer-list.html',
})
export class FoyerList implements OnInit {
  private readonly foyerService = inject(FoyerService);
  private readonly universiteService = inject(UniversiteService);

  protected readonly foyers = signal<Foyer[]>([]);
  protected readonly universites = signal<Universite[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly succes = signal<string | null>(null);

  /** idFoyer -> nom de l'université à laquelle il est affecté. */
  protected readonly universiteParFoyer = computed(() => {
    const map = new Map<number, string>();
    for (const u of this.universites()) {
      if (u.foyer?.idFoyer != null) {
        map.set(u.foyer.idFoyer, u.nomUniversite);
      }
    }
    return map;
  });

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    forkJoin({ foyers: this.foyerService.getAll(), universites: this.universiteService.getAll() }).subscribe({
      next: ({ foyers, universites }) => {
        this.foyers.set(foyers);
        this.universites.set(universites);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Chargement des foyers'));
        this.chargement.set(false);
      },
    });
  }

  supprimer(foyer: Foyer): void {
    const universite = this.universiteParFoyer().get(foyer.idFoyer!);
    if (universite) {
      this.erreur.set(
        `Le foyer « ${foyer.nomFoyer} » est affecté à « ${universite} ». Désaffectez-le d'abord depuis la page Universités.`,
      );
      return;
    }
    if (!confirm(`Supprimer le foyer « ${foyer.nomFoyer} » ?`)) {
      return;
    }
    this.erreur.set(null);
    this.foyerService.delete(foyer.idFoyer!).subscribe({
      next: () => {
        this.succes.set(`Foyer « ${foyer.nomFoyer} » supprimé.`);
        this.charger();
      },
      error: (err) => this.erreur.set(messageErreur(err, 'Suppression du foyer')),
    });
  }
}
