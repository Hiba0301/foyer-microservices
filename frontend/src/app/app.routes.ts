import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'foyers', pathMatch: 'full' },
  {
    path: 'foyers',
    title: 'Foyers',
    loadComponent: () => import('./pages/foyers/foyer-list').then((m) => m.FoyerList),
  },
  {
    path: 'foyers/nouveau',
    title: 'Ajouter un foyer',
    loadComponent: () => import('./pages/foyers/foyer-form').then((m) => m.FoyerForm),
  },
  {
    path: 'foyers/:id/modifier',
    title: 'Modifier un foyer',
    loadComponent: () => import('./pages/foyers/foyer-form').then((m) => m.FoyerForm),
  },
  {
    path: 'universites',
    title: 'Universités',
    loadComponent: () => import('./pages/universites/universite-list').then((m) => m.UniversiteList),
  },
  {
    path: 'universites/nouveau',
    title: 'Ajouter une université',
    loadComponent: () => import('./pages/universites/universite-form').then((m) => m.UniversiteForm),
  },
  {
    path: 'universites/:id/modifier',
    title: 'Modifier une université',
    loadComponent: () => import('./pages/universites/universite-form').then((m) => m.UniversiteForm),
  },
  {
    path: 'blocs',
    title: 'Blocs',
    loadComponent: () => import('./pages/blocs/bloc-list').then((m) => m.BlocList),
  },
  {
    path: 'cqrs',
    title: 'Foyers (CQRS)',
    loadComponent: () => import('./pages/foyers-cqrs/foyer-cqrs').then((m) => m.FoyerCqrs),
  },
  { path: '**', redirectTo: 'foyers' },
];
