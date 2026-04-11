import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { catchError, EMPTY, take } from 'rxjs';
import { ConversationListService } from '../../../../core/services/conversation-list.service';
import { ConversationSummary } from '../../../../core/models/chat.model';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-conversation-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  templateUrl: './conversation-sidebar.component.html',
})
export class ConversationSidebarComponent {
  protected readonly conversationList = inject(ConversationListService);

  readonly mobileOpen = input(false);

  readonly conversationSelected = output<string>();
  readonly conversationCreated = output<void>();
  readonly conversationDeleted = output<string>();
  readonly sidebarClosed = output<void>();

  protected readonly editingId = signal<string | null>(null);
  protected readonly editingName = signal('');
  protected readonly collapsed = signal(false);

  protected readonly sortedConversations = computed(() =>
    [...this.conversationList.conversations()].sort(
      (a, b) => new Date(b.lastActivityAt).getTime() - new Date(a.lastActivityAt).getTime(),
    ),
  );

  protected isActive(conv: ConversationSummary): boolean {
    return this.conversationList.activeConversationId() === conv.id;
  }

  protected selectConversation(conv: ConversationSummary): void {
    if (this.editingId() !== null) return;
    this.conversationList.setActiveConversation(conv.id);
    this.conversationSelected.emit(conv.id);
    this.sidebarClosed.emit();
  }

  protected onCreateConversation(): void {
    this.conversationCreated.emit();
    this.sidebarClosed.emit();
  }

  protected closeSidebar(): void {
    this.sidebarClosed.emit();
  }

  protected startRename(conv: ConversationSummary, event: Event): void {
    event.stopPropagation();
    this.editingId.set(conv.id);
    this.editingName.set(conv.name);
  }

  protected confirmRename(conv: ConversationSummary): void {
    const newName = this.editingName().trim();
    if (newName && newName !== conv.name) {
      this.conversationList
        .renameConversation(conv.id, newName)
        .pipe(
          take(1),
          catchError(() => {
            this.editingName.set(conv.name);
            return EMPTY;
          }),
        )
        .subscribe();
    }
    this.editingId.set(null);
  }

  protected cancelRename(): void {
    this.editingId.set(null);
  }

  protected onDelete(conv: ConversationSummary, event: Event): void {
    event.stopPropagation();
    this.conversationDeleted.emit(conv.id);
  }

  protected toggleCollapsed(): void {
    this.collapsed.update((v) => !v);
  }
}

