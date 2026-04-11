import {
  ChangeDetectionStrategy,
  Component,
  computed,
  EventEmitter,
  inject,
  input,
  Output,
  signal,
} from '@angular/core';
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

  @Output() conversationSelected = new EventEmitter<string>();
  @Output() conversationCreated = new EventEmitter<void>();
  @Output() conversationDeleted = new EventEmitter<string>();
  @Output() sidebarClosed = new EventEmitter<void>();

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
      this.conversationList.renameConversation(conv.id, newName).subscribe();
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

