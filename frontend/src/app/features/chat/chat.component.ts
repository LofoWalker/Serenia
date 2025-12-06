import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  NgZone,
  OnDestroy,
  signal,
  ViewChild
} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {ChatService} from '../../core/services/chat.service';
import {AuthStateService} from '../../core/services/auth-state.service';
import {ChatMessageComponent} from './components/chat-message/chat-message.component';
import {ChatInputComponent} from './components/chat-input/chat-input.component';
import {AlertComponent} from '../../shared/ui/alert/alert.component';

@Component({
  selector: 'app-chat',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChatMessageComponent, ChatInputComponent, AlertComponent],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css'
})
export class ChatComponent implements AfterViewInit, OnDestroy {
  protected readonly chatService = inject(ChatService);
  protected readonly authState = inject(AuthStateService);
  private readonly ngZone = inject(NgZone);
  protected readonly errorMessage = signal('');
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef<HTMLDivElement>;
  @ViewChild('chatInput') private chatInput!: ChatInputComponent;
  private resizeObserver: ResizeObserver | null = null;
  private isUserScrolling = false;
  private scrollTimeout: ReturnType<typeof setTimeout> | null = null;
  ngAfterViewInit(): void {
    this.scrollToBottom();
    this.setupResizeObserver();
  }
  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    if (this.scrollTimeout) {
      clearTimeout(this.scrollTimeout);
    }
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
    if (element.scrollTop < 100 && this.chatService.hasMoreMessages() && !this.chatService.loadingMore()) {
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
    this.chatInput.setDisabled(true);
    this.isUserScrolling = false;
    this.chatService.sendMessage(content).subscribe({
      next: () => {
        this.chatInput.setDisabled(false);
        this.scrollToBottom();
      },
      error: (error: HttpErrorResponse) => {
        this.chatInput.setDisabled(false);
        if (error.status === 401) {
          this.errorMessage.set('Session expirée. Veuillez vous reconnecter.');
        } else {
          this.errorMessage.set("Impossible d'envoyer le message. Veuillez réessayer.");
        }
      }
    });
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
