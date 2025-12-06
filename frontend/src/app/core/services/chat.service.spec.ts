import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ChatService } from './chat.service';
import { environment } from '../../../environments/environment';

describe('ChatService', () => {
  let service: ChatService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/conversations`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(ChatService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should_be_created', () => {
    expect(service).toBeTruthy();
  });

  describe('initial state', () => {
    it('should_have_empty_messages_initially', () => {
      expect(service.messages()).toEqual([]);
    });

    it('should_have_no_conversation_id_initially', () => {
      expect(service.conversationId()).toBeNull();
    });

    it('should_not_be_loading_initially', () => {
      expect(service.loading()).toBe(false);
    });

    it('should_have_no_messages_initially', () => {
      expect(service.hasMessages()).toBe(false);
    });

    it('should_report_total_messages_as_zero_initially', () => {
      expect(service.totalMessages()).toBe(0);
    });
  });

  describe('loadMyMessages', () => {
    it('should_load_messages_and_update_state', () => {
      const backendResponse = {
        conversationId: 'conv-123',
        messages: [
          { role: 'USER', content: 'Hello' },
          { role: 'ASSISTANT', content: 'Hi there!' }
        ]
      };

      service.loadMyMessages().subscribe(result => {
        expect(result).toBeTruthy();
        expect(result?.conversationId).toBe('conv-123');
        expect(result?.messages.length).toBe(2);
      });

      const req = httpMock.expectOne(`${apiUrl}/my-messages`);
      expect(req.request.method).toBe('GET');
      req.flush(backendResponse);

      expect(service.conversationId()).toBe('conv-123');
      expect(service.hasMessages()).toBe(true);
      expect(service.totalMessages()).toBe(2);
    });

    it('should_handle_null_response', () => {
      service.loadMyMessages().subscribe(result => {
        expect(result).toBeNull();
      });

      const req = httpMock.expectOne(`${apiUrl}/my-messages`);
      req.flush(null);

      expect(service.conversationId()).toBeNull();
      expect(service.hasMessages()).toBe(false);
    });

    it('should_map_user_role_correctly', () => {
      const backendResponse = {
        conversationId: 'conv-123',
        messages: [
          { role: 'user', content: 'Hello' },
          { role: 'USER', content: 'Hello again' }
        ]
      };

      service.loadMyMessages().subscribe(result => {
        expect(result?.messages[0].role).toBe('user');
        expect(result?.messages[1].role).toBe('user');
      });

      const req = httpMock.expectOne(`${apiUrl}/my-messages`);
      req.flush(backendResponse);
    });

    it('should_map_assistant_role_correctly', () => {
      const backendResponse = {
        conversationId: 'conv-123',
        messages: [
          { role: 'ASSISTANT', content: 'Hi' },
          { role: 'assistant', content: 'Hello' },
          { role: 'MODEL', content: 'Hey' }
        ]
      };

      service.loadMyMessages().subscribe(result => {
        expect(result?.messages.every(m => m.role === 'assistant')).toBe(true);
      });

      const req = httpMock.expectOne(`${apiUrl}/my-messages`);
      req.flush(backendResponse);
    });
  });

  describe('sendMessage', () => {
    it('should_add_user_message_immediately', () => {
      const response = {
        conversationId: 'conv-123',
        role: 'assistant' as const,
        content: 'Response'
      };

      service.sendMessage('Hello').subscribe();

      expect(service.messages().length).toBe(1);
      expect(service.messages()[0].role).toBe('user');
      expect(service.messages()[0].content).toBe('Hello');

      const req = httpMock.expectOne(`${apiUrl}/add-message`);
      req.flush(response);
    });

    it('should_add_assistant_message_after_response', () => {
      const response = {
        conversationId: 'conv-123',
        role: 'assistant' as const,
        content: 'AI Response'
      };

      service.sendMessage('Hello').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/add-message`);
      req.flush(response);

      expect(service.messages().length).toBe(2);
      expect(service.messages()[1].role).toBe('assistant');
      expect(service.messages()[1].content).toBe('AI Response');
    });

    it('should_set_conversation_id_from_response', () => {
      const response = {
        conversationId: 'new-conv-456',
        role: 'assistant' as const,
        content: 'Response'
      };

      service.sendMessage('Hello').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/add-message`);
      req.flush(response);

      expect(service.conversationId()).toBe('new-conv-456');
    });

    it('should_send_correct_request_body', () => {
      const response = {
        conversationId: 'conv-123',
        role: 'assistant' as const,
        content: 'Response'
      };

      service.sendMessage('Test message').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/add-message`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ content: 'Test message' });
      req.flush(response);
    });
  });

  describe('clearConversation', () => {
    it('should_clear_all_messages', () => {
      const backendResponse = {
        conversationId: 'conv-123',
        messages: [{ role: 'USER', content: 'Hello' }]
      };

      service.loadMyMessages().subscribe();
      httpMock.expectOne(`${apiUrl}/my-messages`).flush(backendResponse);

      expect(service.hasMessages()).toBe(true);

      service.clearConversation();

      expect(service.messages()).toEqual([]);
      expect(service.conversationId()).toBeNull();
      expect(service.hasMessages()).toBe(false);
    });
  });

  describe('deleteMyConversations', () => {
    it('should_send_delete_request_and_clear_conversation', () => {
      const backendResponse = {
        conversationId: 'conv-123',
        messages: [{ role: 'USER', content: 'Hello' }]
      };

      service.loadMyMessages().subscribe();
      httpMock.expectOne(`${apiUrl}/my-messages`).flush(backendResponse);

      service.deleteMyConversations().subscribe();

      const req = httpMock.expectOne(`${apiUrl}/my-conversations`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);

      expect(service.messages()).toEqual([]);
      expect(service.conversationId()).toBeNull();
    });
  });

  describe('loadMoreMessages', () => {
    it('should_increase_visible_count_when_more_messages_available', () => {
      const manyMessages = Array.from({ length: 30 }, (_, i) => ({
        role: i % 2 === 0 ? 'USER' : 'ASSISTANT',
        content: `Message ${i}`
      }));

      const backendResponse = {
        conversationId: 'conv-123',
        messages: manyMessages
      };

      service.loadMyMessages().subscribe();
      httpMock.expectOne(`${apiUrl}/my-messages`).flush(backendResponse);

      expect(service.messages().length).toBe(20);
      expect(service.hasMoreMessages()).toBe(true);

      service.loadMoreMessages();

      expect(service.messages().length).toBe(30);
      expect(service.hasMoreMessages()).toBe(false);
    });

    it('should_not_change_when_no_more_messages', () => {
      const fewMessages = Array.from({ length: 5 }, (_, i) => ({
        role: 'USER',
        content: `Message ${i}`
      }));

      const backendResponse = {
        conversationId: 'conv-123',
        messages: fewMessages
      };

      service.loadMyMessages().subscribe();
      httpMock.expectOne(`${apiUrl}/my-messages`).flush(backendResponse);

      expect(service.hasMoreMessages()).toBe(false);

      service.loadMoreMessages();

      expect(service.messages().length).toBe(5);
    });
  });
});

