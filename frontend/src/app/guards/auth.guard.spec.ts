import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {firstValueFrom, Observable} from 'rxjs';
import {describe, expect, it} from 'vitest';
import {authGuard} from './auth.guard';
import {AuthService} from '../services/auth.service';

describe('authGuard', () => {
  it.each(['CUSTOMER', 'COURIER', 'ADMIN', null])('checks signed-in state for %s', async role => {
    TestBed.configureTestingModule({providers: [provideRouter([]), {provide: AuthService, useValue: {
      sessionChecked: signal(true), currentUser: signal(role ? {id: 1, role} : null)
    }}]});
    const result = TestBed.runInInjectionContext(() => authGuard(new ActivatedRouteSnapshot(), {} as RouterStateSnapshot));
    const pending = firstValueFrom(result as Observable<boolean | UrlTree>);
    TestBed.tick();
    const value = await pending;
    expect(value === true ? true : TestBed.inject(Router).serializeUrl(value as UrlTree)).toBe(role ? true : '/login');
  });

  it('waits for session restoration before redirecting', async () => {
    const checked = signal(false);
    const user = signal<{id: number} | null>(null);
    TestBed.configureTestingModule({providers: [provideRouter([]), {provide: AuthService, useValue: {
      sessionChecked: checked, currentUser: user
    }}]});
    const result = TestBed.runInInjectionContext(() => authGuard(new ActivatedRouteSnapshot(), {} as RouterStateSnapshot));
    let done = false;
    const pending = firstValueFrom(result as Observable<boolean | UrlTree>).then(value => {done = true; return value;});
    TestBed.tick(); expect(done).toBe(false);
    user.set({id: 1}); checked.set(true); TestBed.tick();
    expect(await pending).toBe(true);
  });
});
