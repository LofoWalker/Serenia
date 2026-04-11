import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  NgZone,
  OnDestroy,
  OnInit,
  signal,
  ViewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, EMPTY, take, tap } from 'rxjs';
import { ChatService } from '../../core/services/chat.service';
import { ConversationListService } from '../../core/services/conversation-list.service';
import { ChatMessage } from '../../core/models/chat.model';
import { AuthStateService } from '../../core/services/auth-state.service';
import { SubscriptionService } from '../../core/services/subscription.service';
import { QuotaErrorDTO } from '../../core/models/subscription.model';
import { ChatMessageComponent } from './components/chat-message/chat-message.component';
import { ChatInputComponent } from './components/chat-input/chat-input.component';
import { ConversationSidebarComponent } from './components/conversation-sidebar/conversation-sidebar.component';
import { AlertComponent } from '../../shared/ui/alert/alert.component';
import { ButtonComponent } from '../../shared/ui/button/button.component';

@Component({
  selector: 'app-chat',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ChatMessageComponent,
    ChatInputComponent,
    ConversationSidebarComponent,
    AlertComponent,
    ButtonComponent,
    RouterLink,
  ],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css',
})
export class ChatComponent implements OnInit, AfterViewInit, OnDestroy {
  protected readonly chatService = inject(ChatService);
  protected readonly conversationList = inject(ConversationListService);
  protected readonly authState = inject(AuthStateService);
  protected readonly subscriptionService = inject(SubscriptionService);
  private readonly ngZone = inject(NgZone);
  private readonly router = inject(Router);

  protected readonly errorMessage = signal('');
  protected readonly quotaError = signal<QuotaErrorDTO | null>(null);
  protected readonly mobileSidebarOpen = signal(false);

  protected readonly welcomeMessage = computed<ChatMessage>(() => {
    const firstName = this.authState.user()?.firstName ?? '';
    return {
      role: 'assistant',
      content: `Coucou ${firstName} ! C'est Serenia ✨ Ravi de te rencontrer. T'as passé une bonne journée ?`,
    };
  });

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef<HTMLDivElement>;
  @ViewChild('chatInput') private chatInput!: ChatInputComponent;
  private resizeObserver: ResizeObserver | null = null;
  private mutationObserver: MutationObserver | null = null;
  private isUserScrolling = false;
  private scrollTimeout: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.conversationList
      .loadConversations()
      .pipe(
        take(1),
        tap((conversations) => {
          if (conversations.length > 0) {
            const mostRecent = conversations[0];
            this.conversationList.setActiveConversation(mostRecent.id);
            this.loadConversationMessages(mostRecent.id);
          }
        }),
        catchError(() => {
          this.loadLegacyMessages();
          return EMPTY;
        }),
      )
      .subscribe();

