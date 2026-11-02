import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {describe, expect, it, vi} from 'vitest';
import {UserManagement} from './user-management';
import {UserService} from '../../services/user.service';
import {AuthService} from '../../services/auth.service';
import {UserDetails} from '../../models/user.model';

describe('UserManagement', () => {
  const user: UserDetails = {id: 2, fullName: 'Teszt', email: 'test@example.hu', phoneNumber: null, role: 'CUSTOMER', active: true};
  async function setup() {
    const page = {content: [user], number: 0, size: 10, totalPages: 2, totalElements: 11};
    const service = {getUsers: vi.fn(() => of(page)), updateUserAccess: vi.fn(() => of(user))};
    await TestBed.configureTestingModule({imports: [UserManagement], providers: [provideRouter([]),
      {provide: UserService, useValue: service},
      {provide: AuthService, useValue: {currentUser: () => ({id: 1, role: 'ADMIN'})}}]
    }).compileComponents();
    const fixture = TestBed.createComponent(UserManagement); fixture.detectChanges();
    return {fixture, component: fixture.componentInstance, service};
  }
  it('loads pages with role and active filters', async () => {
    const {component, service} = await setup();
    expect(service.getUsers).toHaveBeenCalledWith(0, '', '');
    component.role = 'COURIER'; component.active = 'false'; component.load();
    expect(service.getUsers).toHaveBeenLastCalledWith(0, 'COURIER', 'false');
    component.load(1); expect(service.getUsers).toHaveBeenLastCalledWith(1, 'COURIER', 'false');
  });
  it('prevents duplicate saves and updates the visible user after success', async () => {
    const {component, service} = await setup();
    const request = new Subject<UserDetails>(); service.updateUserAccess.mockReturnValue(request);
    const row = component.users()[0]; row.draftRole = 'COURIER';
    component.save(row); component.save(row);
    expect(service.updateUserAccess).toHaveBeenCalledTimes(1);
    service.getUsers.mockReturnValue(of({content: [{...user, role: 'COURIER'}], number: 0, size: 10, totalPages: 1, totalElements: 1}));
    request.next({...user, role: 'COURIER'}); request.complete();
    expect(component.savingId()).toBeNull(); expect(component.users()[0].role).toBe('COURIER');
    expect(component.success()).toContain('sikeresen');
  });
  it('does not submit own account changes', async () => {
    const {component, service} = await setup();
    component.save({...user, id: 1, draftRole: 'CUSTOMER', draftActive: false});
    expect(service.updateUserAccess).not.toHaveBeenCalled();
  });
  it('shows conflict errors and unlocks saving', async () => {
    const {component, service} = await setup();
    service.updateUserAccess.mockReturnValue(throwError(() => ({status: 409})));
    component.save(component.users()[0]);
    expect(component.error()).toContain('nem vonhatod'); expect(component.savingId()).toBeNull();
  });
  it('displays forbidden list errors', async () => {
    const {component, fixture, service} = await setup();
    service.getUsers.mockReturnValue(throwError(() => ({status: 403})));
    component.load(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Nincs jogosultságod');
    expect(component.users()).toEqual([]);
  });
});
