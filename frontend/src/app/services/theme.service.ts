import {DOCUMENT} from '@angular/common';
import {computed, DestroyRef, inject, Injectable, signal} from '@angular/core';

export type Theme = 'light' | 'dark';
const STORAGE_KEY = 'parcel-tracker.theme';

@Injectable({providedIn: 'root'})
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly media = this.document.defaultView?.matchMedia?.('(prefers-color-scheme: dark)');
  private savedTheme = this.readPreference();
  private readonly selectedTheme = signal<Theme>(this.savedTheme ?? (this.media?.matches ? 'dark' : 'light'));
  readonly theme = this.selectedTheme.asReadonly();
  readonly isDark = computed(() => this.theme() === 'dark');

  constructor() {
    this.applyTheme();
    const onSystemChange = (event: MediaQueryListEvent): void => {
      if (this.savedTheme === null) {
        this.selectedTheme.set(event.matches ? 'dark' : 'light');
        this.applyTheme();
      }
    };
    this.media?.addEventListener('change', onSystemChange);
    inject(DestroyRef).onDestroy(() => this.media?.removeEventListener('change', onSystemChange));
  }

  toggle(): void {
    this.savedTheme = this.isDark() ? 'light' : 'dark';
    this.selectedTheme.set(this.savedTheme);
    this.applyTheme();
    try { this.document.defaultView?.localStorage.setItem(STORAGE_KEY, this.savedTheme); }
    catch { /* The switch still works when browser storage is disabled. */ }
  }

  private applyTheme(): void {
    this.document.documentElement.dataset['theme'] = this.theme();
    this.document.documentElement.dataset['bsTheme'] = this.theme();
  }

  private readPreference(): Theme | null {
    try {
      const value = this.document.defaultView?.localStorage.getItem(STORAGE_KEY);
      return value === 'dark' || value === 'light' ? value : null;
    } catch { return null; }
  }
}
