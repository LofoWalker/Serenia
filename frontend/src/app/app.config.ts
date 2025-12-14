import {APP_INITIALIZER, ApplicationConfig, inject, provideBrowserGlobalErrorListeners} from '@angular/core';
import {provideRouter, withComponentInputBinding} from '@angular/router';
import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {firstValueFrom} from 'rxjs';

import {routes} from './app.routes';
import {authInterceptor} from './core/interceptors/auth.interceptor';
import {AuthService} from './core/services/auth.service';

function initializeApp() {
  const authService = inject(AuthService);
  return () => firstValueFrom(authService.restoreSession()).then(() => {});
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeApp,
      multi: true
    }
  ]
};
