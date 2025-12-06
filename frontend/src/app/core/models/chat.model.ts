export interface MessageRequest {
  content: string;
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

