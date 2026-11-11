import {DOCUMENT} from '@angular/common';
import {inject, Injectable, signal} from '@angular/core';
import {EN_TRANSLATIONS} from '../i18n/translations';

export type Language = 'hu' | 'en';
const STORAGE_KEY = 'parcel-tracker.language';

@Injectable({providedIn: 'root'})
export class LanguageService {
  private readonly document = inject(DOCUMENT);
  private readonly selectedLanguage = signal<Language>(this.restoreLanguage());
  readonly language = this.selectedLanguage.asReadonly();

  constructor() { this.document.documentElement.lang = this.language(); }

  changeLanguage(language: Language): void {
    this.selectedLanguage.set(language);
    this.document.documentElement.lang = language;
    try { this.document.defaultView?.localStorage.setItem(STORAGE_KEY, language); } catch { /* Storage may be disabled. */ }
  }

  translate(text: string | null | undefined): string {
    if (!text) return '';
    return this.language() === 'en' ? EN_TRANSLATIONS[text.replace(/\s+/g, ' ').trim()] ?? text : text;
  }

  private restoreLanguage(): Language {
    try { return this.document.defaultView?.localStorage.getItem(STORAGE_KEY) === 'en' ? 'en' : 'hu'; }
    catch { return 'hu'; }
  }
}
