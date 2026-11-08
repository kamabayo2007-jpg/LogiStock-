import { Pipe, PipeTransform } from '@angular/core';

/** 125000 → « 125 000 FCFA ». */
@Pipe({ name: 'fcfa' })
export class FcfaPipe implements PipeTransform {
  transform(valeur: number | null | undefined): string {
    const n = Math.round(Number(valeur ?? 0));
    return n.toLocaleString('fr-FR').replace(/[  ]/g, ' ') + ' FCFA';
  }
}
