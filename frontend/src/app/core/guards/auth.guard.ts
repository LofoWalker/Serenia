import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {AuthStateService} from '../services/auth-state.service';

export const authGuard: CanActivateFn = () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (authState.token()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};

export const guestGuard: CanActivateFn = () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (!authState.token()) {
    return true;
  }

  router.navigate(['/chat']);
  return false;
};

