import {Component, ElementRef, inject, signal} from '@angular/core';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {LanguageService} from '../../services/language.service';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  imports: [
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  host: {
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown.escape)': 'closeProfileMenu()'
  }
})
export class Navbar {

  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly languageService = inject(LanguageService);

  private readonly router = inject(Router);
  protected readonly authService = inject(AuthService);
  protected readonly language = this.languageService.language;
  protected readonly menuOpen = signal(false);
  protected readonly profileMenuOpen = signal(false);
  protected errorMessage = '';

  protected toggleMenu(): void {
    this.menuOpen.update(open => !open);
    this.closeProfileMenu();
  }

  protected toggleProfileMenu(): void {
    this.profileMenuOpen.update(open => !open);
  }

  protected closeProfileMenu(): void {
    this.profileMenuOpen.set(false);
  }

  protected onDocumentClick(event: MouseEvent): void {
    const profileMenu =
      this.elementRef.nativeElement.querySelector('.profile-menu');

    if (
      event.target instanceof Node &&
      !profileMenu?.contains(event.target)
    ) {
      this.closeProfileMenu();
    }
  }

  protected changeLanguage(language: 'hu' | 'en'): void {
    this.languageService.changeLanguage(language);
  }

  protected logout() {
    this.errorMessage = '';
    this.authService.logout().subscribe({
      next: () => {
        this.closeProfileMenu();
        this.router.navigate(['/']);
      },
      error: () => {
        this.errorMessage = 'A kijelentkezés most nem sikerült. Próbáld újra később';
      }
    });
  }
}
