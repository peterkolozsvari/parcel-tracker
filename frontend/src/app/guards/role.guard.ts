import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {toObservable} from '@angular/core/rxjs-interop';
import {filter, map, take} from 'rxjs';
import {AuthService} from '../services/auth.service';

export const roleGuard: CanActivateFn = route => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return toObservable(auth.sessionChecked).pipe(
    filter(checked => checked), take(1),
    map(() => {
      const user = auth.currentUser();
      if (!user) return router.createUrlTree(['/login']);
      return user.role === route.data['role'] ? true : router.createUrlTree(['/home']);
    })
  );
};
