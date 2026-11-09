import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../api.service';
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
          @if (!inscription()) {
            <h2>Connexion</h2>
            <p class="sous-titre">Entrez vos identifiants pour continuer</p>
            @if (info()) {
              <div class="alerte ok">{{ info() }}</div>
            }
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
            <p class="bas-carte">Pas encore de compte ? <a href="#" (click)="basculer($event, true)">Demander un compte</a></p>
          } @else {
            <h2>Demande de compte</h2>
            <p class="sous-titre">Un administrateur activera votre compte après vérification</p>
            <label>Nom complet
              <input type="text" [(ngModel)]="nom" placeholder="Aminata Traoré" />
            </label>
            <label>E-mail
              <input type="email" [(ngModel)]="email" placeholder="vous@logistock.ml" />
            </label>
            <label>Mot de passe (8 caractères minimum)
              <input type="password" [(ngModel)]="motDePasse" placeholder="••••••••" />
            </label>
            <label>Confirmer le mot de passe
              <input type="password" [(ngModel)]="confirmation" placeholder="••••••••" (keyup.enter)="demander()" />
            </label>
            @if (erreur()) {
              <div class="alerte">{{ erreur() }}</div>
            }
            <button class="plein" (click)="demander()" [disabled]="attente()">
              {{ attente() ? 'Envoi…' : 'Envoyer la demande' }}
            </button>
            <p class="bas-carte">Déjà un compte ? <a href="#" (click)="basculer($event, false)">Se connecter</a></p>
          }
        </div>
      </div>
    </div>
  `,
})
export class LoginPage {
  private auth = inject(AuthService);
  private api = inject(ApiService);
  private router = inject(Router);

  nom = '';
  email = '';
  motDePasse = '';
  confirmation = '';
  erreur = signal('');
  info = signal('');
  attente = signal(false);
  inscription = signal(false);

  basculer(e: Event, versInscription: boolean) {
    e.preventDefault();
    this.erreur.set('');
    this.motDePasse = '';
    this.confirmation = '';
    this.inscription.set(versInscription);
  }

  entrer() {
    if (!this.email || !this.motDePasse) {
      this.erreur.set('Saisissez votre e-mail et votre mot de passe');
      return;
    }
    this.erreur.set('');
    this.info.set('');
    this.attente.set(true);
    this.auth.connexion(this.email.trim(), this.motDePasse).subscribe({
      next: () => {
        this.attente.set(false);
        this.router.navigate(['/tableau-de-bord']);
      },
      error: (e) => {
        this.attente.set(false);
        this.erreur.set(this.message(e, 'Connexion impossible'));
      },
    });
  }

  demander() {
    if (!this.nom.trim() || !this.email.trim() || !this.motDePasse) {
      this.erreur.set('Tous les champs sont obligatoires');
      return;
    }
    if (this.motDePasse.length < 8) {
      this.erreur.set('Le mot de passe doit contenir au moins 8 caractères');
      return;
    }
    if (this.motDePasse !== this.confirmation) {
      this.erreur.set('Les deux mots de passe ne correspondent pas');
      return;
    }
    this.erreur.set('');
    this.attente.set(true);
    this.api
      .post<{ message: string }>('/auth/inscription', {
        nom: this.nom.trim(),
        email: this.email.trim(),
        motDePasse: this.motDePasse,
      })
      .subscribe({
        next: (r) => {
          this.attente.set(false);
          this.info.set(r.message);
          this.nom = '';
          this.motDePasse = '';
          this.confirmation = '';
          this.inscription.set(false);
        },
        error: (e) => {
          this.attente.set(false);
          this.erreur.set(this.message(e, "Impossible d'envoyer la demande"));
        },
      });
  }

  private message(e: { status?: number; error?: { message?: string } }, defaut: string) {
    return e?.status === 0
      ? "Impossible de joindre le serveur : l'API est-elle démarrée ?"
      : (e?.error?.message ?? defaut);
  }
}
