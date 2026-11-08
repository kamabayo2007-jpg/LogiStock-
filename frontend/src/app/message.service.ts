import { Injectable, signal } from '@angular/core';

export interface Message {
  type: 'ok' | 'erreur';
  texte: string;
}

/** Message affiché en haut de l'écran (succès ou erreur de l'API). */
@Injectable({ providedIn: 'root' })
export class MessageService {
  readonly courant = signal<Message | null>(null);
  private minuteur: ReturnType<typeof setTimeout> | undefined;

  ok(texte: string) {
    this.afficher({ type: 'ok', texte });
  }

  erreur(e: unknown, parDefaut = 'Une erreur est survenue') {
    const err = e as { status?: number; error?: { message?: string } };
    let texte = parDefaut;
    if (err?.status === 0) {
      texte = "Impossible de joindre le serveur : l'API est-elle démarrée ?";
    } else if (err?.error && typeof err.error.message === 'string') {
      texte = err.error.message;
    }
    this.afficher({ type: 'erreur', texte });
  }

  fermer() {
    this.courant.set(null);
  }

  private afficher(m: Message) {
    this.courant.set(m);
    clearTimeout(this.minuteur);
    this.minuteur = setTimeout(() => this.courant.set(null), m.type === 'ok' ? 4000 : 8000);
  }
}
