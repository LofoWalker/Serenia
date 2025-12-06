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
  setDisabled(value: boolean): void {
    this.disabled.set(value);
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
    setTimeout(() => this.adjustHeight(), 0);
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
