import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { AuthService } from '../auth.service';
import { MessageService } from '../message.service';
import { ROLES, Role, Utilisateur, libelleRole } from '../models';
import { Pagination } from '../pagination';
import { DonneesTable } from '../table';

interface FormCompte {
  id?: number;
  nom: string;
  email: string;
  motDePasse: string;
  role: Role;
}

@Component({
  selector: 'app-comptes',
  imports: [FormsModule, Pagination],
  template: `
    <div class="entete">
      <div><h2>Comptes utilisateurs</h2><p class="sous-titre">Accès à l'application et rôles</p></div>
      <button (click)="nouveau()">+ Nouveau compte</button>
    </div>

    @if (edition(); as f) {
      <div class="panneau">
        <h3>{{ f.id ? 'Modifier le compte' : 'Nouveau compte' }}</h3>
        <div class="grille">
          <label>Nom <input [(ngModel)]="f.nom" /></label>
          <label>E-mail <input type="email" [(ngModel)]="f.email" /></label>
          <label>Rôle
            <select [(ngModel)]="f.role">
              @for (r of roles; track r.code) {
                <option [value]="r.code">{{ r.libelle }}</option>
              }
            </select>
          </label>
          <label>Mot de passe {{ f.id ? '(laisser vide pour ne pas le changer)' : '(8 caractères minimum)' }}
            <input type="password" [(ngModel)]="f.motDePasse" />
          </label>
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
            <th class="tri" (click)="t.trier('nom')">Nom <i>{{ t.fleche('nom') }}</i></th>
            <th class="tri" (click)="t.trier('email')">E-mail <i>{{ t.fleche('email') }}</i></th>
            <th class="tri" (click)="t.trier('role')">Rôle <i>{{ t.fleche('role') }}</i></th>
            <th class="tri" (click)="t.trier('actif')">État <i>{{ t.fleche('actif') }}</i></th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (u of t.lignes(); track u.id) {
            <tr>
              <td>{{ u.nom }}</td>
              <td>{{ u.email }}</td>
              <td>{{ role(u.role) }}</td>
              <td>
                <span class="badge" [class.vert]="u.actif" [class.rouge]="!u.actif">{{ u.actif ? 'Actif' : 'Désactivé' }}</span>
              </td>
              <td class="actions">
                <button class="petit secondaire" (click)="modifier(u)">Modifier</button>
                <button class="petit" [class.danger]="u.actif" [class.secondaire]="!u.actif" (click)="basculer(u)">
                  {{ u.actif ? 'Désactiver' : 'Réactiver' }}
                </button>
              </td>
            </tr>
          }
        </tbody>
      </table>
      <app-pagination [t]="t" />
    </div>
  `,
})
export class ComptesPage implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private msg = inject(MessageService);

  comptes = signal<Utilisateur[]>([]);
  t = new DonneesTable<Utilisateur>(() => this.comptes(), 'nom', 1, { actif: (u) => (u.actif ? 1 : 0) });
  edition = signal<FormCompte | null>(null);
  roles = ROLES;
  role = libelleRole;

  ngOnInit() {
    this.charger();
  }

  charger() {
    this.api.get<Utilisateur[]>('/users').subscribe({ next: (r) => this.comptes.set(r), error: (e) => this.msg.erreur(e) });
  }

  nouveau() {
    this.edition.set({ nom: '', email: '', motDePasse: '', role: 'COMMERCIAL' });
  }

  modifier(u: Utilisateur) {
    this.edition.set({ id: u.id, nom: u.nom, email: u.email, motDePasse: '', role: u.role });
  }

  enregistrer(f: FormCompte) {
    const corps = { nom: f.nom, email: f.email, motDePasse: f.motDePasse || null, role: f.role };
    const requete = f.id ? this.api.put<Utilisateur>('/users/' + f.id, corps) : this.api.post<Utilisateur>('/users', corps);
    requete.subscribe({
      next: () => {
        this.msg.ok(f.id ? 'Compte modifié' : 'Compte créé');
        this.edition.set(null);
        this.charger();
      },
      error: (e) => this.msg.erreur(e),
    });
  }

  basculer(u: Utilisateur) {
    this.api.patch<Utilisateur>('/users/' + u.id + '/actif', { actif: !u.actif }).subscribe({
      next: () => {
        this.msg.ok(u.actif ? 'Compte désactivé' : 'Compte réactivé');
        this.charger();
      },
      error: (e) => this.msg.erreur(e),
    });
  }
}
