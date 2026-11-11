import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {provideRouter, Router} from '@angular/router';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {App} from '../app';
import {Home} from '../components/home/home';
import {Login} from '../components/login/login';
import {AuthService} from '../services/auth.service';
import {LanguageService} from '../services/language.service';

describe('Theme switch', () => {
  beforeEach(() => { localStorage.removeItem('parcel-tracker.theme'); localStorage.removeItem('parcel-tracker.language'); });
  afterEach(() => {
    localStorage.removeItem('parcel-tracker.theme'); localStorage.removeItem('parcel-tracker.language');
    document.documentElement.dataset['theme'] = 'light'; document.documentElement.dataset['bsTheme'] = 'light';
    document.documentElement.lang = 'hu';
  });
  it('toggles through the navbar, preserves the theme across navigation and translates the action label', async () => {
    localStorage.setItem('parcel-tracker.theme', 'light');
    await TestBed.configureTestingModule({imports: [App], providers: [
      provideRouter([{path: 'home', component: Home}, {path: 'login', component: Login}]),
      {provide: AuthService, useValue: {sessionChecked: signal(true), currentUser: signal(null)}}
    ]}).compileComponents();
    const fixture = TestBed.createComponent(App); const router = TestBed.inject(Router);
    await router.navigateByUrl('/home'); fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('.theme-button') as HTMLButtonElement;
    expect(button.getAttribute('aria-pressed')).toBe('false');
    expect(button.getAttribute('aria-label')).toBe('Sötét mód bekapcsolása');
    button.click(); fixture.detectChanges();
    expect(document.documentElement.dataset['theme']).toBe('dark');
    expect(button.getAttribute('aria-pressed')).toBe('true');
    expect(button.getAttribute('aria-label')).toBe('Világos mód bekapcsolása');
    await router.navigateByUrl('/login'); fixture.detectChanges();
    expect(document.documentElement.dataset['bsTheme']).toBe('dark');
    TestBed.inject(LanguageService).changeLanguage('en'); fixture.detectChanges();
    expect(button.getAttribute('aria-label')).toBe('Switch to light mode');
    button.click(); fixture.detectChanges();
    expect(document.documentElement.dataset['theme']).toBe('light');
    expect(localStorage.getItem('parcel-tracker.theme')).toBe('light');
    expect(router.url).toBe('/login');
  });
});
