import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Foyer } from '../../models/foyer';
import { Universite } from '../../models/universite';
import { UniversiteService } from '../../services/universite.service';
import { messageErreur } from '../../shared/http-error';

@Component({
  selector: 'app-universite-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './universite-form.html',
})
export class UniversiteForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly universiteService = inject(UniversiteService);

  protected readonly idUniversite = signal<number | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly envoi = signal(false);
  // conservé tel quel à la modification, sinon le PUT désaffecterait le foyer
  private foyerActuel: Foyer | null = null;

  protected readonly form = this.fb.nonNullable.group({
    nomUniversite: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    adresse: ['', [Validators.required, Validators.maxLength(200)]],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.idUniversite.set(Number(id));
      this.universiteService.getById(Number(id)).subscribe({
        next: (u) => {
          this.foyerActuel = u.foyer ?? null;
          this.form.patchValue({ nomUniversite: u.nomUniversite, adresse: u.adresse });
        },
        error: (err) => this.erreur.set(messageErreur(err, "Chargement de l'université")),
      });
    }
  }

  protected champInvalide(nom: 'nomUniversite' | 'adresse'): boolean {
    const control = this.form.controls[nom];
    return control.invalid && (control.touched || control.dirty);
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nomUniversite, adresse } = this.form.getRawValue();
    const universite: Universite = { nomUniversite: nomUniversite.trim(), adresse: adresse.trim() };

    const requete =
      this.idUniversite() !== null
        ? this.universiteService.update({ ...universite, idUniversite: this.idUniversite()!, foyer: this.foyerActuel })
        : this.universiteService.add(universite);

    this.envoi.set(true);
    this.erreur.set(null);
    requete.subscribe({
      next: () => this.router.navigate(['/universites']),
      error: (err) => {
        this.erreur.set(messageErreur(err, "Enregistrement de l'université"));
        this.envoi.set(false);
      },
    });
  }
}
