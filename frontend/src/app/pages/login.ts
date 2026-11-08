import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="connexion">
      <div class="presentation">
        <div class="marque grand"><span class="logo">L</span> LogiStock <span class="sa">SA</span></div>
        <h1>Gérez votre stock et vos ventes en toute simplicité</h1>
        <ul>
          <li>Suivi du stock en temps réel et alertes de rupture</li>
          <li>Commandes clients et factures PDF automatiques</li>
          <li>Tableau de bord adapté à chaque métier</li>
        </ul>
      </div>
      <div class="formulaire">
        <div class="carte-login">
          <h2>Connexion</h2>
          <p class="sous-titre">Entrez vos identifiants pour continuer</p>
          <label>E-mail
            <input type="email" [(ngModel)]="email" placeholder="vous@logistock.ml" (keyup.enter)="entrer()" />
          </label>
          <label>Mot de passe
            <input type="password" [(ngModel)]="motDePasse" placeholder="••••••••" (keyup.enter)="entrer()" />
          </label>
          @if (erreur()) {
            <div class="alerte">{{ erreur() }}</div>
          }
          <button class="plein" (click)="entrer()" [disabled]="attente()">
            {{ attente() ? 'Connexion…' : 'Se connecter' }}
          </button>
        </div>
      </div>
    </div>
  `,
})
export class LoginPage {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  motDePasse = '';
  erreur = signal('');
  attente = signal(false);

  entrer() {
    if (!this.email || !this.motDePasse) {
      this.erreur.set("Saisissez votre e-mail et votre mot de passe");
      return;
    }
    this.erreur.set('');
    this.attente.set(true);
    this.auth.connexion(this.email.trim(), this.motDePasse).subscribe({
      next: () => {
        this.attente.set(false);
        this.router.navigate(['/tableau-de-bord']);
      },
      error: (e) => {
        this.attente.set(false);
        this.erreur.set(
          e?.status === 0
            ? "Impossible de joindre le serveur : l'API est-elle démarrée ?"
            : (e?.error?.message ?? 'Connexion impossible'),
        );
      },
    });
  }
}
