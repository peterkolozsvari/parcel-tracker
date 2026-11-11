import {TestBed} from '@angular/core/testing';
import {ThemeService} from './theme.service';
import {beforeEach, afterEach, describe, expect, it, vi} from 'vitest';

const KEY = 'parcel-tracker.theme';
describe('ThemeService', () => {
  let listener: ((event: MediaQueryListEvent) => void) | undefined;
  const remove = vi.fn();
  function systemDark(matches: boolean): void {
    vi.stubGlobal('matchMedia', vi.fn(() => ({matches,
      addEventListener: vi.fn((_event: string, callback: typeof listener) => { listener = callback; }),
      removeEventListener: remove
    })));
  }
  beforeEach(() => {
    localStorage.removeItem(KEY); listener = undefined; remove.mockClear();
    systemDark(false); TestBed.configureTestingModule({});
  });
  afterEach(() => {
    TestBed.resetTestingModule(); vi.restoreAllMocks(); vi.unstubAllGlobals(); localStorage.removeItem(KEY);
    document.documentElement.dataset['theme'] = 'light'; document.documentElement.dataset['bsTheme'] = 'light';
  });

  it('uses the system theme on first visit without saving an explicit preference', () => {
    systemDark(true);
    expect(TestBed.inject(ThemeService).isDark()).toBe(true);
    expect(document.documentElement.dataset['theme']).toBe('dark');
    expect(document.documentElement.dataset['bsTheme']).toBe('dark');
    expect(localStorage.getItem(KEY)).toBeNull();
  });
  it('restores an explicit light choice even on a dark system', () => {
    systemDark(true); localStorage.setItem(KEY, 'light');
    expect(TestBed.inject(ThemeService).theme()).toBe('light');
  });
  it('restores an explicit dark choice and switches both app and Bootstrap back to light', () => {
    localStorage.setItem(KEY, 'dark'); const service = TestBed.inject(ThemeService);
    expect(service.isDark()).toBe(true); service.toggle();
    expect(service.theme()).toBe('light'); expect(localStorage.getItem(KEY)).toBe('light');
    expect(document.documentElement.dataset['theme']).toBe('light');
    expect(document.documentElement.dataset['bsTheme']).toBe('light');
    service.toggle(); expect(service.isDark()).toBe(true); expect(localStorage.getItem(KEY)).toBe('dark');
  });
  it('follows system changes until the user makes an explicit choice', () => {
    const service = TestBed.inject(ThemeService);
    listener?.({matches: true} as MediaQueryListEvent); expect(service.isDark()).toBe(true);
    service.toggle(); listener?.({matches: true} as MediaQueryListEvent);
    expect(service.isDark()).toBe(false);
  });
  it('ignores invalid preferences and tolerates disabled browser storage', () => {
    localStorage.setItem(KEY, 'invalid'); systemDark(true);
    const service = TestBed.inject(ThemeService); expect(service.isDark()).toBe(true);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Disabled'); });
    expect(() => service.toggle()).not.toThrow(); expect(service.isDark()).toBe(false);
  });
  it('uses system preferences when reading storage is blocked and removes its listener on destroy', () => {
    systemDark(true);
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('Disabled'); });
    expect(TestBed.inject(ThemeService).isDark()).toBe(true);
    TestBed.resetTestingModule(); expect(remove).toHaveBeenCalledWith('change', listener);
  });
});
