import {Component, inject, signal} from '@angular/core';
import {RouterLink, RouterLinkActive} from '@angular/router';
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
})
export class Navbar {

  private readonly languageService = inject(LanguageService);
  protected readonly authService = inject(AuthService);

  protected readonly language = this.languageService.language;
  protected readonly menuOpen = signal(false);

  protected toggleMenu(): void {
    this.menuOpen.update(open => !open);
  }

  protected changeLanguage(language: 'hu' | 'en'): void {
    this.languageService.changeLanguage(language);
  }
}
