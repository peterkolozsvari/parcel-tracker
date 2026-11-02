import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {toObservable} from '@angular/core/rxjs-interop';
import {filter, map, take} from 'rxjs';
import {AuthService} from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return toObservable(auth.sessionChecked).pipe(
    filter(checked => checked), take(1),
    map(() => auth.currentUser() ? true : router.createUrlTree(['/login']))
  );
};
