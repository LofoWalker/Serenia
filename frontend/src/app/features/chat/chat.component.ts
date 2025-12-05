import { Component, ChangeDetectionStrategy, inject, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ChatService } from '../../core/services/chat.service';
import { AuthStateService } from '../../core/services/auth-state.service';
import { ChatMessageComponent } from './components/chat-message/chat-message.component';
import { ChatInputComponent } from './components/chat-input/chat-input.component';
import { AlertComponent } from '../../shared/ui/alert/alert.component';
@Component({
  selector: 'app-chat',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChatMessageComponent, ChatInputComponent, AlertComponent],
  template: `
    <div class="h-[calc(100vh-4rem)] flex flex-col bg-primary-950">
      <div 
        #messagesContainer
        class="flex-1 overflow-y-auto px-4 py-6 scrollbar-thin"
      >
        <div class="max-w-3xl mx-auto">
          @if (!chatService.hasMessages()) {
            <div class="flex flex-col items-center justify-center h-full min-h-[50vh] text-center">
              <div class="w-16 h-16 mb-6 rounded-full bg-primary-800 flex items-center justify-center">
                <svg class="w-8 h-8 text-primary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <h2 class="text-xl font-semibold text-primary-100 mb-2">
                Bienvenue, {{ authState.userFullName() }}
              </h2>
              <p class="text-primary-400 max-w-md">
                Commencez une conversation avec Serenia. Posez vos questions ou partagez vos pensées.
              </p>
            </div>
          } @else {
            <div role="log" aria-live="polite" aria-label="Messages de la conversation">
              @for (message of chatService.messages(); track $index) {
                <app-chat-message [message]="message" />
              }
              @if (chatService.loading()) {
                <div class="flex justify-start mb-4">
                  <div class="bg-primary-800 border border-primary-700 px-4 py-3 rounded-2xl rounded-bl-md">
                    <div class="flex items-center gap-1" aria-label="Serenia est en train de répondre">
                      <span class="w-2 h-2 bg-primary-400 rounded-full animate-bounce" style="animation-delay: 0ms"></span>
                      <span class="w-2 h-2 bg-primary-400 rounded-full animate-bounce" style="animation-delay: 150ms"></span>
                      <span class="w-2 h-2 bg-primary-400 rounded-full animate-bounce" style="animation-delay: 300ms"></span>
                    </div>
                  </div>
                </div>
              }
            </div>
          }
          @if (errorMessage()) {
            <div class="mt-4">
              <app-alert type="error" [message]="errorMessage()" />
            </div>
          }
        </div>
      </div>
      <div class="border-t border-primary-800 bg-primary-900/50 backdrop-blur-sm p-4">
        <div class="max-w-3xl mx-auto">
          <app-chat-input 
            #chatInput
            (messageSent)="onSendMessage($event)"
          />
        </div>
      </div>
    </div>
  `
})
export class ChatComponent implements AfterViewChecked {
  protected readonly chatService = inject(ChatService);
  protected readonly authState = inject(AuthStateService);
  protected readonly errorMessage = signal('');
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef<HTMLDivElement>;
  @ViewChild('chatInput') private chatInput!: ChatInputComponent;
  private shouldScrollToBottom = false;
  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }
  protected onSendMessage(content: string): void {
    this.errorMessage.set('');
    this.chatInput.setDisabled(true);
    this.shouldScrollToBottom = true;
    this.chatService.sendMessage(content).subscribe({
      next: () => {
        this.chatInput.setDisabled(false);
        this.shouldScrollToBottom = true;
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
    if (this.messagesContainer?.nativeElement) {
      const element = this.messagesContainer.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
  }
}
