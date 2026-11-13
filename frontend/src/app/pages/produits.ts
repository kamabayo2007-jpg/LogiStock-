import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { AlertesService } from '../alertes.service';
import { AuthService } from '../auth.service';
import { FcfaPipe } from '../fcfa.pipe';
import { MessageService } from '../message.service';
import { Categorie, Produit } from '../models';
import { Pagination, TriMobile } from '../pagination';
import { DonneesTable } from '../table';
import { Icone } from '../icone';

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
  imports: [FormsModule, FcfaPipe, Pagination, TriMobile, Icone],
  template: `
    <div class="entete">
      <div class="titre-page"><span class="titre-ico"><app-icone nom="produits" [taille]="22" /></span><div><h2>Produits</h2><p class="sous-titre">Catalogue et niveaux de stock</p></div></div>
      @if (peutModifier) {
        <button (click)="nouveau()"><app-icone nom="plus" [taille]="16" /> Nouveau produit</button>
      }
    </div>

    <div class="barre">
      <input type="search" placeholder="Rechercher (désignation ou référence)…" [(ngModel)]="recherche" (ngModelChange)="charger()" />
      @if (!commercial) {
        <div class="onglets">
          <button [class.actif]="vue === 'tous'" (click)="changerVue('tous')"><app-icone nom="produits" [taille]="14" /> Tous</button>
          <button [class.actif]="vue === 'alertes'" (click)="changerVue('alertes')"><app-icone nom="alerte" [taille]="14" /> Alertes</button>
          <button [class.actif]="vue === 'ruptures'" (click)="changerVue('ruptures')"><app-icone nom="croix" [taille]="14" /> Ruptures</button>
        </div>
      }
    </div>

    @if (commercial) {
      <div class="onglets categories">
        <button [class.actif]="filtreCat() === null" (click)="filtreCat.set(null)">Toutes</button>
        @for (c of categories(); track c.id) {
          <button [class.actif]="filtreCat() === c.id" (click)="filtreCat.set(c.id)">{{ c.nom }}</button>
        }
      </div>
      @for (g of groupes(); track g.nom) {
        <section class="panneau">
          <h3><app-icone nom="etiquette" [taille]="16" /> {{ g.nom }} <span class="badge">{{ g.produits.length }}</span></h3>
          <div class="tableau">
            <table>
              <thead><tr><th>Référence</th><th>Désignation</th><th class="num">Prix</th><th class="num">Stock disponible</th></tr></thead>
              <tbody>
                @for (p of g.produits; track p.id) {
                  <tr>
                    <td data-label="Référence">{{ p.reference }}</td>
                    <td data-label="Désignation">{{ p.designation }}</td>
                    <td data-label="Prix" class="num">{{ p.prix | fcfa }}</td>
                    <td data-label="Stock disponible" class="num"><strong>{{ p.quantite }}</strong></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      } @empty {
        <p class="vide">Aucun produit.</p>
      }
    }

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
          <button (click)="enregistrer(f)"><app-icone nom="enregistrer" [taille]="16" /> Enregistrer</button>
          <button class="secondaire" (click)="edition.set(null)"><app-icone nom="croix" [taille]="16" /> Annuler</button>
        </div>
      </div>
    }

    @if (!commercial) {
    <app-tri-mobile [t]="t" [colonnes]="[{ cle: 'designation', label: 'Désignation' }, { cle: 'reference', label: 'Référence' }, { cle: 'categorieNom', label: 'Catégorie' }, { cle: 'prix', label: 'Prix' }, { cle: 'quantite', label: 'Stock' }, { cle: 'etat', label: 'État' }]" />

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
              <td data-label="Référence">{{ p.reference }}</td>
              <td data-label="Désignation">{{ p.designation }}</td>
              <td data-label="Catégorie">{{ p.categorieNom }}</td>
              <td data-label="Prix" class="num">{{ p.prix | fcfa }}</td>
              <td data-label="Stock" class="num"><strong>{{ p.quantite }}</strong></td>
              <td data-label="Seuil" class="num">{{ p.seuilAlerte }}</td>
              <td data-label="État">
                @if (p.quantite === 0) { <span class="badge rouge"><app-icone nom="croix" [taille]="12" /> Rupture</span> }
                @else if (p.quantite <= p.seuilAlerte) { <span class="badge orange"><app-icone nom="alerte" [taille]="12" /> Alerte</span> }
                @else { <span class="badge vert"><app-icone nom="valider" [taille]="12" /> OK</span> }
              </td>
              @if (peutModifier) {
                <td data-label="" class="actions">
                  <button class="petit secondaire" (click)="modifier(p)"><app-icone nom="crayon" [taille]="16" /> Modifier</button>
                  <button class="petit danger" (click)="supprimer(p)"><app-icone nom="corbeille" [taille]="16" /> Supprimer</button>
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
    }

    @if (peutModifier) {
      <div class="panneau">
        <h3>Catégories</h3>
        <div class="puces">
          @for (c of categories(); track c.id) { <span class="puce">{{ c.nom }}</span> }
        </div>
        <div class="barre">
          <input placeholder="Nouvelle catégorie" [(ngModel)]="nouvelleCategorie" (keyup.enter)="ajouterCategorie()" />
          <button class="secondaire" (click)="ajouterCategorie()"><app-icone nom="plus" [taille]="16" /> Ajouter</button>
        </div>
      </div>
    }
  `,
})
export class ProduitsPage implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private msg = inject(MessageService);
  private alertes = inject(AlertesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

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

  /** Le commercial voit le catalogue classé par catégorie, sans alertes ni ruptures. */
  get commercial() {
    return this.auth.a('COMMERCIAL');
  }

  filtreCat = signal<number | null>(null);

  groupes = computed(() => {
    const parNom = new Map<string, Produit[]>();
    for (const p of this.produits()) {
      if (this.filtreCat() !== null && p.categorieId !== this.filtreCat()) continue;
      const liste = parNom.get(p.categorieNom) ?? [];
      liste.push(p);
      parNom.set(p.categorieNom, liste);
    }
    return [...parNom.entries()]
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([nom, produits]) => ({
        nom,
        produits: [...produits].sort((a, b) => a.designation.localeCompare(b.designation)),
      }));
  });

  ngOnInit() {
    // ?vue=alertes (lien de la cloche) ouvre directement la liste des produits en alerte.
    this.route.queryParamMap.subscribe((q) => {
      const v = q.get('vue');
      this.vue = !this.commercial && (v === 'alertes' || v === 'ruptures') ? v : 'tous';
      this.charger();
    });
    this.chargerCategories();
  }

  changerVue(v: 'tous' | 'alertes' | 'ruptures') {
    this.router.navigate([], { queryParams: { vue: v === 'tous' ? null : v } });
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
        this.alertes.rafraichir();
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
        this.alertes.rafraichir();
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
