import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth.service';
import { Icone } from './icone';
import { libelleRole } from './models';
import { MessageService } from './message.service';

/** Coquille de l'application : menu selon le rôle, message d'état, zone de page. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icone],
  template: `
    @if (msg.courant(); as m) {
      <div class="toast" [class.erreur]="m.type === 'erreur'" (click)="msg.fermer()">{{ m.texte }}</div>
    }
    @if (auth.utilisateur(); as u) {
      <div class="layout">
        <aside class="side">
          <div class="marque"><span class="logo">L</span> LogiStock <span class="sa">SA</span></div>
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
            <button class="icone-btn" title="Déconnexion" (click)="auth.logout()"><app-icone nom="sortie" /></button>
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
  role = libelleRole;

  initiales(nom: string): string {
    return nom
      .split(/\s+/)
      .filter((m) => m.length > 0)
      .slice(0, 2)
      .map((m) => m[0].toUpperCase())
      .join('');
  }
}
