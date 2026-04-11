import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ConversationSidebarComponent } from './conversation-sidebar.component';
import { ConversationListService } from '../../../../core/services/conversation-list.service';
import { signal } from '@angular/core';

describe('ConversationSidebarComponent', () => {
  let component: ConversationSidebarComponent;
  let fixture: ComponentFixture<ConversationSidebarComponent>;

  const mockConversations = signal([
    { id: 'conv-1', name: 'First', lastActivityAt: '2026-04-11T10:00:00Z' },
    { id: 'conv-2', name: 'Second', lastActivityAt: '2026-04-11T11:00:00Z' },
  ]);

  const mockService = {
    conversations: mockConversations.asReadonly(),
    activeConversationId: signal<string | null>('conv-1').asReadonly(),
    loading: signal(false).asReadonly(),
    hasConversations: signal(true).asReadonly(),
    setActiveConversation: vi.fn(),
    renameConversation: vi.fn(),
    deleteConversation: vi.fn(),
    createConversation: vi.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConversationSidebarComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ConversationListService, useValue: mockService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConversationSidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should_create', () => {
    expect(component).toBeTruthy();
  });

  it('should_render_conversation_list', () => {
    const items = fixture.nativeElement.querySelectorAll('[class*="group"]');
    expect(items.length).toBeGreaterThanOrEqual(2);
  });

  it('should_emit_conversation_selected_on_click', () => {
    const spy = vi.spyOn(component.conversationSelected, 'emit');
    const items = fixture.nativeElement.querySelectorAll('[class*="group"]');
    items[0].click();
    expect(spy).toHaveBeenCalledWith('conv-2');
  });

  it('should_emit_conversation_created_on_button_click', () => {
    const spy = vi.spyOn(component.conversationCreated, 'emit');
    const createBtn = fixture.nativeElement.querySelector('[aria-label="Nouvelle conversation"]');
    createBtn.click();
    expect(spy).toHaveBeenCalled();
  });

  it('should_toggle_collapsed_state', () => {
    const collapseBtn = fixture.nativeElement.querySelector(
      '[aria-label="Réduire la sidebar"]',
    );
    collapseBtn.click();
    fixture.detectChanges();

    const expandBtn = fixture.nativeElement.querySelector('[aria-label="Ouvrir la sidebar"]');
    expect(expandBtn).toBeTruthy();
  });
});

