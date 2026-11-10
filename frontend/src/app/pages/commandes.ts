import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { AlertesService } from '../alertes.service';
import { AuthService } from '../auth.service';
import { FcfaPipe } from '../fcfa.pipe';
import { MessageService } from '../message.service';
import { Client, Commande, Produit } from '../models';
import { Pagination, TriMobile } from '../pagination';
import { DonneesTable } from '../table';

interface LigneForm {
  produitId: number | null;
  quantite: number;
}

interface FormCommande {
  clientId: number | null;
  lignes: LigneForm[];
}

@Component({
  selector: 'app-commandes',
  imports: [FormsModule, FcfaPipe, DatePipe, Pagination, TriMobile],
  template: `
    <div class="entete">
      <div><h2>Commandes</h2><p class="sous-titre">Création, validation et facturation</p></div>
      @if (peutModifier) {
        <button (click)="nouvelle()">+ Nouvelle commande</button>
      }
    </div>

    <div class="barre">
      <select [(ngModel)]="filtreStatut" (ngModelChange)="charger()">
        <option value="">Tous les statuts</option>
        <option value="EN_COURS">En cours</option>
        <option value="VALIDEE">Validées</option>
        <option value="ANNULEE">Annulées</option>
      </select>
    </div>

    @if (edition(); as f) {
      <div class="panneau">
        <h3>Nouvelle commande</h3>
        <label>Client
          <select [(ngModel)]="f.clientId">
            <option [ngValue]="null">— choisir —</option>
            @for (c of clients(); track c.id) {
              <option [ngValue]="c.id">{{ c.prenom ? c.prenom + ' ' : '' }}{{ c.nom }}</option>
            }
          </select>
        </label>
        <table>
          <thead><tr><th>Produit</th><th class="num">Quantité</th><th class="num">Prix</th><th class="num">Total</th><th></th></tr></thead>
          <tbody>
            @for (l of f.lignes; track $index) {
              <tr>
                <td data-label="Produit">
                  <select [(ngModel)]="l.produitId">
                    <option [ngValue]="null">— choisir —</option>
                    @for (p of produits(); track p.id) {
                      <option [ngValue]="p.id">{{ p.designation }} (stock : {{ p.quantite }})</option>
                    }
                  </select>
                </td>
                <td class="num" data-label="Quantité"><input class="court" type="number" min="1" [(ngModel)]="l.quantite" /></td>
                <td class="num" data-label="Prix">{{ prix(l.produitId) | fcfa }}</td>
                <td class="num" data-label="Total">{{ prix(l.produitId) * l.quantite | fcfa }}</td>
                <td data-label=""><button class="petit danger" (click)="retirerLigne(f, $index)">×</button></td>
              </tr>
            }
          </tbody>
        </table>
        <div class="entete">
          <button class="secondaire" (click)="ajouterLigne(f)">+ Ajouter un produit</button>
          <strong class="total">Total : {{ totalForm(f) | fcfa }}</strong>
        </div>
        <p class="aide">Le stock ne diminue qu'à la validation de la commande.</p>
        <div class="actions">
          <button (click)="creer(f)">Créer la commande</button>
          <button class="secondaire" (click)="edition.set(null)">Annuler</button>
        </div>
      </div>
    }

    <app-tri-mobile [t]="t" [colonnes]="[{ cle: 'date', label: 'Date' }, { cle: 'id', label: 'N°' }, { cle: 'clientNom', label: 'Client' }, { cle: 'statut', label: 'Statut' }, { cle: 'total', label: 'Total' }]" />

    <div class="tableau">
      <table>
        <thead>
          <tr>
            <th class="tri" (click)="t.trier('id')">N° <i>{{ t.fleche('id') }}</i></th>
            <th class="tri" (click)="t.trier('date')">Date <i>{{ t.fleche('date') }}</i></th>
            <th class="tri" (click)="t.trier('clientNom')">Client <i>{{ t.fleche('clientNom') }}</i></th>
            <th class="tri" (click)="t.trier('statut')">Statut <i>{{ t.fleche('statut') }}</i></th>
            <th class="tri num" (click)="t.trier('total')">Total <i>{{ t.fleche('total') }}</i></th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (c of t.lignes(); track c.id) {
            <tr>
              <td data-label="N°">#{{ c.id }}</td>
              <td data-label="Date">{{ c.date | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td data-label="Client">{{ c.clientNom }}</td>
              <td data-label="Statut">
                <span class="badge" [class.orange]="c.statut === 'EN_COURS'" [class.vert]="c.statut === 'VALIDEE'" [class.rouge]="c.statut === 'ANNULEE'">
                  {{ libelle(c.statut) }}
                </span>
              </td>
              <td data-label="Total" class="num">{{ c.total | fcfa }}</td>
              <td data-label="" class="actions">
                <button class="petit secondaire" (click)="basculer(c.id)">{{ ouverte() === c.id ? 'Masquer' : 'Détail' }}</button>
                @if (c.statut === 'VALIDEE') {
                  <button class="petit secondaire" (click)="facture(c)">Facture PDF</button>
                }
                @if (peutModifier && c.statut === 'EN_COURS') {
                  <button class="petit" (click)="valider(c)">Valider</button>
                  <button class="petit danger" (click)="annuler(c)">Annuler</button>
                }
              </td>
            </tr>
            @if (ouverte() === c.id) {
              <tr class="detail">
                <td colspan="6">
                  <table>
                    <thead><tr><th>Référence</th><th>Désignation</th><th class="num">Qté</th><th class="num">Prix unitaire</th><th class="num">Total</th></tr></thead>
                    <tbody>
                      @for (l of c.lignes; track l.produitId) {
                        <tr>
                          <td data-label="Référence">{{ l.reference }}</td><td data-label="Désignation">{{ l.designation }}</td>
                          <td data-label="Qté" class="num">{{ l.quantite }}</td>
                          <td data-label="Prix unitaire" class="num">{{ l.prixUnitaire | fcfa }}</td>
                          <td data-label="Total" class="num">{{ l.total | fcfa }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </td>
              </tr>
            }
          } @empty {
            <tr><td colspan="6" class="vide">Aucune commande.</td></tr>
          }
        </tbody>
      </table>
      <app-pagination [t]="t" />
    </div>
  `,
})
export class CommandesPage implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private msg = inject(MessageService);
  private alertes = inject(AlertesService);

  commandes = signal<Commande[]>([]);
  t = new DonneesTable<Commande>(() => this.commandes(), 'date', -1);
  clients = signal<Client[]>([]);
  produits = signal<Produit[]>([]);
  edition = signal<FormCommande | null>(null);
  ouverte = signal<number | null>(null);
  filtreStatut = '';

  get peutModifier() {
    return this.auth.a('COMMERCIAL');
  }

  ngOnInit() {
    this.charger();
    if (this.peutModifier) {
      this.chargerReferentiels();
    }
  }

  chargerReferentiels() {
    this.api.get<Client[]>('/clients').subscribe({ next: (r) => this.clients.set(r), error: (e) => this.msg.erreur(e) });
    this.api.get<Produit[]>('/produits').subscribe({ next: (r) => this.produits.set(r), error: (e) => this.msg.erreur(e) });
  }

  charger() {
    this.api.get<Commande[]>('/commandes', { statut: this.filtreStatut }).subscribe({
      next: (r) => this.commandes.set(r),
      error: (e) => this.msg.erreur(e),
    });
  }

  libelle(statut: string): string {
    return statut === 'EN_COURS' ? 'En cours' : statut === 'VALIDEE' ? 'Validée' : 'Annulée';
  }

  basculer(id: number) {
    this.ouverte.set(this.ouverte() === id ? null : id);
  }

  nouvelle() {
    this.chargerReferentiels();
    this.edition.set({ clientId: null, lignes: [{ produitId: null, quantite: 1 }] });
  }

  ajouterLigne(f: FormCommande) {
    f.lignes.push({ produitId: null, quantite: 1 });
  }

  retirerLigne(f: FormCommande, index: number) {
    f.lignes.splice(index, 1);
  }

  prix(produitId: number | null): number {
    return this.produits().find((p) => p.id === produitId)?.prix ?? 0;
  }

  totalForm(f: FormCommande): number {
    return f.lignes.reduce((somme, l) => somme + this.prix(l.produitId) * (l.quantite || 0), 0);
  }

  creer(f: FormCommande) {
    if (!f.clientId) {
      this.msg.erreur({ error: { message: 'Choisissez un client' } });
      return;
    }
    const lignes = f.lignes.filter((l) => l.produitId !== null);
    this.api.post<Commande>('/commandes', { clientId: f.clientId, lignes }).subscribe({
      next: (c) => {
        this.msg.ok('Commande n°' + c.id + ' créée (en cours)');
        this.edition.set(null);
        this.charger();
      },
      error: (e) => this.msg.erreur(e),
    });
  }

  valider(c: Commande) {
    if (!confirm('Valider la commande n°' + c.id + ' ? Le stock sera diminué et une facture sera émise.')) return;
    this.api.post<Commande>('/commandes/' + c.id + '/validation').subscribe({
      next: () => {
        this.msg.ok('Commande n°' + c.id + ' validée : stock mis à jour, facture émise');
        this.charger();
        this.chargerReferentiels();
        this.alertes.rafraichir();
      },
      error: (e) => this.msg.erreur(e),
    });
  }

  annuler(c: Commande) {
    if (!confirm('Annuler la commande n°' + c.id + ' ?')) return;
    this.api.post<Commande>('/commandes/' + c.id + '/annulation').subscribe({
      next: () => {
        this.msg.ok('Commande n°' + c.id + ' annulée');
        this.charger();
      },
      error: (e) => this.msg.erreur(e),
    });
  }

  /** Télécharge le PDF avec le token (un simple lien ne pourrait pas envoyer l'en-tête Authorization). */
  facture(c: Commande) {
    this.api.blob('/commandes/' + c.id + '/facture.pdf').subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60000);
      },
      error: () => this.msg.erreur(null, 'Aucune facture pour cette commande'),
    });
  }
}
