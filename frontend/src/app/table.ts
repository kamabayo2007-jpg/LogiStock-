import { computed, signal } from '@angular/core';

type Acces<T> = Record<string, (ligne: T) => string | number | null>;

/**
 * Données d'un tableau : tri par colonne (clic sur l'en-tête) et pagination.
 * `source` est la liste complète ; `acces` permet de trier sur une valeur calculée.
 */
export class DonneesTable<T> {
  readonly cle = signal('');
  readonly sens = signal<1 | -1>(1);
  readonly page = signal(1);
  readonly taille = signal(10);

  private source: () => T[];
  private acces: Acces<T>;

  readonly triees = computed(() => {
    const liste = [...this.source()];
    const cle = this.cle();
    if (!cle) {
      return liste;
    }
    const sens = this.sens();
    const valeur = (l: T): string | number | null => {
      if (this.acces[cle]) {
        return this.acces[cle](l);
      }
      const brut = (l as unknown as Record<string, unknown>)[cle];
      return brut === undefined || brut === null ? null : (brut as string | number);
    };
    return liste.sort((a, b) => {
      const x = valeur(a);
      const y = valeur(b);
      if (x === null && y === null) return 0;
      if (x === null) return 1;
      if (y === null) return -1;
      if (typeof x === 'number' && typeof y === 'number') return (x - y) * sens;
      return String(x).localeCompare(String(y), 'fr', { numeric: true, sensitivity: 'base' }) * sens;
    });
  });

  readonly total = computed(() => this.triees().length);
  readonly nbPages = computed(() => Math.max(1, Math.ceil(this.total() / this.taille())));
  readonly pageCourante = computed(() => Math.min(this.page(), this.nbPages()));
  readonly lignes = computed(() => {
    const debut = (this.pageCourante() - 1) * this.taille();
    return this.triees().slice(debut, debut + this.taille());
  });

  constructor(source: () => T[], cleInitiale = '', sens: 1 | -1 = 1, acces: Acces<T> = {}) {
    this.source = source;
    this.acces = acces;
    this.cle.set(cleInitiale);
    this.sens.set(sens);
  }

  trier(cle: string) {
    if (this.cle() === cle) {
      this.sens.set(this.sens() === 1 ? -1 : 1);
    } else {
      this.cle.set(cle);
      this.sens.set(1);
    }
    this.page.set(1);
  }

  fleche(cle: string): string {
    return this.cle() === cle ? (this.sens() === 1 ? '▲' : '▼') : '↕';
  }

  precedent() {
    this.page.set(Math.max(1, this.pageCourante() - 1));
  }

  suivant() {
    this.page.set(Math.min(this.nbPages(), this.pageCourante() + 1));
  }

  changerTaille(valeur: string) {
    this.taille.set(Number(valeur) || 10);
    this.page.set(1);
  }
}
