import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {firstValueFrom, Observable} from 'rxjs';
import {describe, expect, it} from 'vitest';
import {roleGuard} from './role.guard';
import {AuthService} from '../services/auth.service';

describe('roleGuard', () => {
  it('waits for session restoration before making the access decision', async () => {
    const checked = signal(false);
    TestBed.configureTestingModule({providers: [provideRouter([]), {provide: AuthService, useValue: {
      sessionChecked: checked, currentUser: signal({role: 'ADMIN'})
    }}]});
    const result = TestBed.runInInjectionContext(() => roleGuard(
      Object.assign(new ActivatedRouteSnapshot(), {data: {role: 'ADMIN'}}), {} as RouterStateSnapshot));
    let finished = false;
    const pending = firstValueFrom(result as Observable<boolean | UrlTree>).then(value => { finished = true; return value; });
    TestBed.tick();
    expect(finished).toBe(false);
    checked.set(true); TestBed.tick();
    expect(await pending).toBe(true);
  });
  it.each([
    ['ADMIN', 'ADMIN', true], ['COURIER', 'COURIER', true], ['CUSTOMER', 'ADMIN', '/home'],
    [null, 'ADMIN', '/login']
  ])('checks user %s against required role %s', async (role, required, expected) => {
    TestBed.configureTestingModule({providers: [provideRouter([]), {provide: AuthService, useValue: {
      sessionChecked: signal(true), currentUser: signal(role ? {role} : null)
    }}]});
    const result = TestBed.runInInjectionContext(() => roleGuard(
      Object.assign(new ActivatedRouteSnapshot(), {data: {role: required}}), {} as RouterStateSnapshot));
    const pending = firstValueFrom(result as Observable<boolean | UrlTree>);
    TestBed.tick();
    const value = await pending;
    expect(value === true ? true : TestBed.inject(Router).serializeUrl(value as UrlTree)).toBe(expected);
  });
});
