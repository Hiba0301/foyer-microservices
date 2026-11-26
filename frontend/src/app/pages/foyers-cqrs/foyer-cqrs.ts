import { DatePipe, JsonPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { timer } from 'rxjs';
import { EvenementFoyer, FoyerView } from '../../models/foyer-view';
import { FoyerCqrsService } from '../../services/foyer-cqrs.service';
import { messageErreur } from '../../shared/http-error';

/**
 * Démonstration CQRS : les boutons envoient des COMMANDES (écriture),
 * le tableau est lu via des REQUÊTES sur le modèle de lecture (foyer_view).
 */
@Component({
  selector: 'app-foyer-cqrs',
  imports: [ReactiveFormsModule, DatePipe, JsonPipe],
  templateUrl: './foyer-cqrs.html',
})
export class FoyerCqrs implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cqrs = inject(FoyerCqrsService);

  protected readonly foyers = signal<FoyerView[]>([]);
  protected readonly evenements = signal<EvenementFoyer[]>([]);
  protected readonly foyerSelectionne = signal<FoyerView | null>(null);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly succes = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    nomFoyer: ['', [Validators.required, Validators.minLength(2)]],
    capaciteFoyer: [100, [Validators.required, Validators.min(1)]],
  });

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.cqrs.lister().subscribe({
      next: (foyers) => {
        this.foyers.set(foyers);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Requête GetAllFoyersQuery'));
        this.chargement.set(false);
      },
    });
  }

  creer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nomFoyer, capaciteFoyer } = this.form.getRawValue();
    this.erreur.set(null);
    this.cqrs.creer(nomFoyer.trim(), Number(capaciteFoyer)).subscribe({
      next: (r) => {
        this.succes.set(`CreerFoyerCommand traitée : foyer ${r.foyerId}`);
        this.form.reset({ nomFoyer: '', capaciteFoyer: 100 });
        this.rechargerApresProjection();
      },
      error: (err) => this.erreur.set(messageErreur(err, 'CreerFoyerCommand')),
    });
  }

  modifier(foyer: FoyerView, valeur: string): void {
    this.erreur.set(null);
    this.cqrs.modifierCapacite(foyer.foyerId, Number(valeur)).subscribe({
      next: () => {
        this.succes.set(`ModifierCapaciteFoyerCommand traitée pour « ${foyer.nomFoyer} »`);
        this.rechargerApresProjection(foyer);
      },
      error: (err) => this.erreur.set(messageErreur(err, 'ModifierCapaciteFoyerCommand')),
    });
  }

  supprimer(foyer: FoyerView): void {
    if (!confirm(`Envoyer SupprimerFoyerCommand pour « ${foyer.nomFoyer} » ?`)) {
      return;
    }
    this.erreur.set(null);
    this.cqrs.supprimer(foyer.foyerId).subscribe({
      next: () => {
        this.succes.set(`SupprimerFoyerCommand traitée pour « ${foyer.nomFoyer} »`);
        this.rechargerApresProjection(foyer);
      },
      error: (err) => this.erreur.set(messageErreur(err, 'SupprimerFoyerCommand')),
    });
  }

  afficherHistorique(foyer: FoyerView): void {
    this.foyerSelectionne.set(foyer);
    this.cqrs.historique(foyer.foyerId).subscribe({
      next: (evenements) => this.evenements.set(evenements),
      error: (err) => this.erreur.set(messageErreur(err, "Lecture de l'event store")),
    });
  }

  /**
   * Cohérence à terme : la projection met à jour foyer_view de façon asynchrone,
   * on relit donc la vue un court instant après la commande.
   */
  private rechargerApresProjection(foyer?: FoyerView): void {
    timer(400).subscribe(() => {
      this.charger();
      if (foyer && this.foyerSelectionne()?.foyerId === foyer.foyerId) {
        this.afficherHistorique(foyer);
      }
    });
  }
}
