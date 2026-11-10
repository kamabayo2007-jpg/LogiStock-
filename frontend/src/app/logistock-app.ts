import { Component, effect, inject, untracked } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AlertesService } from './alertes.service';
import { AuthService } from './auth.service';
import { Cloche } from './cloche';
import { Icone } from './icone';
import { libelleRole } from './models';
import { MessageService } from './message.service';

/** Coquille de l'application : menu selon le rôle, alertes, message d'état, zone de page. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icone, Cloche],
  template: `
    @if (msg.courant(); as m) {
      <div class="toast" [class.erreur]="m.type === 'erreur'" [class.alerte]="m.type === 'alerte'" (click)="msg.fermer()">{{ m.texte }}</div>
    }
    @if (auth.utilisateur(); as u) {
      <div class="layout">
        <aside class="side">
          <div class="haut">
            <div class="marque"><span class="logo">L</span> LogiStock <span class="sa">SA</span></div>
            @if (auth.voitAlertes()) {
              <app-cloche />
            }
          </div>
          <nav>
            @for (e of auth.menu(); track e.chemin) {
              <a [routerLink]="e.chemin" routerLinkActive="actif"><app-icone [nom]="e.icone" /> {{ e.libelle }}</a>
            }
          </nav>
          <div class="moi">
            <div class="avatar">{{ initiales(u.nom) }}</div>
            <div class="qui">
              <strong>{{ u.nom }}</strong>
              <small>{{ role(u.role) }}</small>
            </div>
            <div class="outils">
              <a routerLink="/a-propos" class="icone-btn" title="À propos" aria-label="À propos"><app-icone nom="info" /></a>
              <button class="icone-btn" title="Déconnexion" aria-label="Déconnexion" (click)="auth.logout()"><app-icone nom="sortie" /></button>
            </div>
          </div>
        </aside>
        <main><router-outlet /></main>
      </div>
    } @else {
      <router-outlet />
    }
  `,
})
export class LogiStockApp {
  auth = inject(AuthService);
  msg = inject(MessageService);
  private alertes = inject(AlertesService);
  role = libelleRole;

  constructor() {
    // Les alertes de stock ne sont suivies que pour l'administrateur et le gestionnaire de stock.
    effect(() => {
      const suivre = this.auth.utilisateur() !== null && this.auth.voitAlertes();
      untracked(() => (suivre ? this.alertes.demarrer() : this.alertes.arreter()));
    });
  }

  initiales(nom: string): string {
    return nom
      .split(/\s+/)
      .filter((m) => m.length > 0)
      .slice(0, 2)
      .map((m) => m[0].toUpperCase())
      .join('');
  }
}
