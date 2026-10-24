import {Injectable, signal} from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LanguageService {

  language = signal<'hu' | 'en'>('hu');

  changeLanguage(language: 'hu' | 'en') {
    this.language.set(language);
  }

}