    if (!this.subscriptionService.status()) {
      this.subscriptionService
        .getStatus()
        .pipe(
          take(1),
          catchError(() => EMPTY),
        )
        .subscribe();
    }
  }

  ngAfterViewInit(): void {
    this.scrollToBottom();
    this.setupResizeObserver();
    this.setupMutationObserver();
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.mutationObserver?.disconnect();
    if (this.scrollTimeout) {
      clearTimeout(this.scrollTimeout);
    }
  }

  protected onConversationSelected(conversationId: string): void {
    this.errorMessage.set('');
    this.quotaError.set(null);
    this.mobileSidebarOpen.set(false);
    this.loadConversationMessages(conversationId);
  }

  protected onConversationCreated(): void {
    this.mobileSidebarOpen.set(false);
    this.conversationList
      .createConversation()
      .pipe(
        take(1),
        tap((conv) => {
          this.loadConversationMessages(conv.id);
        }),
        catchError(() => {
          this.errorMessage.set('Impossible de créer la conversation.');
          return EMPTY;
        }),
      )
      .subscribe();
  }

  protected onConversationDeleted(conversationId: string): void {
    const wasActive = this.conversationList.activeConversationId() === conversationId;
    this.conversationList
      .deleteConversation(conversationId)
      .pipe(
        take(1),
        tap(() => {
          if (wasActive) {
            const activeId = this.conversationList.activeConversationId();
            if (activeId) {
              this.loadConversationMessages(activeId);
            } else {
              this.chatService.clearConversation();
            }
          }
        }),
        catchError(() => {
          this.errorMessage.set('Impossible de supprimer la conversation.');
          return EMPTY;
        }),
      )
      .subscribe();
  }

  private loadConversationMessages(conversationId: string): void {
    this.chatService
      .switchConversation(conversationId)
      .pipe(
        take(1),
        tap(() => {
          setTimeout(() => this.scrollToBottom(), 100);
        }),
        catchError(() => {
          this.errorMessage.set('Impossible de charger les messages.');
          return EMPTY;
        }),
      )
      .subscribe();
  }

  private loadLegacyMessages(): void {
    this.chatService
      .loadMyMessages()
      .pipe(
        take(1),
        tap(() => {
          setTimeout(() => this.scrollToBottom(), 100);
        }),
        catchError(() => {
          this.errorMessage.set('Impossible de charger vos messages.');
          return EMPTY;
        }),
      )
      .subscribe();
  }

  private setupResizeObserver(): void {
    if (!this.messagesContainer?.nativeElement) return;
    this.resizeObserver = new ResizeObserver(() => {
      this.ngZone.run(() => {
        if (!this.isUserScrolling) {
          this.scrollToBottom();
        }
      });
    });
    this.resizeObserver.observe(this.messagesContainer.nativeElement);
  }

  private setupMutationObserver(): void {
    if (!this.messagesContainer?.nativeElement) return;
    this.mutationObserver = new MutationObserver(() => {
      this.ngZone.run(() => {
        if (!this.isUserScrolling) {
          this.scrollToBottom();
        }
      });
    });
    this.mutationObserver.observe(this.messagesContainer.nativeElement, {
      childList: true,
      subtree: true,
      characterData: true,
    });
  }

  protected onScroll(): void {
    const element = this.messagesContainer?.nativeElement;
    if (!element) return;
    const isAtBottom = element.scrollHeight - element.scrollTop - element.clientHeight < 50;
    this.isUserScrolling = !isAtBottom;
    if (this.scrollTimeout) {
      clearTimeout(this.scrollTimeout);
    }
    this.scrollTimeout = setTimeout(() => {
      if (isAtBottom) {
        this.isUserScrolling = false;
      }
    }, 150);
    if (
      element.scrollTop < 100 &&
      this.chatService.hasMoreMessages() &&
      !this.chatService.loadingMore()
    ) {
      this.loadMore();
    }
  }

  protected loadMore(): void {
    const element = this.messagesContainer?.nativeElement;
    const previousScrollHeight = element?.scrollHeight || 0;
    this.chatService.loadMoreMessages();
    requestAnimationFrame(() => {
      if (element) {
        const newScrollHeight = element.scrollHeight;
        element.scrollTop = newScrollHeight - previousScrollHeight;
      }
    });
  }

  protected onSendMessage(content: string): void {
    this.errorMessage.set('');
    this.quotaError.set(null);
    this.chatInput.setDisabled(true);
    this.isUserScrolling = false;
    const activeConvId = this.conversationList.activeConversationId() ?? undefined;
    this.chatService
      .sendMessage(content, activeConvId)
      .pipe(
        take(1),
        tap((response) => {
          this.chatInput.setDisabled(false);
          this.scrollToBottom();
          this.subscriptionService.refreshStatus();
          if (activeConvId) {
            this.conversationList.moveConversationToTop(activeConvId);
          }
          if (!activeConvId && response.conversationId) {
            this.conversationList.setActiveConversation(response.conversationId);
            this.conversationList.loadConversations().pipe(take(1)).subscribe();
          }
        }),
        catchError((error: HttpErrorResponse) => {
          this.chatInput.setDisabled(false);
          if (error.status === 401) {
            this.errorMessage.set('Session expirée. Veuillez vous reconnecter.');
          } else if (error.status === 429) {
            const quotaErr = error.error as QuotaErrorDTO;
            this.quotaError.set(quotaErr);
            this.errorMessage.set(this.getQuotaErrorMessage(quotaErr));
          } else {
            this.errorMessage.set("Impossible d'envoyer le message. Veuillez réessayer.");
          }
          return EMPTY;
        }),
      )
      .subscribe();
  }

  private getQuotaErrorMessage(error: QuotaErrorDTO): string {
    switch (error.quotaType) {
      case 'DAILY_MESSAGE_LIMIT':
        return `Limite quotidienne atteinte (${error.current}/${error.limit} messages). Réessayez demain ou passez à un plan supérieur.`;
      case 'MONTHLY_TOKEN_LIMIT':
        return `Limite mensuelle de tokens atteinte. Passez à un plan supérieur pour continuer.`;
      default:
        return error.message || 'Quota dépassé.';
    }
  }

  protected goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  protected openMobileSidebar(): void {
    this.mobileSidebarOpen.set(true);
  }

  protected closeMobileSidebar(): void {
    this.mobileSidebarOpen.set(false);
  }

  private scrollToBottom(): void {
    requestAnimationFrame(() => {
      if (this.messagesContainer?.nativeElement) {
        const element = this.messagesContainer.nativeElement;
        element.scrollTop = element.scrollHeight;
      }
    });
  }
}
