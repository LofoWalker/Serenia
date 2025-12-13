import {ChangeDetectionStrategy, Component, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-chat-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  templateUrl: './chat-input.component.html',
  styleUrl: './chat-input.component.css'
})
export class ChatInputComponent {
  readonly disabled = signal(false);
  readonly messageSent = output<string>();
  protected messageText = '';
  protected readonly textareaHeight = signal('48px');
  protected readonly glowState = signal<'active' | 'fading' | 'none'>('none');
  private typingTimeout: ReturnType<typeof setTimeout> | null = null;

  setDisabled(value: boolean): void {
    this.disabled.set(value);
  }

  protected onInput(): void {
    this.glowState.set('active');
    if (this.typingTimeout) {
      clearTimeout(this.typingTimeout);
    }
    this.typingTimeout = setTimeout(() => {
      this.glowState.set('fading');
      setTimeout(() => {
        if (this.glowState() === 'fading') {
          this.glowState.set('none');
        }
      }, 600);
    }, 100);
  }

  protected onSubmit(event: Event): void {
    event.preventDefault();
    this.sendMessage();
  }
  protected onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private sendMessage(): void {
    const trimmed = this.messageText.trim();
    if (!trimmed || this.disabled()) return;
    this.messageSent.emit(trimmed);
    this.messageText = '';
    this.textareaHeight.set('48px');
  }
  private adjustHeight(): void {
    const lines = this.messageText.split('\n').length;
    const height = Math.min(Math.max(48, lines * 24 + 24), 200);
    this.textareaHeight.set(`${height}px`);
  }
}
