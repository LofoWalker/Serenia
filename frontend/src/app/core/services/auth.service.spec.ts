import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { AuthStateService } from './auth-state.service';
import { SubscriptionService } from './subscription.service';
import { AuthResponse, LoginRequest, RegistrationRequest, User } from '../models/user.model';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let authStateSpy: {
    setLoading: ReturnType<typeof vi.fn>;
    setToken: ReturnType<typeof vi.fn>;
    setUser: ReturnType<typeof vi.fn>;
    clear: ReturnType<typeof vi.fn>;
    token: ReturnType<typeof vi.fn>;
  };
  let subscriptionServiceSpy: {
    clearStatus: ReturnType<typeof vi.fn>;
  };

  const mockUser: User = {
    id: '123',
    lastName: 'Doe',
    firstName: 'John',
    email: 'john.doe@example.com',
    role: 'USER',
  };

  const authUrl = `${environment.apiUrl}/auth`;
  const profileUrl = `${environment.apiUrl}/profile`;
  const passwordUrl = `${environment.apiUrl}/password`;

  beforeEach(() => {
    authStateSpy = {
      setLoading: vi.fn(),
      setToken: vi.fn(),
      setUser: vi.fn(),
      clear: vi.fn(),
      token: vi.fn().mockReturnValue('mock-token'),
    };

    subscriptionServiceSpy = {
      clearStatus: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthStateService, useValue: authStateSpy },
        { provide: SubscriptionService, useValue: subscriptionServiceSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should_be_created', () => {
    expect(service).toBeTruthy();
  });

  describe('register', () => {
    it('should_send_registration_request_and_manage_loading_state', () => {
      const request: RegistrationRequest = {
        lastName: 'Doe',
        firstName: 'John',
        email: 'john.doe@example.com',
        password: 'password123',
      };
      const response = { message: 'Registration successful' };

      service.register(request).subscribe((result) => {
        expect(result).toEqual(response);
      });

      expect(authStateSpy.setLoading).toHaveBeenCalledWith(true);

      const req = httpMock.expectOne(`${authUrl}/register`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(response);

      expect(authStateSpy.setLoading).toHaveBeenCalledWith(false);
    });
  });

  describe('activate', () => {
    it('should_send_activation_request_with_token_param', () => {
      const token = 'activation-token';
      const response = { message: 'Account activated' };

      service.activate(token).subscribe((result) => {
        expect(result).toEqual(response);
      });

      expect(authStateSpy.setLoading).toHaveBeenCalledWith(true);

      const req = httpMock.expectOne(`${authUrl}/activate?token=${token}`);
      expect(req.request.method).toBe('GET');
      req.flush(response);

      expect(authStateSpy.setLoading).toHaveBeenCalledWith(false);
    });
  });

  describe('login', () => {
    it('should_send_login_request_and_store_auth_data', () => {
      const request: LoginRequest = {
        email: 'john.doe@example.com',
        password: 'password123',
      };
      const response: AuthResponse = {
        user: mockUser,
        token: 'jwt-token',
      };

      service.login(request).subscribe((result) => {
        expect(result).toEqual(response);
      });

      expect(authStateSpy.setLoading).toHaveBeenCalledWith(true);

      const req = httpMock.expectOne(`${authUrl}/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(response);

      expect(authStateSpy.setToken).toHaveBeenCalledWith('jwt-token');
      expect(authStateSpy.setUser).toHaveBeenCalledWith(mockUser);
      expect(authStateSpy.setLoading).toHaveBeenCalledWith(false);
    });
  });

  describe('getProfile', () => {
    it('should_fetch_user_profile_and_update_state', () => {
      service.getProfile().subscribe((result) => {
        expect(result).toEqual(mockUser);
      });

      const req = httpMock.expectOne(`${profileUrl}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);

      expect(authStateSpy.setUser).toHaveBeenCalledWith(mockUser);
    });
  });

  describe('deleteAccount', () => {
    it('should_delete_account_and_logout', () => {
      service.deleteAccount().subscribe();

      expect(authStateSpy.setLoading).toHaveBeenCalledWith(true);

      const req = httpMock.expectOne(`${profileUrl}`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);

      expect(authStateSpy.clear).toHaveBeenCalled();
      expect(subscriptionServiceSpy.clearStatus).toHaveBeenCalled();
      expect(authStateSpy.setLoading).toHaveBeenCalledWith(false);
    });
  });

  describe('logout', () => {
    it('should_clear_auth_state_and_subscription_status', () => {
      service.logout();

      expect(authStateSpy.clear).toHaveBeenCalled();
      expect(subscriptionServiceSpy.clearStatus).toHaveBeenCalled();
    });
  });

  describe('restoreSession', () => {
    it('should_call_getProfile_when_token_exists', () => {
      authStateSpy.token.mockReturnValue('existing-token');

      service.restoreSession().subscribe((result) => {
        expect(result).toEqual(mockUser);
      });

      const req = httpMock.expectOne(`${profileUrl}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);

      expect(authStateSpy.setUser).toHaveBeenCalledWith(mockUser);
    });

    it('should_return_null_when_no_token', () => {
      authStateSpy.token.mockReturnValue(null);

      service.restoreSession().subscribe((result) => {
        expect(result).toBeNull();
      });

      httpMock.expectNone(`${profileUrl}`);
    });

    it('should_clear_state_and_return_null_on_error', () => {
      authStateSpy.token.mockReturnValue('invalid-token');

      service.restoreSession().subscribe((result) => {
        expect(result).toBeNull();
      });

      const req = httpMock.expectOne(`${profileUrl}`);
      req.flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(authStateSpy.clear).toHaveBeenCalled();
    });
  });
});
