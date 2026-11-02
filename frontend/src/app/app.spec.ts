import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {provideRouter} from '@angular/router';
import {App} from './app';
import {AuthService} from './services/auth.service';
import {Role} from './models/user.model';

describe('Application navigation', () => {
  async function render(role: Role | null) {
    await TestBed.configureTestingModule({imports: [App], providers: [provideRouter([]),
      {provide: AuthService, useValue: {
        sessionChecked: signal(true), currentUser: signal(role ? {id: 1, fullName: 'Test User', role} : null)
      }}]
    }).compileComponents();
    const fixture = TestBed.createComponent(App); fixture.detectChanges(); return fixture;
  }
  it('shows login for an anonymous visitor', async () => {
    const fixture = await render(null);
    expect(fixture.nativeElement.querySelector('a[href="/login"]')?.textContent).toContain('Bejelentkezés');
    expect(fixture.nativeElement.querySelector('button[aria-label="Profilmenü"]')).toBeNull();
  });
  it('shows user management only for an administrator', async () => {
    const fixture = await render('ADMIN');
    fixture.nativeElement.querySelector('button[aria-label="Profilmenü"]').click(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[routerLink="/admin/users"]')?.textContent).toContain('Felhasználókezelés');
    expect(fixture.nativeElement.querySelector('a[href="/login"]')).toBeNull();
  });
});
