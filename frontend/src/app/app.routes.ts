import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './guards';

export const routes: Routes = [
  { path: 'login', title: 'Connexion · LogiStock SA', loadComponent: () => import('./pages/login').then((m) => m.LoginPage) },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tableau-de-bord' },
      {
        path: 'tableau-de-bord',
        title: 'Tableau de bord · LogiStock SA',
        loadComponent: () => import('./pages/tableau-de-bord').then((m) => m.TableauDeBordPage),
      },
      { path: 'produits', title: 'Produits · LogiStock SA', loadComponent: () => import('./pages/produits').then((m) => m.ProduitsPage) },
      {
        path: 'mouvements',
        title: 'Mouvements de stock · LogiStock SA',
        canActivate: [roleGuard(['ADMINISTRATEUR', 'GESTIONNAIRE_STOCK'])],
        loadComponent: () => import('./pages/mouvements').then((m) => m.MouvementsPage),
      },
      {
        path: 'clients',
        title: 'Clients · LogiStock SA',
        canActivate: [roleGuard(['ADMINISTRATEUR', 'COMMERCIAL'])],
        loadComponent: () => import('./pages/clients').then((m) => m.ClientsPage),
      },
      {
        path: 'commandes',
        title: 'Commandes · LogiStock SA',
        canActivate: [roleGuard(['ADMINISTRATEUR', 'COMMERCIAL'])],
        loadComponent: () => import('./pages/commandes').then((m) => m.CommandesPage),
      },
      {
        path: 'a-propos',
        title: 'À propos · LogiStock SA',
        loadComponent: () => import('./pages/a-propos').then((m) => m.AProposPage),
      },
      {
        path: 'comptes',
        title: 'Comptes · LogiStock SA',
        canActivate: [roleGuard(['ADMINISTRATEUR'])],
        loadComponent: () => import('./pages/comptes').then((m) => m.ComptesPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
