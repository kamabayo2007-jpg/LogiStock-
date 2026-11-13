import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { AlertesService } from '../alertes.service';
import { AuthService } from '../auth.service';
import { MessageService } from '../message.service';
import { Mouvement, Produit } from '../models';
import { Pagination, TriMobile } from '../pagination';
import { DonneesTable } from '../table';
import { Icone } from '../icone';

@Component({
  selector: 'app-mouvements',
  imports: [FormsModule, DatePipe, Pagination, TriMobile, Icone],
  template: `
    <div class="entete"><div class="titre-page"><span class="titre-ico"><app-icone nom="mouvements" [taille]="22" /></span><div><h2>Mouvements de stock</h2><p class="sous-titre">Entrées, sorties et historique complet</p></div></div></div>

    @if (peutEnregistrer) {
      <div class="panneau">
        <h3>Enregistrer un mouvement</h3>
        <div class="grille">
          <label>Produit
            <select [(ngModel)]="form.produitId">
              <option [ngValue]="null">— choisir —</option>
              @for (p of produits(); track p.id) {
                <option [ngValue]="p.id">{{ p.designation }} (stock : {{ p.quantite }})</option>
              }
            </select>
          </label>
          <label>Type
            <select [(ngModel)]="form.type">
              <option value="ENTREE">Entrée (réception)</option>
              <option value="SORTIE">Sortie</option>
            </select>
          </label>
          <label>Quantité <input type="number" min="1" [(ngModel)]="form.quantite" /></label>
          <label>Motif <input [(ngModel)]="form.motif" placeholder="ex. Réception fournisseur" /></label>
        </div>
        <div class="actions"><button (click)="enregistrer()"><app-icone nom="enregistrer" [taille]="16" /> Enregistrer</button></div>
      </div>
    }

    <div class="barre">
      <select [(ngModel)]="filtreProduit" (ngModelChange)="charger()">
        <option [ngValue]="null">Tous les produits</option>
        @for (p of produits(); track p.id) {
          <option [ngValue]="p.id">{{ p.designation }}</option>
        }
      </select>
      <select [(ngModel)]="filtreType" (ngModelChange)="charger()">
        <option value="">Entrées et sorties</option>
        <option value="ENTREE">Entrées</option>
        <option value="SORTIE">Sorties</option>
      </select>
    </div>

    <app-tri-mobile [t]="t" [colonnes]="[{ cle: 'date', label: 'Date' }, { cle: 'produit', label: 'Produit' }, { cle: 'type', label: 'Type' }, { cle: 'quantite', label: 'Quantité' }, { cle: 'commandeId', label: 'Commande' }]" />

    <div class="tableau">
      <table>
        <thead>
          <tr>
            <th class="tri" (click)="t.trier('date')">Date <i>{{ t.fleche('date') }}</i></th>
            <th class="tri" (click)="t.trier('produit')">Produit <i>{{ t.fleche('produit') }}</i></th>
            <th class="tri" (click)="t.trier('type')">Type <i>{{ t.fleche('type') }}</i></th>
            <th class="tri num" (click)="t.trier('quantite')">Quantité <i>{{ t.fleche('quantite') }}</i></th>
            <th class="tri" (click)="t.trier('motif')">Motif <i>{{ t.fleche('motif') }}</i></th>
            <th class="tri" (click)="t.trier('commandeId')">Commande <i>{{ t.fleche('commandeId') }}</i></th>
          </tr>
        </thead>
        <tbody>
          @for (m of t.lignes(); track m.id) {
            <tr>
              <td data-label="Date">{{ m.date | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td data-label="Produit">{{ nomProduit(m.produitId) }}</td>
              <td data-label="Type">
                <span class="badge" [class.vert]="m.type === 'ENTREE'" [class.orange]="m.type === 'SORTIE'">
                  <app-icone [nom]="m.type === 'ENTREE' ? 'entree' : 'depart'" [taille]="12" />
                  {{ m.type === 'ENTREE' ? 'Entrée' : 'Sortie' }}
                </span>
              </td>
              <td data-label="Quantité" class="num">{{ m.quantite }}</td>
              <td data-label="Motif">{{ m.motif }}</td>
              <td data-label="Commande">{{ m.commandeId ? '#' + m.commandeId : '' }}</td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="vide">Aucun mouvement.</td></tr>
          }
        </tbody>
      </table>
      <app-pagination [t]="t" />
    </div>
  `,
})
export class MouvementsPage implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private msg = inject(MessageService);
  private alertes = inject(AlertesService);

  produits = signal<Produit[]>([]);
  mouvements = signal<Mouvement[]>([]);
  t = new DonneesTable<Mouvement>(() => this.mouvements(), 'date', -1, {
    produit: (m) => this.nomProduit(m.produitId),
  });
  filtreProduit: number | null = null;
  filtreType = '';
  form: { produitId: number | null; type: 'ENTREE' | 'SORTIE'; quantite: number; motif: string } = {
    produitId: null, type: 'ENTREE', quantite: 1, motif: '',
  };

  get peutEnregistrer() {
    return this.auth.a('GESTIONNAIRE_STOCK');
  }

  ngOnInit() {
    this.chargerProduits();
    this.charger();
  }

  chargerProduits() {
    this.api.get<Produit[]>('/produits').subscribe({ next: (r) => this.produits.set(r), error: (e) => this.msg.erreur(e) });
  }

  charger() {
    this.api.get<Mouvement[]>('/mouvements', { produitId: this.filtreProduit, type: this.filtreType }).subscribe({
      next: (r) => this.mouvements.set(r),
      error: (e) => this.msg.erreur(e),
    });
  }

  nomProduit(id: number): string {
    return this.produits().find((p) => p.id === id)?.designation ?? '#' + id;
  }

  enregistrer() {
    if (!this.form.produitId) {
      this.msg.erreur({ error: { message: 'Choisissez un produit' } });
      return;
    }
    this.api.post('/mouvements', { ...this.form, motif: this.form.motif.trim() || null }).subscribe({
      next: () => {
        this.msg.ok('Mouvement enregistré');
        this.form = { produitId: null, type: 'ENTREE', quantite: 1, motif: '' };
        this.chargerProduits();
        this.charger();
        this.alertes.rafraichir();
      },
      error: (e) => this.msg.erreur(e),
    });
  }
}
