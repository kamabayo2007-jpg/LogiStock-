import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './guards';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login').then((m) => m.LoginPage) },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tableau-de-bord' },
      {
        path: 'tableau-de-bord',
        loadComponent: () => import('./pages/tableau-de-bord').then((m) => m.TableauDeBordPage),
      },
      { path: 'produits', loadComponent: () => import('./pages/produits').then((m) => m.ProduitsPage) },
      {
        path: 'mouvements',
        canActivate: [roleGuard(['ADMINISTRATEUR', 'GESTIONNAIRE_STOCK'])],
        loadComponent: () => import('./pages/mouvements').then((m) => m.MouvementsPage),
      },
      {
        path: 'clients',
        canActivate: [roleGuard(['ADMINISTRATEUR', 'COMMERCIAL'])],
        loadComponent: () => import('./pages/clients').then((m) => m.ClientsPage),
      },
      {
        path: 'commandes',
        canActivate: [roleGuard(['ADMINISTRATEUR', 'COMMERCIAL'])],
        loadComponent: () => import('./pages/commandes').then((m) => m.CommandesPage),
      },
      {
        path: 'comptes',
        canActivate: [roleGuard(['ADMINISTRATEUR'])],
        loadComponent: () => import('./pages/comptes').then((m) => m.ComptesPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
