import { computed, signal } from '@angular/core';
import { Valeur, nombrePages, paginer, trierPar } from './tri';

/**
 * Une liste affichée dans un tableau : elle garde les données, le tri choisi et la page courante.
 * Chaque colonne triable a une clé et une fonction qui donne la valeur à comparer.
 */
export class Liste<T> {
  readonly source = signal<T[]>([]);
  readonly cle = signal<string | null>(null);
  readonly sens = signal<1 | -1>(1);
  readonly page = signal(1);

  readonly triees = computed(() => {
    const cle = this.cle();
    return trierPar(this.source(), cle ? this.accesseurs[cle] : undefined, this.sens());
  });
  readonly total = computed(() => this.source().length);
  readonly pages = computed(() => nombrePages(this.total(), this.taille));
  readonly pageCourante = computed(() => Math.min(this.page(), this.pages()));
  readonly lignes = computed(() => paginer(this.triees(), this.page(), this.taille));
  readonly debut = computed(() => (this.total() === 0 ? 0 : (this.pageCourante() - 1) * this.taille + 1));
  readonly fin = computed(() => Math.min(this.pageCourante() * this.taille, this.total()));

  constructor(
    private accesseurs: Record<string, (ligne: T) => Valeur>,
    cleInitiale: string | null = null,
    sensInitial: 1 | -1 = 1,
    readonly taille = 10,
  ) {
    this.cle.set(cleInitiale);
    this.sens.set(sensInitial);
  }

  definir(lignes: T[]) {
    this.source.set(lignes);
    if (this.page() > this.pages()) this.page.set(1);
  }

  basculer(cle: string) {
    if (this.cle() === cle) {
      this.sens.set(this.sens() === 1 ? -1 : 1);
    } else {
      this.cle.set(cle);
      this.sens.set(1);
    }
    this.page.set(1);
  }

  fleche(cle: string): string {
    return this.cle() !== cle ? '' : this.sens() === 1 ? '▲' : '▼';
  }

  aller(page: number) {
    this.page.set(Math.min(Math.max(1, page), this.pages()));
  }
}
