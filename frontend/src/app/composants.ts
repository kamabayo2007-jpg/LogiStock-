import { Component, Input } from '@angular/core';
import { Liste } from './liste';

/** En-tête de colonne cliquable : <th tri="prix" [liste]="produits">Prix</th> */
@Component({
  selector: 'th[tri]',
  template: `<ng-content /><span class="fleche">{{ liste.fleche(tri) }}</span>`,
  host: {
    class: 'triable',
    '(click)': 'liste.basculer(tri)',
    '[attr.aria-sort]': "liste.cle() === tri ? (liste.sens() === 1 ? 'ascending' : 'descending') : 'none'",
  },
})
export class TriColonne {
  @Input({ required: true }) tri!: string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  @Input({ required: true }) liste!: Liste<any>;
}

/** Barre « 1–10 sur 47 · Précédent · Page 1/5 · Suivant » sous un tableau. */
@Component({
  selector: 'app-pagination',
  template: `
    <div class="pagination">
      <span>
        @if (liste.total() === 0) { Aucun résultat }
        @else { {{ liste.debut() }}–{{ liste.fin() }} sur {{ liste.total() }} }
      </span>
      @if (liste.pages() > 1) {
        <div class="pages">
          <button class="petit secondaire" [disabled]="liste.pageCourante() === 1" (click)="liste.aller(liste.pageCourante() - 1)">‹ Précédent</button>
          <span>Page {{ liste.pageCourante() }} / {{ liste.pages() }}</span>
          <button class="petit secondaire" [disabled]="liste.pageCourante() === liste.pages()" (click)="liste.aller(liste.pageCourante() + 1)">Suivant ›</button>
        </div>
      }
    </div>
  `,
})
export class Pagination {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  @Input({ required: true }) liste!: Liste<any>;
}

/** Petites icônes du menu (tracés simples, couleur du texte). */
@Component({
  selector: 'app-icone',
  template: `
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      @switch (nom) {
        @case ('accueil') { <rect x="3" y="3" width="7" height="9" /><rect x="14" y="3" width="7" height="5" /><rect x="14" y="12" width="7" height="9" /><rect x="3" y="16" width="7" height="5" /> }
        @case ('produits') { <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" /><polyline points="3.27 6.96 12 12.01 20.73 6.96" /><line x1="12" y1="22.08" x2="12" y2="12" /> }
        @case ('mouvements') { <polyline points="17 1 21 5 17 9" /><path d="M3 11V9a4 4 0 0 1 4-4h14" /><polyline points="7 23 3 19 7 15" /><path d="M21 13v2a4 4 0 0 1-4 4H3" /> }
        @case ('clients') { <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" /> }
        @case ('commandes') { <circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" /><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" /> }
        @case ('comptes') { <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /> }
      }
    </svg>
  `,
})
export class Icone {
  @Input() nom = '';
}
