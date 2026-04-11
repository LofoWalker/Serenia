export interface MessageRequest {
  content: string;
  conversationId?: string;
}

export interface MessageResponse {
  conversationId: string;
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp?: string;
}

export interface ConversationMessagesResponse {
  conversationId: string;
  messages: ChatMessage[];
}

export interface ConversationSummary {
  id: string;
  name: string;
  lastActivityAt: string;
}

export interface CreateConversationRequest {
  name?: string;
}

export interface RenameConversationRequest {
  name: string;
}

