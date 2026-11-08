import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AlertesService } from './alertes.service';
import { Icone } from './icone';

/** Cloche d'alertes de stock : pastille avec le nombre, et panneau avec la liste. */
@Component({
  selector: 'app-cloche',
  imports: [Icone],
  host: { class: 'cloche' },
  template: `
    <button class="icone-btn" title="Alertes de stock" aria-label="Alertes de stock" (click)="ouvert.set(!ouvert())">
      <app-icone nom="cloche" />
      @if (al.nombre() > 0) {
        <span class="pastille">{{ al.nombre() }}</span>
      }
    </button>
    @if (ouvert()) {
      <div class="voile" (click)="ouvert.set(false)"></div>
      <div class="menu-alertes">
        <div class="menu-titre">
          <strong>Alertes de stock</strong>
          <span class="badge" [class.rouge]="al.nombre() > 0" [class.vert]="al.nombre() === 0">{{ al.nombre() }}</span>
        </div>
        @for (p of al.produits().slice(0, 8); track p.id) {
          <div class="ligne-alerte">
            <div><strong>{{ p.designation }}</strong><small>{{ p.reference }} · seuil {{ p.seuilAlerte }}</small></div>
            @if (p.quantite === 0) {
              <span class="badge rouge">Rupture</span>
            } @else {
              <span class="badge orange">{{ p.quantite }}</span>
            }
          </div>
        } @empty {
          <p class="vide">Aucune alerte : tous les stocks sont suffisants. 👍</p>
        }
        @if (al.nombre() > 8) {
          <small class="aide">… et {{ al.nombre() - 8 }} autre(s).</small>
        }
        <button class="plein petit" (click)="voir()">Voir les produits en alerte</button>
        @switch (al.etat()) {
          @case ('a-activer') {
            <button class="plein petit secondaire" (click)="al.activerNotifications()">🔔 Activer les notifications du navigateur</button>
          }
          @case ('actives') {
            <small class="aide">Notifications du navigateur activées.</small>
          }
          @case ('refusees') {
            <small class="aide">Notifications bloquées : autorisez-les dans les réglages du navigateur pour ce site.</small>
          }
          @case ('indisponible') {
            <small class="aide">Ce navigateur ne gère pas les notifications.</small>
          }
        }
      </div>
    }
  `,
})
export class Cloche {
  al = inject(AlertesService);
  private router = inject(Router);
  ouvert = signal(false);

  voir() {
    this.ouvert.set(false);
    this.router.navigate(['/produits'], { queryParams: { vue: 'alertes' } });
  }
}
