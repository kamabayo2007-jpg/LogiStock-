import { Component, inject } from '@angular/core';
import { AuthService } from '../auth.service';
import { Icone } from '../icone';
import { Role, libelleRole } from '../models';

/** Noms des membres du groupe : à compléter (la section n'apparaît que si la liste n'est pas vide). */
const EQUIPE: string[] = [
  'Mama Dit Kama Traoré – Scrum Master',
  'Ahamadou Diarra – Lead back, qualité et sécurité',
  'Hamzata Salahadine – Lead back',
  'Gaoussou Cissé – Lead back',
  'Founè Koita – Lead front',
  'Nouhoum Berthe – Lead front',
  'Mouhamoud Diarra – Lead front',
];

const GUIDE: Record<Role, string[]> = {
  ADMINISTRATEUR: [
    'Consulter le tableau de bord : chiffre d\'affaires, commandes, stock et alertes.',
    'Créer, modifier, désactiver et réactiver les comptes utilisateurs, et leur attribuer un rôle.',
    'Consulter les produits, les mouvements de stock, les clients, les commandes et les factures.',
  ],
  GESTIONNAIRE_STOCK: [
    'Ajouter, modifier et supprimer des produits et des catégories.',
    'Enregistrer les entrées (réceptions) et les sorties de stock, et consulter l\'historique.',
    'Suivre les alertes : la cloche indique les produits sous leur seuil d\'alerte.',
  ],
  COMMERCIAL: [
    'Gérer le fichier clients et consulter l\'historique d\'achats de chaque client.',
    'Créer une commande avec plusieurs produits, la modifier tant qu\'elle est en cours, puis la valider ou l\'annuler.',
    'Consulter le catalogue classé par catégorie (prix et stock disponible).',
    'Télécharger la facture au format PDF d\'une commande validée.',
  ],
};

@Component({
  selector: 'app-a-propos',
  imports: [Icone],
  template: `
    <div class="entete">
      <div class="titre-page"><span class="titre-ico"><app-icone nom="info" [taille]="22" /></span><div><h2>À propos</h2><p class="sous-titre">Présentation de l'application et guide d'utilisation</p></div></div>
    </div>

    <section class="panneau apropos-tete">
      <div class="marque grand-noir"><span class="logo">L</span> LogiStock <span class="sa">SA</span></div>
      <p>
        LogiStock SA est une application web de <strong>gestion de stock et de facturation</strong> : elle suit les produits
        et leurs quantités, enregistre les mouvements de stock, gère les clients et les commandes, et émet automatiquement
        les factures. Chaque personne accède uniquement aux fonctions de son métier.
      </p>
      <p class="aide">Version 1.0 · Projet fil rouge, Informatique de Gestion (Groupe N°1)</p>
    </section>

    <div class="grille-cartes">
      <section class="carte">
        <h3>Votre rôle : {{ role(auth.utilisateur()?.role ?? '') }}</h3>
        <ul class="liste">
          @for (ligne of guide(); track ligne) {
            <li>{{ ligne }}</li>
          }
        </ul>
      </section>

      <section class="carte">
        <h3>Règles de gestion</h3>
        <ul class="liste">
          <li>Le stock d'un produit ne peut <strong>jamais être négatif</strong> : une sortie supérieure au stock est refusée.</li>
          <li>Une commande est créée « en cours » ; le stock ne diminue qu'à sa <strong>validation</strong>.</li>
          <li>La validation est <strong>tout ou rien</strong> : si un seul produit manque, rien n'est modifié.</li>
          <li>Chaque vente validée crée une sortie de stock par produit et une facture numérotée (FAC-AAAA-NNNN).</li>
          <li>Le prix d'une commande est <strong>figé</strong> au moment de sa création.</li>
          <li>Une commande <strong>en cours</strong> peut être modifiée (client, produits, quantités) ; une commande validée ne peut plus l'être.</li>
          <li>Un produit est « en alerte » quand son stock est inférieur ou égal à son seuil.</li>
        </ul>
      </section>

      <section class="carte">
        <h3>Sécurité</h3>
        <ul class="liste">
          <li>Connexion par e-mail et mot de passe ; les mots de passe sont <strong>stockés chiffrés</strong> (BCrypt).</li>
          <li>Session sécurisée par jeton JWT, valable 8 heures.</li>
          <li>Accès contrôlé par rôle, côté serveur : un rôle ne peut pas appeler les fonctions d'un autre.</li>
          <li>Un administrateur ne peut pas désactiver son propre compte.</li>
        </ul>
      </section>

      <section class="carte">
        <h3>Alertes de stock (administrateur et gestionnaire de stock)</h3>
        <ul class="liste">
          <li>La cloche en bas du menu (en haut sur téléphone) affiche le nombre de produits sous leur seuil.</li>
          <li>Un message apparaît à la connexion, puis dès qu'un produit passe sous son seuil.</li>
          <li>Les notifications du navigateur peuvent être activées depuis la cloche.</li>
        </ul>
      </section>

      <section class="carte">
        <h3>Technologies</h3>
        <div class="puces">
          <span class="puce">Angular</span><span class="puce">Spring Boot 3</span><span class="puce">Java 17</span>
          <span class="puce">MySQL / MariaDB</span><span class="puce">JWT</span><span class="puce">Docker</span>
        </div>
        <p class="aide">Architecture en couches (domaine, cas d'usage, adaptateurs) avec tests automatisés du métier.</p>
      </section>

      @if (equipe.length > 0) {
        <section class="carte">
          <h3>L'équipe</h3>
          <div class="puces">
            @for (nom of equipe; track nom) { <span class="puce">{{ nom }}</span> }
          </div>
        </section>
      }
    </div>
    <p class="aide pied">© 2026 LogiStock SA · Application réalisée dans un cadre pédagogique.</p>
  `,
})
export class AProposPage {
  auth = inject(AuthService);
  role = libelleRole;
  equipe = EQUIPE;

  guide(): string[] {
    const r = this.auth.utilisateur()?.role;
    return r ? GUIDE[r] : [];
  }
}
