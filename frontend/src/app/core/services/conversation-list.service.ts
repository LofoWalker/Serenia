import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  ConversationSummary,
  CreateConversationRequest,
  RenameConversationRequest,
} from '../models/chat.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ConversationListService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/conversations`;

  private readonly conversationsSignal = signal<ConversationSummary[]>([]);
  private readonly activeConversationIdSignal = signal<string | null>(null);
  private readonly loadingSignal = signal(false);

  readonly conversations = this.conversationsSignal.asReadonly();
  readonly activeConversationId = this.activeConversationIdSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly hasConversations = computed(() => this.conversationsSignal().length > 0);

  loadConversations(): Observable<ConversationSummary[]> {
    this.loadingSignal.set(true);
    return this.http.get<ConversationSummary[]>(this.apiUrl).pipe(
      tap((conversations) => {
        this.conversationsSignal.set(conversations);
        this.loadingSignal.set(false);
      }),
    );
  }

  createConversation(name?: string): Observable<ConversationSummary> {
    const request: CreateConversationRequest = { name };
    return this.http.post<ConversationSummary>(this.apiUrl, request).pipe(
      tap((conversation) => {
        this.conversationsSignal.update((list) => [conversation, ...list]);
        this.activeConversationIdSignal.set(conversation.id);
      }),
    );
  }

  renameConversation(id: string, name: string): Observable<ConversationSummary> {
    const request: RenameConversationRequest = { name };
    return this.http.put<ConversationSummary>(`${this.apiUrl}/${id}/name`, request).pipe(
      tap((updated) => {
        this.conversationsSignal.update((list) =>
          list.map((c) => (c.id === id ? updated : c)),
        );
      }),
    );
  }

  deleteConversation(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => {
        this.conversationsSignal.update((list) => list.filter((c) => c.id !== id));
        if (this.activeConversationIdSignal() === id) {
          const remaining = this.conversationsSignal();
          this.activeConversationIdSignal.set(remaining.length > 0 ? remaining[0].id : null);
        }
      }),
    );
  }

  setActiveConversation(id: string | null): void {
    this.activeConversationIdSignal.set(id);
  }

  moveConversationToTop(id: string): void {
    this.conversationsSignal.update((list) => {
      const conv = list.find((c) => c.id === id);
      if (!conv) return list;
      return [
        { ...conv, lastActivityAt: new Date().toISOString() },
        ...list.filter((c) => c.id !== id),
      ];
    });
  }

  clearAll(): void {
    this.conversationsSignal.set([]);
    this.activeConversationIdSignal.set(null);
  }
}

