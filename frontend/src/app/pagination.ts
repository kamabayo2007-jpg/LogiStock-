import { Component, input } from '@angular/core';
import { DonneesTable } from './table';
import { Icone } from './icone';

@Component({
  selector: 'app-pagination',
  imports: [Icone],
  template: `
    <div class="pagination">
      <span>{{ t().total() }} résultat{{ t().total() > 1 ? 's' : '' }}</span>
      <div class="pages">
        <label class="inline">Lignes
          <select [value]="t().taille()" (change)="t().changerTaille($any($event.target).value)">
            <option value="5">5</option>
            <option value="10">10</option>
            <option value="25">25</option>
            <option value="50">50</option>
          </select>
        </label>
        <button class="petit secondaire" (click)="t().precedent()" [disabled]="t().pageCourante() === 1" aria-label="Page précédente"><app-icone nom="gauche" [taille]="16" /></button>
        <span>Page {{ t().pageCourante() }} / {{ t().nbPages() }}</span>
        <button class="petit secondaire" (click)="t().suivant()" [disabled]="t().pageCourante() === t().nbPages()" aria-label="Page suivante"><app-icone nom="droite" [taille]="16" /></button>
      </div>
    </div>
  `,
})
export class Pagination {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  t = input.required<DonneesTable<any>>();
}

/** Sur téléphone, les en-têtes de colonnes disparaissent : ce menu les remplace pour trier. */
@Component({
  selector: 'app-tri-mobile',
  template: `
    <div class="tri-mobile">
      <label class="inline">Trier par
        <select [value]="t().cle()" (change)="t().choisir($any($event.target).value)">
          @for (c of colonnes(); track c.cle) {
            <option [value]="c.cle">{{ c.label }}</option>
          }
        </select>
      </label>
      <button class="petit secondaire" (click)="t().inverser()">{{ t().sens() === 1 ? '▲ croissant' : '▼ décroissant' }}</button>
    </div>
  `,
})
export class TriMobile {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  t = input.required<DonneesTable<any>>();
  colonnes = input.required<{ cle: string; label: string }[]>();
}
