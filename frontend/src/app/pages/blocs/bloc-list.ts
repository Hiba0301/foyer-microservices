import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Bloc } from '../../models/bloc';
import { Foyer } from '../../models/foyer';
import { BlocService } from '../../services/bloc.service';
import { FoyerService } from '../../services/foyer.service';
import { messageErreur } from '../../shared/http-error';

/** Blocs (bloc-service) : affectation à un foyer avec contrôle de capacité côté serveur. */
@Component({
  selector: 'app-bloc-list',
  imports: [ReactiveFormsModule],
  templateUrl: './bloc-list.html',
})
export class BlocList implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly blocService = inject(BlocService);
  private readonly foyerService = inject(FoyerService);

  protected readonly blocs = signal<Bloc[]>([]);
  protected readonly foyers = signal<Foyer[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly succes = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    nomBloc: ['', [Validators.required, Validators.minLength(1)]],
    capaciteBloc: [50, [Validators.required, Validators.min(1)]],
  });

  ngOnInit(): void {
    this.charger();
    this.foyerService.getAll().subscribe({
      next: (foyers) => this.foyers.set(foyers),
      error: () => this.foyers.set([]),
    });
  }

  charger(): void {
    this.blocService.getAll().subscribe({
      next: (blocs) => {
        this.blocs.set(blocs);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Chargement des blocs'));
        this.chargement.set(false);
      },
    });
  }

  ajouter(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nomBloc, capaciteBloc } = this.form.getRawValue();
    this.blocService.add(nomBloc.trim(), Number(capaciteBloc)).subscribe({
      next: (b) => {
        this.succes.set(`Bloc « ${b.nomBloc} » ajouté.`);
        this.form.reset({ nomBloc: '', capaciteBloc: 50 });
        this.charger();
      },
      error: (err) => this.erreur.set(messageErreur(err, 'Ajout du bloc')),
    });
  }

  affecter(bloc: Bloc, idFoyer: string): void {
    if (!idFoyer) {
      return;
    }
    this.erreur.set(null);
    this.blocService.affecter(bloc.idBloc, Number(idFoyer)).subscribe({
      next: (b) => {
        this.succes.set(`Bloc « ${b.nomBloc} » affecté au foyer « ${b.foyer?.nomFoyer} ».`);
        this.charger();
      },
      error: (err) => this.erreur.set(messageErreur(err, 'Affectation')),
    });
  }

  desaffecter(bloc: Bloc): void {
    this.blocService.desaffecter(bloc.idBloc).subscribe({
      next: () => {
        this.succes.set(`Bloc « ${bloc.nomBloc} » retiré de son foyer.`);
        this.charger();
      },
      error: (err) => this.erreur.set(messageErreur(err, 'Désaffectation')),
    });
  }

  supprimer(bloc: Bloc): void {
    if (!confirm(`Supprimer le bloc « ${bloc.nomBloc} » ?`)) {
      return;
    }
    this.blocService.delete(bloc.idBloc).subscribe({
      next: () => {
        this.succes.set(`Bloc « ${bloc.nomBloc} » supprimé.`);
        this.charger();
      },
      error: (err) => this.erreur.set(messageErreur(err, 'Suppression')),
    });
  }
}
