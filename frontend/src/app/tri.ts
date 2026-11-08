/** Fonctions pures de tri et de pagination (sans Angular, donc faciles à tester). */

export type Valeur = string | number | null | undefined;

/** Compare deux valeurs : nombres dans l'ordre numérique, textes dans l'ordre alphabétique français ; les vides en dernier. */
export function comparer(a: Valeur, b: Valeur): number {
  const videA = a === null || a === undefined || a === '';
  const videB = b === null || b === undefined || b === '';
  if (videA && videB) return 0;
  if (videA) return 1;
  if (videB) return -1;
  if (typeof a === 'number' && typeof b === 'number') return a - b;
  return String(a).localeCompare(String(b), 'fr', { sensitivity: 'base', numeric: true });
}

/** Renvoie une copie triée (l'original ne change pas). sens : 1 = croissant, -1 = décroissant. */
export function trierPar<T>(lignes: T[], acces: ((l: T) => Valeur) | undefined, sens: 1 | -1): T[] {
  if (!acces) return lignes.slice();
  return lignes
    .map((ligne, index) => ({ ligne, index }))
    .sort((x, y) => {
      const c = comparer(acces(x.ligne), acces(y.ligne));
      // Les vides restent en dernier quel que soit le sens ; à égalité, l'ordre d'origine est conservé.
      const vide = (v: Valeur) => v === null || v === undefined || v === '';
      if (vide(acces(x.ligne)) || vide(acces(y.ligne))) return c || x.index - y.index;
      return c * sens || x.index - y.index;
    })
    .map((x) => x.ligne);
}

export function nombrePages(total: number, taille: number): number {
  return Math.max(1, Math.ceil(total / taille));
}

export function paginer<T>(lignes: T[], page: number, taille: number): T[] {
  const p = Math.min(Math.max(1, page), nombrePages(lignes.length, taille));
  return lignes.slice((p - 1) * taille, p * taille);
}
