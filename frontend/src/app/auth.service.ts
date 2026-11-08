import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { API_URL } from './config';
import { Role, Utilisateur } from './models';

interface ReponseConnexion {
  token: string;
  user: Utilisateur;
}

export interface EntreeMenu {
  chemin: string;
  libelle: string;
  icone: string;
}

const CLE_TOKEN = 'logistock.token';
const CLE_USER = 'logistock.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  readonly utilisateur = signal<Utilisateur | null>(this.lireUtilisateur());

  /** Menu de gauche : dépend du rôle de la personne connectée (US3). */
  readonly menu = computed<EntreeMenu[]>(() => {
    const role = this.utilisateur()?.role;
    const menu: EntreeMenu[] = [
      { chemin: '/tableau-de-bord', libelle: 'Tableau de bord', icone: 'tableau' },
      { chemin: '/produits', libelle: 'Produits', icone: 'produits' },
    ];
    if (role === 'ADMINISTRATEUR' || role === 'GESTIONNAIRE_STOCK') {
      menu.push({ chemin: '/mouvements', libelle: 'Mouvements de stock', icone: 'mouvements' });
    }
    if (role === 'ADMINISTRATEUR' || role === 'COMMERCIAL') {
      menu.push({ chemin: '/clients', libelle: 'Clients', icone: 'clients' });
      menu.push({ chemin: '/commandes', libelle: 'Commandes', icone: 'commandes' });
    }
    if (role === 'ADMINISTRATEUR') {
      menu.push({ chemin: '/comptes', libelle: 'Comptes', icone: 'comptes' });
    }
    return menu;
  });

  token(): string | null {
    try {
      return localStorage.getItem(CLE_TOKEN);
    } catch {
      return null;
    }
  }

  a(role: Role): boolean {
    return this.utilisateur()?.role === role;
  }

  connexion(email: string, motDePasse: string) {
    return this.http
      .post<ReponseConnexion>(API_URL + '/auth/login', { email, password: motDePasse })
      .pipe(
        tap((r) => {
          try {
            localStorage.setItem(CLE_TOKEN, r.token);
            localStorage.setItem(CLE_USER, JSON.stringify(r.user));
          } catch {
            /* stockage indisponible : la session durera jusqu'au rechargement */
          }
          this.utilisateur.set(r.user);
        }),
      );
  }

  logout() {
    try {
      localStorage.removeItem(CLE_TOKEN);
      localStorage.removeItem(CLE_USER);
    } catch {
      /* rien à faire */
    }
    this.utilisateur.set(null);
    this.router.navigate(['/login']);
  }

  private lireUtilisateur(): Utilisateur | null {
    try {
      const brut = localStorage.getItem(CLE_USER);
      return brut && localStorage.getItem(CLE_TOKEN) ? (JSON.parse(brut) as Utilisateur) : null;
    } catch {
      return null;
    }
  }
}
