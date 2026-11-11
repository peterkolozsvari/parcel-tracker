import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {provideRouter, Router} from '@angular/router';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {App} from '../app';
import {Home} from '../components/home/home';
import {Login} from '../components/login/login';
import {AuthService} from '../services/auth.service';

describe('HU / EN navigation', () => {
  beforeEach(() => localStorage.removeItem('parcel-tracker.language'));
  afterEach(() => { localStorage.removeItem('parcel-tracker.language'); document.documentElement.lang = 'hu'; });

  it('switches the open page without reloading, carries language across routes and preserves form values', async () => {
    await TestBed.configureTestingModule({imports: [App], providers: [
      provideRouter([{path: 'home', component: Home}, {path: 'login', component: Login}]),
      {provide: AuthService, useValue: {sessionChecked: signal(true), currentUser: signal(null), login: vi.fn()}}
    ]}).compileComponents();
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/home'); fixture.detectChanges(); await fixture.whenStable();
    const button = (text: string): HTMLButtonElement => Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .find(element => element.textContent?.trim() === text)!;
    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Üdvözlünk');
    button('EN').click(); fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Welcome to Parcel Tracker!');
    expect(fixture.nativeElement.querySelector('a[href="/shipments/new"]').textContent).toContain('Send a parcel');
    expect(button('EN').getAttribute('aria-pressed')).toBe('true');
    await router.navigateByUrl('/login'); fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('label[for="email"]').textContent).toContain('Email address');
    const email = fixture.nativeElement.querySelector('input[type="email"]') as HTMLInputElement;
    email.value = 'teszt@example.hu'; email.dispatchEvent(new Event('input')); fixture.detectChanges();
    button('HU').click(); fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('label[for="email"]').textContent).toContain('Email-cím');
    expect(email.value).toBe('teszt@example.hu'); expect(router.url).toBe('/login');
  });
});
