import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { MessageService } from './message.service';
import { Produit } from './models';

export type EtatNotifications = 'actives' | 'refusees' | 'a-activer' | 'indisponible';

/**
 * Alertes de stock (US8) : interroge l'API toutes les minutes, alimente la cloche,
 * prévient à la connexion et envoie une notification du navigateur quand un produit passe sous son seuil.
 */
@Injectable({ providedIn: 'root' })
export class AlertesService {
  private api = inject(ApiService);
  private msg = inject(MessageService);

  readonly produits = signal<Produit[]>([]);
  readonly nombre = computed(() => this.produits().length);
  /** Produits (id -> quantité) dont l'alerte a été lue : une nouvelle baisse de stock redevient « non lue ». */
  private lues = signal<Record<number, number>>(this.lireLues());
  /** Nombre affiché sur la pastille de la cloche : alertes pas encore lues. */
  readonly nonLues = computed(() => this.produits().filter((p) => this.lues()[p.id] !== p.quantite).length);
  readonly ruptures = computed(() => this.produits().filter((p) => p.quantite === 0).length);
  readonly etat = signal<EtatNotifications>(this.lireEtat());

  private minuteur: ReturnType<typeof setInterval> | undefined;
  private connus = new Map<number, number>(); // produit -> quantité vue à la dernière vérification
  private premiere = true;

  demarrer() {
    if (this.minuteur) return;
    this.rafraichir();
    this.minuteur = setInterval(() => this.rafraichir(), 60000);
  }

  /** Marque toutes les alertes actuelles comme lues (à l'ouverture de la cloche). */
  marquerLues() {
    const lues: Record<number, number> = {};
    for (const p of this.produits()) lues[p.id] = p.quantite;
    this.lues.set(lues);
    try {
      localStorage.setItem('logistock.alertes.lues', JSON.stringify(lues));
    } catch {
      /* stockage indisponible : la lecture reste valable jusqu'au rechargement */
    }
  }

  private lireLues(): Record<number, number> {
    try {
      return JSON.parse(localStorage.getItem('logistock.alertes.lues') ?? '{}');
    } catch {
      return {};
    }
  }

  arreter() {
    clearInterval(this.minuteur);
    this.minuteur = undefined;
    this.produits.set([]);
    this.connus.clear();
    this.premiere = true;
  }

  rafraichir() {
    this.api.get<Produit[]>('/produits/alertes').subscribe({
      next: (liste) => {
        this.produits.set(liste);
        this.signaler(liste);
      },
      error: () => {
        /* silencieux : une erreur réseau ne doit pas gêner l'utilisateur toutes les minutes */
      },
    });
  }

  /** À appeler depuis un clic : les navigateurs exigent une action de l'utilisateur. */
  async activerNotifications() {
    if (typeof Notification === 'undefined') {
      this.etat.set('indisponible');
      return;
    }
    try {
      if ('serviceWorker' in navigator) {
        await navigator.serviceWorker.register('sw.js');
      }
      await Notification.requestPermission();
    } catch {
      /* on retombe sur l'état réel ci-dessous */
    }
    this.etat.set(this.lireEtat());
  }

  private signaler(liste: Produit[]) {
    if (this.premiere) {
      this.premiere = false;
      if (liste.length > 0) {
        const r = liste.filter((p) => p.quantite === 0).length;
        this.msg.alerte(
          '⚠ ' + liste.length + ' produit(s) sous le seuil d\'alerte' + (r > 0 ? ' (dont ' + r + ' en rupture)' : '') + '.',
        );
      }
    } else {
      // Nouveau produit en alerte, ou stock qui a encore baissé depuis la dernière vérification.
      const nouveaux = liste.filter((p) => {
        const avant = this.connus.get(p.id);
        return avant === undefined || p.quantite < avant;
      });
      for (const p of nouveaux) {
        const texte = p.quantite === 0 ? p.designation + ' est en rupture de stock' : p.designation + ' : plus que ' + p.quantite + ' en stock';
        this.msg.alerte('⚠ ' + texte);
        this.notifierNavigateur('Alerte de stock', texte);
      }
    }
    this.connus = new Map(liste.map((p) => [p.id, p.quantite]));
  }

  private async notifierNavigateur(titre: string, corps: string) {
    if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return;
    try {
      const reg = 'serviceWorker' in navigator ? await navigator.serviceWorker.getRegistration() : undefined;
      if (reg) {
        await reg.showNotification(titre, { body: corps, tag: 'alerte-stock' });
      } else {
        new Notification(titre, { body: corps });
      }
    } catch {
      /* navigateur sans notifications : la cloche et le message restent */
    }
  }

  private lireEtat(): EtatNotifications {
    if (typeof Notification === 'undefined') return 'indisponible';
    return Notification.permission === 'granted' ? 'actives' : Notification.permission === 'denied' ? 'refusees' : 'a-activer';
  }
}
