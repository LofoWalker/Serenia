export interface User {
  id: string;
  lastName: string;
  firstName: string;
  email: string;
  roles: string[];
}

export interface AuthResponse {
  user: User;
  token: string;
}

export interface RegistrationRequest {
  lastName: string;
  firstName: string;
  email: string;
  password: string;
}

export interface ApiMessageResponse {
  message: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ActivationResponse {
  message: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}


