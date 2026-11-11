import {TranslatePipe} from '../../i18n/translate.pipe';
import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';
import {UserService} from '../../services/user.service';
import {AuthService} from '../../services/auth.service';
import {Role, UserDetails} from '../../models/user.model';
import {PageResponse} from '../../models/page-response.model';

type EditableUser = UserDetails & {draftRole: Role; draftActive: boolean};

@Component({
  selector: 'app-user-management',
  imports: [TranslatePipe, FormsModule],
  templateUrl: './user-management.html',
  styleUrl: './user-management.css'
})
export class UserManagement implements OnInit {
  private readonly service = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly roleLabels: Record<Role, string> = {CUSTOMER: 'Ügyfél', COURIER: 'Futár', ADMIN: 'Adminisztrátor'};
  readonly roles: Role[] = ['CUSTOMER', 'COURIER', 'ADMIN'];
  readonly page = signal<PageResponse<UserDetails> | null>(null);
  readonly users = signal<EditableUser[]>([]);
  readonly loading = signal(false);
  readonly savingId = signal<number | null>(null);
  readonly error = signal('');
  readonly success = signal('');
  role: Role | '' = '';
  active = '';

  ngOnInit(): void { this.load(); }
  isSelf(user: UserDetails): boolean { return user.id === this.auth.currentUser()?.id; }

  load(index = 0): void {
    if (this.loading() || this.savingId() !== null || index < 0) return;
    this.loading.set(true);
    this.error.set('');
    this.service.getUsers(index, this.role, this.active).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))
    ).subscribe({
      next: result => {
        this.page.set(result);
        this.users.set(result.content.map(user => ({...user, draftRole: user.role, draftActive: user.active})));
      },
      error: error => { this.page.set(null); this.users.set([]); this.handleError(error.status); }
    });
  }

  save(user: EditableUser): void {
    if (this.savingId() !== null || this.loading() || this.isSelf(user)) return;
    this.savingId.set(user.id);
    this.error.set('');
    this.success.set('');
    this.service.updateUserAccess(user.id, user.draftRole, user.draftActive).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.savingId.set(null))
    ).subscribe({
      next: result => {
        this.users.update(users => users.map(item => item.id === result.id
          ? {...result, draftRole: result.role, draftActive: result.active} : item));
        this.success.set('A felhasználó hozzáférését sikeresen módosítottuk.');
        this.savingId.set(null);
        this.load(this.page()?.number ?? 0);
      },
      error: error => this.handleError(error.status)
    });
  }

  private handleError(status: number): void {
    if (status === 401) this.router.navigate(['/login']);
    this.error.set(status === 403 ? 'Nincs jogosultságod a felhasználókezeléshez.'
      : status === 409 ? 'A saját adminisztrátori hozzáférésed nem vonhatod vissza.'
      : status === 404 ? 'A felhasználó már nem található. Frissítsd a listát.'
      : 'A művelet nem sikerült. Próbáld újra.');
  }
}
