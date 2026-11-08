import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { AuthService } from '../auth.service';
import { FcfaPipe } from '../fcfa.pipe';
import { MessageService } from '../message.service';
import { Client, Commande } from '../models';
import { Pagination, TriMobile } from '../pagination';
import { DonneesTable } from '../table';

interface FormClient {
  id?: number;
  nom: string;
  prenom: string;
  telephone: string;
  email: string;
  adresse: string;
}

@Component({
  selector: 'app-clients',
  imports: [FormsModule, FcfaPipe, DatePipe, Pagination, TriMobile],
  template: `
    <div class="entete">
      <div><h2>Clients</h2><p class="sous-titre">Fichier client et historique d'achats</p></div>
      @if (peutModifier) {
        <button (click)="nouveau()">+ Nouveau client</button>
      }
    </div>

    <div class="barre">
      <input type="search" placeholder="Rechercher par nom ou prénom…" [(ngModel)]="recherche" (ngModelChange)="charger()" />
    </div>

    @if (edition(); as f) {
      <div class="panneau">
        <h3>{{ f.id ? 'Modifier le client' : 'Nouveau client' }}</h3>
        <div class="grille">
          <label>Nom (ou société) <input [(ngModel)]="f.nom" /></label>
          <label>Prénom (particulier) <input [(ngModel)]="f.prenom" /></label>
          <label>Téléphone <input [(ngModel)]="f.telephone" /></label>
          <label>E-mail <input type="email" [(ngModel)]="f.email" /></label>
          <label class="large">Adresse <input [(ngModel)]="f.adresse" /></label>
        </div>
        <div class="actions">
          <button (click)="enregistrer(f)">Enregistrer</button>
          <button class="secondaire" (click)="edition.set(null)">Annuler</button>
        </div>
      </div>
    }

    <app-tri-mobile [t]="t" [colonnes]="[{ cle: 'nom', label: 'Nom' }, { cle: 'telephone', label: 'Téléphone' }, { cle: 'email', label: 'E-mail' }, { cle: 'adresse', label: 'Adresse' }]" />

    <div class="tableau">
      <table>
        <thead>
          <tr>
            <th class="tri" (click)="t.trier('nom')">Nom <i>{{ t.fleche('nom') }}</i></th>
            <th class="tri" (click)="t.trier('telephone')">Téléphone <i>{{ t.fleche('telephone') }}</i></th>
            <th class="tri" (click)="t.trier('email')">E-mail <i>{{ t.fleche('email') }}</i></th>
            <th class="tri" (click)="t.trier('adresse')">Adresse <i>{{ t.fleche('adresse') }}</i></th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (c of t.lignes(); track c.id) {
            <tr>
              <td data-label="Nom">{{ c.prenom ? c.prenom + ' ' : '' }}{{ c.nom }}</td>
              <td data-label="Téléphone">{{ c.telephone }}</td>
              <td data-label="E-mail">{{ c.email }}</td>
              <td data-label="Adresse">{{ c.adresse }}</td>
              <td data-label="" class="actions">
                <button class="petit secondaire" (click)="voirHistorique(c)">Historique</button>
                @if (peutModifier) {
                  <button class="petit secondaire" (click)="modifier(c)">Modifier</button>
                  <button class="petit danger" (click)="supprimer(c)">Supprimer</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="vide">Aucun client.</td></tr>
          }
        </tbody>
      </table>
      <app-pagination [t]="t" />
    </div>

    @if (historique(); as h) {
      <div class="panneau">
        <h3>Historique d'achats : {{ h.client.prenom ? h.client.prenom + ' ' : '' }}{{ h.client.nom }}</h3>
        <table>
          <thead><tr><th>Commande</th><th>Date</th><th>Statut</th><th class="num">Total</th></tr></thead>
          <tbody>
            @for (c of h.commandes; track c.id) {
              <tr>
                <td data-label="Commande">#{{ c.id }}</td>
                <td data-label="Date">{{ c.date | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td data-label="Statut">{{ c.statut }}</td>
                <td data-label="Total" class="num">{{ c.total | fcfa }}</td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="vide">Ce client n'a pas encore de commande.</td></tr>
            }
          </tbody>
        </table>
        <div class="actions"><button class="secondaire" (click)="historique.set(null)">Fermer</button></div>
      </div>
    }
  `,
})
export class ClientsPage implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private msg = inject(MessageService);

  clients = signal<Client[]>([]);
  t = new DonneesTable<Client>(() => this.clients(), 'nom');
  edition = signal<FormClient | null>(null);
  historique = signal<{ client: Client; commandes: Commande[] } | null>(null);
  recherche = '';

  get peutModifier() {
    return this.auth.a('COMMERCIAL');
  }

  ngOnInit() {
    this.charger();
  }

  charger() {
    this.api.get<Client[]>('/clients', { q: this.recherche.trim() }).subscribe({
      next: (r) => this.clients.set(r),
      error: (e) => this.msg.erreur(e),
    });
  }

  nouveau() {
    this.edition.set({ nom: '', prenom: '', telephone: '', email: '', adresse: '' });
  }

  modifier(c: Client) {
    this.edition.set({
      id: c.id, nom: c.nom, prenom: c.prenom ?? '', telephone: c.telephone ?? '', email: c.email ?? '', adresse: c.adresse ?? '',
    });
  }

  enregistrer(f: FormClient) {
    const corps = { nom: f.nom, prenom: f.prenom, telephone: f.telephone, email: f.email, adresse: f.adresse };
    const requete = f.id ? this.api.put<Client>('/clients/' + f.id, corps) : this.api.post<Client>('/clients', corps);
    requete.subscribe({
      next: () => {
        this.msg.ok(f.id ? 'Client modifié' : 'Client créé');
        this.edition.set(null);
        this.charger();
      },
      error: (e) => this.msg.erreur(e),
    });
  }

  supprimer(c: Client) {
    if (!confirm('Supprimer le client « ' + c.nom + ' » ?')) return;
    this.api.delete('/clients/' + c.id).subscribe({
      next: () => {
        this.msg.ok('Client supprimé');
        this.charger();
      },
      error: (e) => this.msg.erreur(e),
    });
  }

  voirHistorique(c: Client) {
    this.api.get<Commande[]>('/clients/' + c.id + '/commandes').subscribe({
      next: (r) => this.historique.set({ client: c, commandes: r }),
      error: (e) => this.msg.erreur(e),
    });
  }
}
