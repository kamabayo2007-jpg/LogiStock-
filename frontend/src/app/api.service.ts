import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_URL } from './config';

/** Petit client HTTP : toutes les pages passent par lui pour parler à l'API. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  get<T>(chemin: string, params: Record<string, string | number | null | undefined> = {}) {
    let p = new HttpParams();
    for (const [cle, valeur] of Object.entries(params)) {
      if (valeur !== null && valeur !== undefined && valeur !== '') {
        p = p.set(cle, String(valeur));
      }
    }
    return this.http.get<T>(API_URL + chemin, { params: p });
  }

  post<T>(chemin: string, corps: unknown = {}) {
    return this.http.post<T>(API_URL + chemin, corps);
  }

  put<T>(chemin: string, corps: unknown) {
    return this.http.put<T>(API_URL + chemin, corps);
  }

  patch<T>(chemin: string, corps: unknown) {
    return this.http.patch<T>(API_URL + chemin, corps);
  }

  delete(chemin: string) {
    return this.http.delete<void>(API_URL + chemin);
  }

  blob(chemin: string) {
    return this.http.get(API_URL + chemin, { responseType: 'blob' });
  }
}
