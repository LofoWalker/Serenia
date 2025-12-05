import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { MessageRequest, MessageResponse, ChatMessage } from '../models/chat.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/conversations`;

  private readonly messagesSignal = signal<ChatMessage[]>([]);
  private readonly conversationIdSignal = signal<string | null>(null);
  private readonly loadingSignal = signal(false);

  readonly messages = this.messagesSignal.asReadonly();
  readonly conversationId = this.conversationIdSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly hasMessages = computed(() => this.messagesSignal().length > 0);

  sendMessage(content: string): Observable<MessageResponse> {
    this.loadingSignal.set(true);

    const userMessage: ChatMessage = { role: 'user', content };
    this.messagesSignal.update(messages => [...messages, userMessage]);

    const request: MessageRequest = { content };
    return this.http.post<MessageResponse>(`${this.apiUrl}/add-message`, request).pipe(
      tap(response => {
        this.conversationIdSignal.set(response.conversationId);
        const assistantMessage: ChatMessage = {
          role: 'assistant',
          content: response.content
        };
        this.messagesSignal.update(messages => [...messages, assistantMessage]);
        this.loadingSignal.set(false);
      })
    );
  }

  loadHistory(conversationId: string): Observable<ChatMessage[]> {
    this.loadingSignal.set(true);
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/${conversationId}/messages`).pipe(
      tap(messages => {
        this.messagesSignal.set(messages);
        this.conversationIdSignal.set(conversationId);
        this.loadingSignal.set(false);
      })
    );
  }

  clearConversation(): void {
    this.messagesSignal.set([]);
    this.conversationIdSignal.set(null);
  }
}

