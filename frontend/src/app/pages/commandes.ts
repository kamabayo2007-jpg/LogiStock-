import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../api.service';
import { AlertesService } from '../alertes.service';
import { AuthService } from '../auth.service';
import { FcfaPipe } from '../fcfa.pipe';
import { MessageService } from '../message.service';
import { Client, Commande, Produit } from '../models';
import { Pagination, TriMobile } from '../pagination';
import { DonneesTable } from '../table';
import { Icone } from '../icone';

interface LigneForm {
  produitId: number | null;
  quantite: number;
}

interface FormCommande {
  id?: number;
  clientId: number | null;
  lignes: LigneForm[];
}

@Component({
  selector: 'app-commandes',
  imports: [FormsModule, FcfaPipe, DatePipe, Pagination, TriMobile, Icone],
  template: `
    <div class="entete">
      <div class="titre-page"><span class="titre-ico"><app-icone nom="commandes" [taille]="22" /></span><div><h2>Commandes</h2><p class="sous-titre">Création, validation et facturation</p></div></div>
      @if (peutModifier) {
        <button (click)="nouvelle()"><app-icone nom="plus" [taille]="16" /> Nouvelle commande</button>
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
        <h3>{{ f.id ? 'Modifier la commande n°' + f.id : 'Nouvelle commande' }}</h3>
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
                <td class="num" data-label="Quantité">
                  <input class="court" type="number" min="1" [(ngModel)]="l.quantite" />
                  @if (manque(l)) { <small class="manque">Stock insuffisant (disponible : {{ stock(l.produitId) }})</small> }
                </td>
                <td class="num" data-label="Prix">{{ prix(l.produitId) | fcfa }}</td>
                <td class="num" data-label="Total">{{ prix(l.produitId) * l.quantite | fcfa }}</td>
                <td data-label=""><button class="petit danger" (click)="retirerLigne(f, $index)" aria-label="Retirer la ligne"><app-icone nom="corbeille" [taille]="14" /></button></td>
              </tr>
            }
          </tbody>
        </table>
        <div class="entete">
          <button class="secondaire" (click)="ajouterLigne(f)"><app-icone nom="panier-plus" [taille]="16" /> Ajouter un produit</button>
          <strong class="total">Total : {{ totalForm(f) | fcfa }}</strong>
        </div>
        <p class="aide">Le stock ne diminue qu'à la validation de la commande.</p>
        <div class="actions">
          <button (click)="enregistrer(f)"><app-icone nom="enregistrer" [taille]="16" /> {{ f.id ? 'Enregistrer les modifications' : 'Créer la commande' }}</button>
          <button class="secondaire" (click)="edition.set(null)"><app-icone nom="croix" [taille]="16" /> Annuler</button>
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
            <tr class="cliquable" title="Cliquer pour voir le détail" (click)="basculer(c.id)">
              <td data-label="N°">#{{ c.id }}</td>
              <td data-label="Date">{{ c.date | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td data-label="Client">{{ c.clientNom }}</td>
              <td data-label="Statut">
                <span class="badge" [class.orange]="c.statut === 'EN_COURS'" [class.vert]="c.statut === 'VALIDEE'" [class.rouge]="c.statut === 'ANNULEE'">
                  <app-icone [nom]="c.statut === 'VALIDEE' ? 'valider' : c.statut === 'ANNULEE' ? 'croix' : 'horloge'" [taille]="12" />
                  {{ libelle(c.statut) }}
                </span>
              </td>
              <td data-label="Total" class="num">{{ c.total | fcfa }}</td>
              <td data-label="" class="actions" (click)="$event.stopPropagation()">
                <button class="petit secondaire" (click)="basculer(c.id)"><app-icone nom="oeil" [taille]="16" /> {{ ouverte() === c.id ? 'Masquer' : 'Détail' }}</button>
                @if (c.statut === 'VALIDEE') {
                  <button class="petit secondaire" (click)="facture(c)"><app-icone nom="pdf" [taille]="16" /> Facture PDF</button>
                }
                @if (peutModifier && c.statut === 'EN_COURS') {
                  <button class="petit secondaire" (click)="modifier(c)"><app-icone nom="crayon" [taille]="16" /> Modifier</button>
                  <button class="petit" (click)="valider(c)"><app-icone nom="valider" [taille]="16" /> Valider</button>
                  <button class="petit danger" (click)="annuler(c)"><app-icone nom="croix" [taille]="16" /> Annuler</button>
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
  private route = inject(ActivatedRoute);

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
    // Liens du tableau de bord : ?statut=EN_COURS filtre la liste, ?ouvrir=12 ouvre le détail de la commande 12.
    this.route.queryParamMap.subscribe((q) => {
      const s = q.get('statut');
      this.filtreStatut = s === 'EN_COURS' || s === 'VALIDEE' || s === 'ANNULEE' ? s : '';
      const o = Number(q.get('ouvrir'));
      this.ouverte.set(o > 0 ? o : null);
      this.charger();
    });
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

  /** Charge une commande en cours dans le formulaire pour la corriger (client, produits, quantités). */
  modifier(c: Commande) {
    this.chargerReferentiels();
    this.edition.set({
      id: c.id,
      clientId: c.clientId,
      lignes: c.lignes.map((l) => ({ produitId: l.produitId, quantite: l.quantite })),
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  stock(produitId: number | null): number {
    return this.produits().find((p) => p.id === produitId)?.quantite ?? 0;
  }

  /** Prévient avant la validation : la quantité demandée dépasse le stock actuel. */
  manque(l: LigneForm): boolean {
    return l.produitId !== null && (l.quantite || 0) > this.stock(l.produitId);
  }

  enregistrer(f: FormCommande) {
    if (!f.clientId) {
      this.msg.erreur({ error: { message: 'Choisissez un client' } });
      return;
    }
    const lignes = f.lignes.filter((l) => l.produitId !== null);
    const corps = { clientId: f.clientId, lignes };
    const requete = f.id
      ? this.api.put<Commande>('/commandes/' + f.id, corps)
      : this.api.post<Commande>('/commandes', corps);
    requete.subscribe({
      next: (c) => {
        this.msg.ok(f.id ? 'Commande n°' + c.id + ' modifiée' : 'Commande n°' + c.id + ' créée (en cours)');
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
