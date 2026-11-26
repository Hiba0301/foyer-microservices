import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { Foyer } from '../../models/foyer';
import { Universite } from '../../models/universite';
import { FoyerService } from '../../services/foyer.service';
import { UniversiteService } from '../../services/universite.service';
import { messageErreur } from '../../shared/http-error';

@Component({
  selector: 'app-foyer-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './foyer-form.html',
})
export class FoyerForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly foyerService = inject(FoyerService);
  private readonly universiteService = inject(UniversiteService);

  protected readonly idFoyer = signal<number | null>(null);
  protected readonly universitesSansFoyer = signal<Universite[]>([]);
  protected readonly erreur = signal<string | null>(null);
  protected readonly envoi = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    nomFoyer: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    capaciteFoyer: [1, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]],
    idUniversite: [''],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.idFoyer.set(Number(id));
      this.foyerService.getById(Number(id)).subscribe({
        next: (foyer) => this.form.patchValue({ nomFoyer: foyer.nomFoyer, capaciteFoyer: foyer.capaciteFoyer }),
        error: (err) => this.erreur.set(messageErreur(err, 'Chargement du foyer')),
      });
    } else {
      // en création, on propose d'affecter directement le foyer à une université libre
      this.universiteService.getAll().subscribe({
        next: (universites) => this.universitesSansFoyer.set(universites.filter((u) => !u.foyer)),
        error: (err) => this.erreur.set(messageErreur(err, 'Chargement des universités')),
      });
    }
  }

  protected champInvalide(nom: 'nomFoyer' | 'capaciteFoyer'): boolean {
    const control = this.form.controls[nom];
    return control.invalid && (control.touched || control.dirty);
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nomFoyer, capaciteFoyer, idUniversite } = this.form.getRawValue();
    const foyer: Foyer = { nomFoyer: nomFoyer.trim(), capaciteFoyer: Number(capaciteFoyer) };

    let requete: Observable<Foyer>;
    if (this.idFoyer() !== null) {
      requete = this.foyerService.update({ ...foyer, idFoyer: this.idFoyer()! });
    } else if (idUniversite) {
      requete = this.foyerService.addAndAffecter(foyer, Number(idUniversite));
    } else {
      requete = this.foyerService.add(foyer);
    }

    this.envoi.set(true);
    this.erreur.set(null);
    requete.subscribe({
      next: () => this.router.navigate(['/foyers']),
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Enregistrement du foyer'));
        this.envoi.set(false);
      },
    });
  }
}
