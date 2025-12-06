import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';
import {ChatMessage} from '../../../../core/models/chat.model';

@Component({
  selector: 'app-chat-message',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './chat-message.component.html',
  styleUrl: './chat-message.component.css'
})
export class ChatMessageComponent {
  readonly message = input.required<ChatMessage>();
  protected readonly containerClasses = computed(() => {
    const isUser = this.message().role === 'user';
    return `flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`;
  });
  protected readonly bubbleClasses = computed(() => {
    const isUser = this.message().role === 'user';
    const base = 'max-w-[85%] sm:max-w-[70%] px-4 py-3 rounded-2xl';
    if (isUser) {
      return `${base} bg-primary-100 text-primary-900 rounded-br-md`;
    }
    return `${base} bg-primary-800 text-primary-100 rounded-bl-md border border-primary-700`;
  });
  protected readonly ariaLabel = computed(() => {
    const role = this.message().role === 'user' ? 'Vous' : 'Serenia';
    return `${role}: ${this.message().content}`;
  });
}
