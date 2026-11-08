import { Component, input } from '@angular/core';
import { DonneesTable } from './table';

@Component({
  selector: 'app-pagination',
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
        <button class="petit secondaire" (click)="t().precedent()" [disabled]="t().pageCourante() === 1">‹</button>
        <span>Page {{ t().pageCourante() }} / {{ t().nbPages() }}</span>
        <button class="petit secondaire" (click)="t().suivant()" [disabled]="t().pageCourante() === t().nbPages()">›</button>
      </div>
    </div>
  `,
})
export class Pagination {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  t = input.required<DonneesTable<any>>();
}
