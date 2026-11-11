import {TestBed} from '@angular/core/testing';
import {LanguageService} from './language.service';
import {beforeEach, afterEach, describe, expect, it, vi} from 'vitest';

const KEY = 'parcel-tracker.language';
describe('LanguageService', () => {
  beforeEach(() => { localStorage.removeItem(KEY); TestBed.configureTestingModule({}); });
  afterEach(() => { vi.restoreAllMocks(); localStorage.removeItem(KEY); document.documentElement.lang = 'hu'; });

  it('defaults to Hungarian and restores an English preference', () => {
    expect(TestBed.inject(LanguageService).language()).toBe('hu');
    localStorage.setItem(KEY, 'en');
    const restored = TestBed.runInInjectionContext(() => new LanguageService());
    expect(restored.language()).toBe('en'); expect(document.documentElement.lang).toBe('en');
  });
  it('switches translations, document language and persisted preference in both directions', () => {
    const service = TestBed.inject(LanguageService);
    service.changeLanguage('en');
    expect(service.translate('Küldemény feladása')).toBe('Create shipment');
    expect(localStorage.getItem(KEY)).toBe('en'); expect(document.documentElement.lang).toBe('en');
    service.changeLanguage('hu');
    expect(service.translate('Küldemény feladása')).toBe('Küldemény feladása');
    expect(localStorage.getItem(KEY)).toBe('hu'); expect(document.documentElement.lang).toBe('hu');
  });
  it('keeps unknown user text and tolerates unavailable storage', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Disabled'); });
    const service = TestBed.inject(LanguageService);
    expect(() => service.changeLanguage('en')).not.toThrow();
    expect(service.translate('Custom delivery instructions')).toBe('Custom delivery instructions');
    expect(service.translate(null)).toBe('');
  });
  it('falls back to Hungarian for invalid or inaccessible saved preferences', () => {
    localStorage.setItem(KEY, 'invalid');
    expect(TestBed.inject(LanguageService).language()).toBe('hu');
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('Disabled'); });
    expect(TestBed.runInInjectionContext(() => new LanguageService()).language()).toBe('hu');
  });
});
