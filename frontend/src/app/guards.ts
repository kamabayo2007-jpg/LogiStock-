import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './models';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.utilisateur() ? true : inject(Router).createUrlTree(['/login']);
};

/** Autorise seulement certains rôles ; sinon retour au tableau de bord. */
export const roleGuard =
  (roles: Role[]): CanActivateFn =>
  () => {
    const role = inject(AuthService).utilisateur()?.role;
    return role && roles.includes(role) ? true : inject(Router).createUrlTree(['/tableau-de-bord']);
  };
