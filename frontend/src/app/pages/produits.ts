import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { AuthService } from '../auth.service';
import { FcfaPipe } from '../fcfa.pipe';
import { MessageService } from '../message.service';
import { Categorie, Produit } from '../models';
import { Pagination } from '../pagination';
import { DonneesTable } from '../table';

interface FormProduit {
  id?: number;
  reference: string;
  designation: string;
  description: string;
  prix: number;
  quantite: number;
  seuilAlerte: number;
  categorieId: number | null;
}

@Component({
  selector: 'app-produits',
  imports: [FormsModule, FcfaPipe, Pagination],
  template: `
    <div class="entete">
      <div><h2>Produits</h2><p class="sous-titre">Catalogue et niveaux de stock</p></div>
      @if (peutModifier) {
        <button (click)="nouveau()">+ Nouveau produit</button>
      }
    </div>

    <div class="barre">
      <input type="search" placeholder="Rechercher (désignation ou référence)…" [(ngModel)]="recherche" (ngModelChange)="charger()" />
      <div class="onglets">
        <button [class.actif]="vue === 'tous'" (click)="changerVue('tous')">Tous</button>
        <button [class.actif]="vue === 'alertes'" (click)="changerVue('alertes')">Alertes</button>
        <button [class.actif]="vue === 'ruptures'" (click)="changerVue('ruptures')">Ruptures</button>
      </div>
    </div>

    @if (edition(); as f) {
      <div class="panneau">
        <h3>{{ f.id ? 'Modifier le produit' : 'Nouveau produit' }}</h3>
        <div class="grille">
          <label>Référence <input [(ngModel)]="f.reference" /></label>
          <label>Désignation <input [(ngModel)]="f.designation" /></label>
          <label>Catégorie
            <select [(ngModel)]="f.categorieId">
              <option [ngValue]="null">— choisir —</option>
              @for (c of categories(); track c.id) {
                <option [ngValue]="c.id">{{ c.nom }}</option>
              }
            </select>
          </label>
          <label>Prix (FCFA) <input type="number" min="0" [(ngModel)]="f.prix" /></label>
          @if (!f.id) {
            <label>Quantité initiale <input type="number" min="0" [(ngModel)]="f.quantite" /></label>
          }
          <label>Seuil d'alerte <input type="number" min="0" [(ngModel)]="f.seuilAlerte" /></label>
          <label class="large">Description <input [(ngModel)]="f.description" /></label>
        </div>
        <div class="actions">
          <button (click)="enregistrer(f)">Enregistrer</button>
          <button class="secondaire" (click)="edition.set(null)">Annuler</button>
        </div>
      </div>
    }

    <div class="tableau">
      <table>
        <thead>
          <tr>
            <th class="tri" (click)="t.trier('reference')">Référence <i>{{ t.fleche('reference') }}</i></th>
            <th class="tri" (click)="t.trier('designation')">Désignation <i>{{ t.fleche('designation') }}</i></th>
            <th class="tri" (click)="t.trier('categorieNom')">Catégorie <i>{{ t.fleche('categorieNom') }}</i></th>
            <th class="tri num" (click)="t.trier('prix')">Prix <i>{{ t.fleche('prix') }}</i></th>
            <th class="tri num" (click)="t.trier('quantite')">Stock <i>{{ t.fleche('quantite') }}</i></th>
            <th class="tri num" (click)="t.trier('seuilAlerte')">Seuil <i>{{ t.fleche('seuilAlerte') }}</i></th>
            <th class="tri" (click)="t.trier('etat')">État <i>{{ t.fleche('etat') }}</i></th>
            @if (peutModifier) { <th></th> }
          </tr>
        </thead>
        <tbody>
          @for (p of t.lignes(); track p.id) {
            <tr>
              <td>{{ p.reference }}</td>
              <td>{{ p.designation }}</td>
              <td>{{ p.categorieNom }}</td>
              <td class="num">{{ p.prix | fcfa }}</td>
              <td class="num"><strong>{{ p.quantite }}</strong></td>
              <td class="num">{{ p.seuilAlerte }}</td>
              <td>
                @if (p.quantite === 0) { <span class="badge rouge">Rupture</span> }
                @else if (p.quantite <= p.seuilAlerte) { <span class="badge orange">Alerte</span> }
                @else { <span class="badge vert">OK</span> }
              </td>
              @if (peutModifier) {
                <td class="actions">
                  <button class="petit secondaire" (click)="modifier(p)">Modifier</button>
                  <button class="petit danger" (click)="supprimer(p)">Supprimer</button>
                </td>
              }
            </tr>
          } @empty {
            <tr><td colspan="8" class="vide">Aucun produit.</td></tr>
          }
        </tbody>
      </table>
      <app-pagination [t]="t" />
    </div>

    @if (peutModifier) {
      <div class="panneau">
        <h3>Catégories</h3>
        <div class="puces">
          @for (c of categories(); track c.id) { <span class="puce">{{ c.nom }}</span> }
        </div>
        <div class="barre">
          <input placeholder="Nouvelle catégorie" [(ngModel)]="nouvelleCategorie" (keyup.enter)="ajouterCategorie()" />
          <button class="secondaire" (click)="ajouterCategorie()">Ajouter</button>
        </div>
      </div>
    }
  `,
})
export class ProduitsPage implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private msg = inject(MessageService);

  produits = signal<Produit[]>([]);
  t = new DonneesTable<Produit>(() => this.produits(), 'designation', 1, {
    etat: (p) => (p.quantite === 0 ? 0 : p.quantite <= p.seuilAlerte ? 1 : 2),
  });
  categories = signal<Categorie[]>([]);
  edition = signal<FormProduit | null>(null);
  recherche = '';
  nouvelleCategorie = '';
  vue: 'tous' | 'alertes' | 'ruptures' = 'tous';

  get peutModifier() {
    return this.auth.a('GESTIONNAIRE_STOCK');
  }

  ngOnInit() {
    this.charger();
    this.chargerCategories();
  }

  changerVue(v: 'tous' | 'alertes' | 'ruptures') {
    this.vue = v;
    this.charger();
  }

  charger() {
    const chemin = this.vue === 'alertes' ? '/produits/alertes' : this.vue === 'ruptures' ? '/produits/ruptures' : '/produits';
    this.api.get<Produit[]>(chemin, this.vue === 'tous' ? { q: this.recherche.trim() } : {}).subscribe({
      next: (r) => this.produits.set(r),
      error: (e) => this.msg.erreur(e),
    });
  }

  chargerCategories() {
    this.api.get<Categorie[]>('/categories').subscribe({
      next: (r) => this.categories.set(r),
      error: (e) => this.msg.erreur(e),
    });
  }

  nouveau() {
    this.edition.set({ reference: '', designation: '', description: '', prix: 0, quantite: 0, seuilAlerte: 5, categorieId: null });
  }

  modifier(p: Produit) {
    this.edition.set({
      id: p.id, reference: p.reference, designation: p.designation, description: p.description ?? '',
      prix: p.prix, quantite: p.quantite, seuilAlerte: p.seuilAlerte, categorieId: p.categorieId,
    });
  }

  enregistrer(f: FormProduit) {
    const corps = {
      reference: f.reference.trim(), designation: f.designation.trim(), description: f.description.trim() || null,
      prix: f.prix, quantite: f.quantite, seuilAlerte: f.seuilAlerte, categorieId: f.categorieId,
    };
    const requete = f.id ? this.api.put<Produit>('/produits/' + f.id, corps) : this.api.post<Produit>('/produits', corps);
    requete.subscribe({
      next: () => {
        this.msg.ok(f.id ? 'Produit modifié' : 'Produit créé');
        this.edition.set(null);
        this.charger();
      },
      error: (e) => this.msg.erreur(e),
    });
  }

  supprimer(p: Produit) {
    if (!confirm('Supprimer le produit « ' + p.designation + ' » ?')) return;
    this.api.delete('/produits/' + p.id).subscribe({
      next: () => {
        this.msg.ok('Produit supprimé');
        this.charger();
      },
      error: (e) => this.msg.erreur(e),
    });
  }

  ajouterCategorie() {
    const nom = this.nouvelleCategorie.trim();
    if (!nom) return;
    this.api.post<Categorie>('/categories', { nom }).subscribe({
      next: () => {
        this.nouvelleCategorie = '';
        this.msg.ok('Catégorie ajoutée');
        this.chargerCategories();
      },
      error: (e) => this.msg.erreur(e),
    });
  }
}
