import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ChatMessage } from '../../../../core/models/chat.model';

@Component({
  selector: 'app-chat-message',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  templateUrl: './chat-message.component.html',
  styleUrl: './chat-message.component.css',
})
export class ChatMessageComponent {
  readonly message = input.required<ChatMessage>();

  protected readonly isUser = computed(() => this.message().role === 'user');

  protected readonly containerClasses = computed(() => {
    return `flex ${this.isUser() ? 'justify-end' : 'justify-start'} mb-4`;
  });

  protected readonly bubbleClasses = computed(() => {
    const base = 'max-w-[90%] px-4 py-3 rounded-2xl';
    if (this.isUser()) {
      return `${base} bg-primary-100 text-primary-900 rounded-br-md`;
    }
    return `${base} bg-primary-800 text-primary-100 rounded-bl-md border border-primary-700`;
  });

  protected readonly ariaLabel = computed(() => {
    const role = this.isUser() ? 'Vous' : 'Serenia';
    return `${role}: ${this.message().content}`;
  });
}
