import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ConversationListService } from './conversation-list.service';
import { environment } from '../../../environments/environment';
import { ConversationSummary } from '../models/chat.model';

describe('ConversationListService', () => {
  let service: ConversationListService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/conversations`;

  const mockConversations: ConversationSummary[] = [
    { id: 'conv-1', name: 'First', lastActivityAt: '2026-04-11T10:00:00Z' },
    { id: 'conv-2', name: 'Second', lastActivityAt: '2026-04-11T11:00:00Z' },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ConversationListService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should_be_created', () => {
    expect(service).toBeTruthy();
  });

  describe('initial state', () => {
    it('should_have_empty_conversations_initially', () => {
      expect(service.conversations()).toEqual([]);
    });

    it('should_have_no_active_conversation_initially', () => {
      expect(service.activeConversationId()).toBeNull();
    });

    it('should_not_have_conversations_initially', () => {
      expect(service.hasConversations()).toBe(false);
    });
  });

  describe('loadConversations', () => {
    it('should_load_and_store_conversations', () => {
      service.loadConversations().subscribe((result) => {
        expect(result.length).toBe(2);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockConversations);

      expect(service.conversations().length).toBe(2);
      expect(service.hasConversations()).toBe(true);
    });

    it('should_handle_empty_list', () => {
      service.loadConversations().subscribe();

      httpMock.expectOne(apiUrl).flush([]);

      expect(service.conversations()).toEqual([]);
      expect(service.hasConversations()).toBe(false);
    });
  });

  describe('createConversation', () => {
    it('should_create_and_add_to_list', () => {
      const newConv: ConversationSummary = {
        id: 'conv-new',
        name: 'New Conversation',
        lastActivityAt: '2026-04-11T12:00:00Z',
      };

      service.createConversation('New Conversation').subscribe();

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ name: 'New Conversation' });
      req.flush(newConv);

      expect(service.conversations().length).toBe(1);
      expect(service.conversations()[0].id).toBe('conv-new');
      expect(service.activeConversationId()).toBe('conv-new');
    });
  });

  describe('renameConversation', () => {
    it('should_rename_and_update_in_list', () => {
      service.loadConversations().subscribe();
      httpMock.expectOne(apiUrl).flush(mockConversations);

      const updated: ConversationSummary = {
        ...mockConversations[0],
        name: 'Renamed',
      };

      service.renameConversation('conv-1', 'Renamed').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/conv-1/name`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ name: 'Renamed' });
      req.flush(updated);

      expect(service.conversations().find((c) => c.id === 'conv-1')?.name).toBe('Renamed');
    });
  });

  describe('deleteConversation', () => {
    it('should_delete_and_remove_from_list', () => {
      service.loadConversations().subscribe();
      httpMock.expectOne(apiUrl).flush(mockConversations);

      service.deleteConversation('conv-1').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/conv-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);

      expect(service.conversations().length).toBe(1);
      expect(service.conversations()[0].id).toBe('conv-2');
    });

    it('should_switch_active_to_next_when_deleting_active', () => {
      service.loadConversations().subscribe();
      httpMock.expectOne(apiUrl).flush(mockConversations);

      service.setActiveConversation('conv-1');

      service.deleteConversation('conv-1').subscribe();
      httpMock.expectOne(`${apiUrl}/conv-1`).flush(null);

      expect(service.activeConversationId()).toBe('conv-2');
    });

    it('should_set_null_when_deleting_last_conversation', () => {
      service.loadConversations().subscribe();
      httpMock.expectOne(apiUrl).flush([mockConversations[0]]);

      service.setActiveConversation('conv-1');

      service.deleteConversation('conv-1').subscribe();
      httpMock.expectOne(`${apiUrl}/conv-1`).flush(null);

      expect(service.activeConversationId()).toBeNull();
    });
  });

  describe('setActiveConversation', () => {
    it('should_set_active_conversation_id', () => {
      service.setActiveConversation('conv-123');
      expect(service.activeConversationId()).toBe('conv-123');
    });
  });

  describe('clearAll', () => {
    it('should_clear_conversations_and_active_id', () => {
      service.loadConversations().subscribe();
      httpMock.expectOne(apiUrl).flush(mockConversations);
      service.setActiveConversation('conv-1');

      service.clearAll();

      expect(service.conversations()).toEqual([]);
      expect(service.activeConversationId()).toBeNull();
    });
  });
});

