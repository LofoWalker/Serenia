import { TestBed } from '@angular/core/testing';
import { AuthStateService } from './auth-state.service';
import { User } from '../models/user.model';

describe('AuthStateService', () => {
  let service: AuthStateService;

  const mockUser: User = {
    id: '123',
    lastName: 'Doe',
    firstName: 'John',
    email: 'john.doe@example.com',
    role: 'USER',
  };

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthStateService);
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  describe('initialization', () => {
    it('should_be_created', () => {
      expect(service).toBeTruthy();
    });

    it('should_have_null_user_initially', () => {
      expect(service.user()).toBeNull();
    });

    it('should_have_null_token_when_no_stored_token', () => {
      expect(service.token()).toBeNull();
    });

    it('should_not_be_authenticated_initially', () => {
      expect(service.isAuthenticated()).toBe(false);
    });

    it('should_have_empty_user_full_name_initially', () => {
      expect(service.userFullName()).toBe('');
    });
  });

  describe('setUser', () => {
    it('should_update_user_signal', () => {
      service.setUser(mockUser);

      expect(service.user()).toEqual(mockUser);
    });

    it('should_update_user_full_name_when_user_is_set', () => {
      service.setUser(mockUser);

      expect(service.userFullName()).toBe('John Doe');
    });

    it('should_clear_user_when_set_to_null', () => {
      service.setUser(mockUser);
      service.setUser(null);

      expect(service.user()).toBeNull();
      expect(service.userFullName()).toBe('');
    });
  });

  describe('setToken', () => {
    it('should_update_token_signal', () => {
      service.setToken('test-token');

      expect(service.token()).toBe('test-token');
    });

    it('should_store_token_in_session_storage', () => {
      service.setToken('test-token');

      expect(sessionStorage.getItem('serenia_token')).toBe('test-token');
    });

    it('should_remove_token_from_session_storage_when_null', () => {
      service.setToken('test-token');
      service.setToken(null);

      expect(sessionStorage.getItem('serenia_token')).toBeNull();
    });
  });

  describe('setLoading', () => {
    it('should_update_loading_signal', () => {
      service.setLoading(true);

      expect(service.loading()).toBe(true);
    });

    it('should_toggle_loading_state', () => {
      service.setLoading(true);
      service.setLoading(false);

      expect(service.loading()).toBe(false);
    });
  });

  describe('isAuthenticated', () => {
    it('should_return_false_when_only_token_is_set', () => {
      service.setToken('test-token');

      expect(service.isAuthenticated()).toBe(false);
    });

    it('should_return_false_when_only_user_is_set', () => {
      service.setUser(mockUser);

      expect(service.isAuthenticated()).toBe(false);
    });

    it('should_return_true_when_both_token_and_user_are_set', () => {
      service.setToken('test-token');
      service.setUser(mockUser);

      expect(service.isAuthenticated()).toBe(true);
    });
  });

  describe('clear', () => {
    it('should_clear_user_and_token', () => {
      service.setToken('test-token');
      service.setUser(mockUser);

      service.clear();

      expect(service.user()).toBeNull();
      expect(service.token()).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
    });

    it('should_remove_token_from_session_storage', () => {
      service.setToken('test-token');

      service.clear();

      expect(sessionStorage.getItem('serenia_token')).toBeNull();
    });
  });
});
