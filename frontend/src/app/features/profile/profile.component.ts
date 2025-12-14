import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {catchError, EMPTY, take, tap} from 'rxjs';
import {AuthService} from '../../core/services/auth.service';
import {AuthStateService} from '../../core/services/auth-state.service';
import {ChatService} from '../../core/services/chat.service';
import {ButtonComponent} from '../../shared/ui/button/button.component';
import {AlertComponent} from '../../shared/ui/alert/alert.component';

@Component({
  selector: 'app-profile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent, AlertComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly chatService = inject(ChatService);
  private readonly router = inject(Router);
  protected readonly authState = inject(AuthStateService);
  protected readonly showDeleteConfirm = signal(false);
  protected readonly showDeleteConversationsConfirm = signal(false);
  protected readonly deleting = signal(false);
  protected readonly deletingConversations = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  ngOnInit(): void {
    if (!this.authState.user()) {
      this.authService.getProfile().pipe(
        take(1),
        catchError(() => {
          this.errorMessage.set('Impossible de charger le profil.');
          return EMPTY;
        })
      ).subscribe();
    }
  }
  protected getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }
  protected deleteConversations(): void {
    this.deletingConversations.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.chatService.deleteMyConversations().pipe(
      take(1),
      tap(() => {
        this.deletingConversations.set(false);
        this.showDeleteConversationsConfirm.set(false);
        this.successMessage.set('Vos conversations ont été supprimées avec succès.');
      }),
      catchError(() => {
        this.deletingConversations.set(false);
        this.errorMessage.set('Impossible de supprimer les conversations. Veuillez réessayer.');
        return EMPTY;
      })
    ).subscribe();
  }
  protected deleteAccount(): void {
    this.deleting.set(true);
    this.errorMessage.set('');
    this.authService.deleteAccount().pipe(
      take(1),
      tap(() => this.router.navigate(['/'])),
      catchError(() => {
        this.deleting.set(false);
        this.errorMessage.set('Impossible de supprimer le compte. Veuillez réessayer.');
        return EMPTY;
      })
    ).subscribe();
  }
}
