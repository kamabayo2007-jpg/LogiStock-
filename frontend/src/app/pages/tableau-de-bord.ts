import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { AuthService } from '../auth.service';
import { FcfaPipe } from '../fcfa.pipe';
import { Icone } from '../icone';
import { MessageService } from '../message.service';
import { Client, Commande, Mouvement, Produit } from '../models';

interface BarreMois {
  cle: string;
  label: string;
  montant: number;
  pct: number;
}

interface BarreH {
  nom: string;
  valeur: number;
  texte: string;
  pct: number;
}

const MOIS = ['Janv.', 'Févr.', 'Mars', 'Avr.', 'Mai', 'Juin', 'Juil.', 'Août', 'Sept.', 'Oct.', 'Nov.', 'Déc.'];

/** Tableau de bord (US15 à US18) : les blocs affichés dépendent du rôle. */
@Component({
  selector: 'app-tableau-de-bord',
  imports: [FcfaPipe, DatePipe, RouterLink, Icone],
  template: `
    <div class="entete">
      <div>
        <h2>Bonjour, {{ prenom }} 👋</h2>
        <p class="sous-titre">{{ aujourdhui }}</p>
      </div>
    </div>

    <div class="kpis">
      @if (voitVentes) {
        <div class="kpi">
          <div class="kpi-ico vert"><app-icone nom="argent" /></div>
          <div><small>Chiffre d'affaires</small><strong>{{ chiffreAffaires() | fcfa }}</strong>
            <span class="sous">{{ validees().length }} commande(s) validée(s)</span></div>
        </div>
        <div class="kpi">
          <div class="kpi-ico orange"><app-icone nom="horloge" /></div>
          <div><small>Commandes en cours</small><strong>{{ enCours() }}</strong>
            <span class="sous">à valider ou annuler</span></div>
        </div>
        <div class="kpi">
          <div class="kpi-ico violet"><app-icone nom="clients" /></div>
          <div><small>Clients</small><strong>{{ clients().length }}</strong>
            <span class="sous">dans le fichier client</span></div>
        </div>
      }
      <div class="kpi">
        <div class="kpi-ico bleu"><app-icone nom="produits" /></div>
        <div><small>Produits au catalogue</small><strong>{{ produits().length }}</strong>
          <span class="sous">{{ unites() }} unités en stock</span></div>
      </div>
      @if (voitStock) {
        <div class="kpi">
          <div class="kpi-ico bleu"><app-icone nom="argent" /></div>
          <div><small>Valeur du stock</small><strong>{{ valeurStock() | fcfa }}</strong>
            <span class="sous">au prix de vente</span></div>
        </div>
      }
      <div class="kpi">
        <div class="kpi-ico rouge"><app-icone nom="alerte" /></div>
        <div><small>Alertes de stock</small><strong>{{ alertes().length }}</strong>
          <span class="sous">dont {{ ruptures() }} en rupture</span></div>
      </div>
    </div>

    <div class="grille-cartes">
      @if (voitVentes) {
        <section class="carte large">
          <h3>Ventes des 6 derniers mois</h3>
          <div class="histogramme">
            @for (m of ventesParMois(); track m.cle) {
              <div class="colonne-wrap">
                <span class="valeur">{{ abrege(m.montant) }}</span>
                <div class="colonne"><div class="rempli" [style.height.%]="m.pct"></div></div>
                <small>{{ m.label }}</small>
              </div>
            }
          </div>
        </section>

        <section class="carte">
          <h3>Commandes par statut</h3>
          <div class="donut-wrap">
            <div class="donut" [style.background]="degrade()"><div class="trou">{{ commandes().length }}</div></div>
            <ul class="legende">
              <li><i class="p vert"></i> Validées <strong>{{ validees().length }}</strong></li>
              <li><i class="p orange"></i> En cours <strong>{{ enCours() }}</strong></li>
              <li><i class="p rouge"></i> Annulées <strong>{{ annulees() }}</strong></li>
            </ul>
          </div>
        </section>

        <section class="carte">
          <h3>Produits les plus vendus</h3>
          @for (b of topProduits(); track b.nom) {
            <div class="hbar">
              <div class="hbar-t"><span>{{ b.nom }}</span><strong>{{ b.texte }}</strong></div>
              <div class="piste"><div class="jauge" [style.width.%]="b.pct"></div></div>
            </div>
          } @empty {
            <p class="vide">Aucune vente validée pour le moment.</p>
          }
        </section>
      }

      <section class="carte">
        <h3>Valeur du stock par catégorie</h3>
        @for (b of stockParCategorie(); track b.nom) {
          <div class="hbar">
            <div class="hbar-t"><span>{{ b.nom }}</span><strong>{{ b.texte }}</strong></div>
            <div class="piste"><div class="jauge bleu" [style.width.%]="b.pct"></div></div>
          </div>
        } @empty {
          <p class="vide">Aucun produit.</p>
        }
      </section>

      @if (voitStock) {
        <section class="carte">
          <h3>Mouvements des 30 derniers jours</h3>
          <div class="duo">
            <div><small>Entrées</small><strong class="vert-t">+{{ entrees30() }}</strong></div>
            <div><small>Sorties</small><strong class="orange-t">−{{ sorties30() }}</strong></div>
            <div><small>Mouvements</small><strong>{{ mouvements().length }}</strong></div>
          </div>
          <a routerLink="/mouvements" class="lien">Voir l'historique complet →</a>
        </section>
      }

      <section class="carte">
        <h3>Produits à réapprovisionner</h3>
        @for (p of aReapprovisionner(); track p.id) {
          <div class="ligne-alerte">
            <div><strong>{{ p.designation }}</strong><small>{{ p.reference }} · seuil {{ p.seuilAlerte }}</small></div>
            @if (p.quantite === 0) {
              <span class="badge rouge">Rupture</span>
            } @else {
              <span class="badge orange">{{ p.quantite }} restant(s)</span>
            }
          </div>
        } @empty {
          <p class="vide">Tous les stocks sont au-dessus du seuil d'alerte. 👍</p>
        }
        <a routerLink="/produits" class="lien">Voir tous les produits →</a>
      </section>

      @if (voitVentes) {
        <section class="carte large">
          <h3>Dernières commandes</h3>
          <div class="tableau sans-cadre">
            <table>
              <thead><tr><th>N°</th><th>Date</th><th>Client</th><th>Statut</th><th class="num">Total</th></tr></thead>
              <tbody>
                @for (c of dernieres(); track c.id) {
                  <tr>
                    <td>#{{ c.id }}</td>
                    <td>{{ c.date | date: 'dd/MM/yyyy HH:mm' }}</td>
                    <td>{{ c.clientNom }}</td>
                    <td>
                      <span class="badge" [class.orange]="c.statut === 'EN_COURS'" [class.vert]="c.statut === 'VALIDEE'" [class.rouge]="c.statut === 'ANNULEE'">
                        {{ c.statut === 'EN_COURS' ? 'En cours' : c.statut === 'VALIDEE' ? 'Validée' : 'Annulée' }}
                      </span>
                    </td>
                    <td class="num">{{ c.total | fcfa }}</td>
                  </tr>
                } @empty {
                  <tr><td colspan="5" class="vide">Aucune commande.</td></tr>
                }
              </tbody>
            </table>
          </div>
          <a routerLink="/commandes" class="lien">Toutes les commandes →</a>
        </section>
      }
    </div>
  `,
})
export class TableauDeBordPage implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private msg = inject(MessageService);

  produits = signal<Produit[]>([]);
  commandes = signal<Commande[]>([]);
  clients = signal<Client[]>([]);
  mouvements = signal<Mouvement[]>([]);

  prenom = (this.auth.utilisateur()?.nom ?? '').split(/\s+/)[0];
  aujourdhui = new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

  get voitVentes() {
    return this.auth.a('ADMINISTRATEUR') || this.auth.a('COMMERCIAL');
  }

  get voitStock() {
    return this.auth.a('ADMINISTRATEUR') || this.auth.a('GESTIONNAIRE_STOCK');
  }

  // ----- Indicateurs -----
  unites = computed(() => this.produits().reduce((s, p) => s + p.quantite, 0));
  valeurStock = computed(() => this.produits().reduce((s, p) => s + p.prix * p.quantite, 0));
  alertes = computed(() => this.produits().filter((p) => p.quantite <= p.seuilAlerte));
  ruptures = computed(() => this.produits().filter((p) => p.quantite === 0).length);
  validees = computed(() => this.commandes().filter((c) => c.statut === 'VALIDEE'));
  enCours = computed(() => this.commandes().filter((c) => c.statut === 'EN_COURS').length);
  annulees = computed(() => this.commandes().filter((c) => c.statut === 'ANNULEE').length);
  chiffreAffaires = computed(() => this.validees().reduce((s, c) => s + c.total, 0));
  dernieres = computed(() => this.commandes().slice(0, 5));
  aReapprovisionner = computed(() =>
    [...this.alertes()].sort((a, b) => a.quantite - b.quantite || a.designation.localeCompare(b.designation)).slice(0, 6),
  );

  entrees30 = computed(() => this.somme30('ENTREE'));
  sorties30 = computed(() => this.somme30('SORTIE'));

  ventesParMois = computed<BarreMois[]>(() => {
    const maintenant = new Date();
    const mois: BarreMois[] = [];
    for (let i = 5; i >= 0; i--) {
      const d = new Date(maintenant.getFullYear(), maintenant.getMonth() - i, 1);
      const cle = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
      mois.push({ cle, label: MOIS[d.getMonth()], montant: 0, pct: 0 });
    }
    for (const c of this.validees()) {
      const m = mois.find((x) => x.cle === c.date.slice(0, 7));
      if (m) m.montant += c.total;
    }
    const max = Math.max(...mois.map((m) => m.montant), 1);
    return mois.map((m) => ({ ...m, pct: m.montant === 0 ? 2 : Math.max(4, (m.montant / max) * 100) }));
  });

  topProduits = computed<BarreH[]>(() => {
    const parProduit = new Map<number, { nom: string; montant: number }>();
    for (const c of this.validees()) {
      for (const l of c.lignes) {
        const actuel = parProduit.get(l.produitId) ?? { nom: l.designation, montant: 0 };
        actuel.montant += l.total;
        parProduit.set(l.produitId, actuel);
      }
    }
    const tri = [...parProduit.values()].sort((a, b) => b.montant - a.montant).slice(0, 5);
    const max = Math.max(...tri.map((t) => t.montant), 1);
    return tri.map((t) => ({ nom: t.nom, valeur: t.montant, texte: this.abrege(t.montant), pct: (t.montant / max) * 100 }));
  });

  stockParCategorie = computed<BarreH[]>(() => {
    const parCat = new Map<string, number>();
    for (const p of this.produits()) {
      parCat.set(p.categorieNom, (parCat.get(p.categorieNom) ?? 0) + p.prix * p.quantite);
    }
    const tri = [...parCat.entries()].sort((a, b) => b[1] - a[1]);
    const max = Math.max(...tri.map((t) => t[1]), 1);
    return tri.map(([nom, valeur]) => ({ nom, valeur, texte: this.abrege(valeur), pct: (valeur / max) * 100 }));
  });

  degrade = computed(() => {
    const total = this.commandes().length;
    if (total === 0) return '#e5e7eb';
    const a = (this.validees().length / total) * 100;
    const b = a + (this.enCours() / total) * 100;
    return `conic-gradient(#16a34a 0 ${a}%, #f59e0b ${a}% ${b}%, #dc2626 ${b}% 100%)`;
  });

  ngOnInit() {
    this.api.get<Produit[]>('/produits').subscribe({ next: (r) => this.produits.set(r), error: (e) => this.msg.erreur(e) });
    if (this.voitVentes) {
      this.api.get<Commande[]>('/commandes').subscribe({ next: (r) => this.commandes.set(r), error: (e) => this.msg.erreur(e) });
      this.api.get<Client[]>('/clients').subscribe({ next: (r) => this.clients.set(r), error: (e) => this.msg.erreur(e) });
    }
    if (this.voitStock) {
      this.api.get<Mouvement[]>('/mouvements').subscribe({ next: (r) => this.mouvements.set(r), error: (e) => this.msg.erreur(e) });
    }
  }

  /** 1 250 000 → « 1,3 M » ; 85 000 → « 85 k » : lisible dans un petit graphique. */
  abrege(n: number): string {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace('.', ',') + ' M';
    if (n >= 1_000) return Math.round(n / 1_000) + ' k';
    return String(Math.round(n));
  }

  private somme30(type: 'ENTREE' | 'SORTIE'): number {
    const limite = Date.now() - 30 * 24 * 3600 * 1000;
    return this.mouvements()
      .filter((m) => m.type === type && new Date(m.date).getTime() >= limite)
      .reduce((s, m) => s + m.quantite, 0);
  }
}
