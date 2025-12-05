import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, of, map } from 'rxjs';
import { MessageRequest, MessageResponse, ChatMessage, ConversationMessagesResponse } from '../models/chat.model';
import { environment } from '../../../environments/environment';

interface BackendChatMessage {
  role: string;
  content: string;
  timestamp?: string;
}

interface BackendConversationResponse {
  conversationId: string;
  messages: BackendChatMessage[];
}

const MESSAGES_PER_PAGE = 20;

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/conversations`;

  private readonly allMessagesSignal = signal<ChatMessage[]>([]);
  private readonly visibleCountSignal = signal(MESSAGES_PER_PAGE);
  private readonly conversationIdSignal = signal<string | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly loadingMoreSignal = signal(false);

  readonly conversationId = this.conversationIdSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly loadingMore = this.loadingMoreSignal.asReadonly();

  readonly totalMessages = computed(() => this.allMessagesSignal().length);
  readonly hasMessages = computed(() => this.allMessagesSignal().length > 0);
  readonly hasMoreMessages = computed(() => this.visibleCountSignal() < this.allMessagesSignal().length);

  readonly messages = computed(() => {
    const all = this.allMessagesSignal();
    const visibleCount = this.visibleCountSignal();
    const startIndex = Math.max(0, all.length - visibleCount);
    return all.slice(startIndex);
  });

  private mapRole(role: string): 'user' | 'assistant' {
    const normalizedRole = role.toLowerCase();
    return normalizedRole === 'user' ? 'user' : 'assistant';
  }

  private mapMessages(messages: BackendChatMessage[]): ChatMessage[] {
    return messages.map(msg => ({
      role: this.mapRole(msg.role),
      content: msg.content,
      timestamp: msg.timestamp
    }));
  }

  loadMoreMessages(): void {
    if (!this.hasMoreMessages()) return;

    this.loadingMoreSignal.set(true);
    const newCount = Math.min(
      this.visibleCountSignal() + MESSAGES_PER_PAGE,
      this.allMessagesSignal().length
    );
    this.visibleCountSignal.set(newCount);
    this.loadingMoreSignal.set(false);
  }

  loadMyMessages(): Observable<ConversationMessagesResponse | null> {
    this.loadingSignal.set(true);
    return this.http.get<BackendConversationResponse | null>(`${this.apiUrl}/my-messages`).pipe(
      map(response => {
        if (!response) return null;
        return {
          conversationId: response.conversationId,
          messages: this.mapMessages(response.messages)
        };
      }),
      tap(response => {
        if (response) {
          this.conversationIdSignal.set(response.conversationId);
          this.allMessagesSignal.set(response.messages);
          this.visibleCountSignal.set(MESSAGES_PER_PAGE);
        }
        this.loadingSignal.set(false);
      })
    );
  }

  sendMessage(content: string): Observable<MessageResponse> {
    this.loadingSignal.set(true);

    const userMessage: ChatMessage = { role: 'user', content };
    this.allMessagesSignal.update(messages => [...messages, userMessage]);
    this.visibleCountSignal.update(count => count + 1);

    const request: MessageRequest = { content };
    return this.http.post<MessageResponse>(`${this.apiUrl}/add-message`, request).pipe(
      tap(response => {
        this.conversationIdSignal.set(response.conversationId);
        const assistantMessage: ChatMessage = {
          role: 'assistant',
          content: response.content
        };
        this.allMessagesSignal.update(messages => [...messages, assistantMessage]);
        this.visibleCountSignal.update(count => count + 1);
        this.loadingSignal.set(false);
      })
    );
  }

  loadHistory(conversationId: string): Observable<ChatMessage[]> {
    this.loadingSignal.set(true);
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/${conversationId}/messages`).pipe(
      tap(messages => {
        this.allMessagesSignal.set(messages);
        this.visibleCountSignal.set(MESSAGES_PER_PAGE);
        this.conversationIdSignal.set(conversationId);
        this.loadingSignal.set(false);
      })
    );
  }

  clearConversation(): void {
    this.allMessagesSignal.set([]);
    this.visibleCountSignal.set(MESSAGES_PER_PAGE);
    this.conversationIdSignal.set(null);
  }

  deleteMyConversations(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/my-conversations`).pipe(
      tap(() => {
        this.clearConversation();
      })
    );
  }
}

