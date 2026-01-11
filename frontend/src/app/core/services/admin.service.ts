import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap, finalize } from 'rxjs';
import { Dashboard, Timeline, UserList, UserDetail } from '../models/admin.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/admin`;

  private readonly loadingSignal = signal(false);
  private readonly dashboardSignal = signal<Dashboard | null>(null);

  readonly loading = this.loadingSignal.asReadonly();
  readonly dashboard = this.dashboardSignal.asReadonly();

  loadDashboard(): Observable<Dashboard> {
    this.loadingSignal.set(true);
    return this.http.get<Dashboard>(`${this.apiUrl}/dashboard`).pipe(
      tap(data => this.dashboardSignal.set(data)),
      finalize(() => this.loadingSignal.set(false))
    );
  }

  getTimeline(metric: string, days: number): Observable<Timeline> {
    return this.http.get<Timeline>(`${this.apiUrl}/timeline`, {
      params: { metric, days: days.toString() }
    });
  }

  getUsers(page: number = 0, size: number = 20): Observable<UserList> {
    return this.http.get<UserList>(`${this.apiUrl}/users`, {
      params: { page: page.toString(), size: size.toString() }
    });
  }

  getUserByEmail(email: string): Observable<UserDetail> {
    return this.http.get<UserDetail>(`${this.apiUrl}/users/${encodeURIComponent(email)}`);
  }
}

