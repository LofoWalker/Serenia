import {
  Component,
  OnInit,
  inject,
  signal,
  ElementRef,
  viewChild,
  effect,
  AfterViewInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../core/services/admin.service';
import { Dashboard, Timeline, UserList, UserDetail } from '../../core/models/admin.model';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html'
})
export class AdminDashboardComponent implements OnInit, AfterViewInit {
  private readonly adminService = inject(AdminService);
  readonly Math = Math;

  readonly dashboard = signal<Dashboard | null>(null);
  readonly loading = signal(true);
  readonly selectedDays = signal(7);
  readonly selectedMetric = signal<'users' | 'messages'>('messages');

  readonly userList = signal<UserList | null>(null);
  readonly selectedUser = signal<UserDetail | null>(null);
  searchEmail = '';
  readonly searchError = signal<string | null>(null);
  readonly currentPage = signal(0);
  readonly pageSize = 10;

  private chart: Chart | null = null;
  private readonly chartCanvas = viewChild<ElementRef<HTMLCanvasElement>>('chartCanvas');
  private chartReady = false;

  constructor() {
    effect(() => {
      const days = this.selectedDays();
      const metric = this.selectedMetric();
      if (this.chartReady) {
        this.loadTimeline(metric, days);
      }
    });
  }

  ngOnInit(): void {
    this.adminService.loadDashboard().subscribe({
      next: data => {
        this.dashboard.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
    this.loadUsers();
  }

  ngAfterViewInit(): void {
    this.chartReady = true;
    this.loadTimeline(this.selectedMetric(), this.selectedDays());
  }

  selectDays(days: number): void {
    this.selectedDays.set(days);
  }

  selectMetric(metric: 'users' | 'messages'): void {
    this.selectedMetric.set(metric);
  }

  loadUsers(): void {
    this.adminService.getUsers(this.currentPage(), this.pageSize).subscribe({
      next: data => this.userList.set(data)
    });
  }

  nextPage(): void {
    const list = this.userList();
    if (list && (this.currentPage() + 1) * this.pageSize < list.totalCount) {
      this.currentPage.set(this.currentPage() + 1);
      this.loadUsers();
    }
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.set(this.currentPage() - 1);
      this.loadUsers();
    }
  }

  searchUser(): void {
    const email = this.searchEmail.trim();
    if (!email) return;

    this.searchError.set(null);
    this.adminService.getUserByEmail(email).subscribe({
      next: user => this.selectedUser.set(user),
      error: () => {
        this.searchError.set('Utilisateur non trouvé');
        this.selectedUser.set(null);
      }
    });
  }

  selectUserFromList(user: UserDetail): void {
    this.selectedUser.set(user);
    this.searchEmail = user.email;
  }

  closeUserDetail(): void {
    this.selectedUser.set(null);
  }

  private loadTimeline(metric: string, days: number): void {
    this.adminService.getTimeline(metric, days).subscribe({
      next: timeline => this.renderChart(timeline)
    });
  }

  private renderChart(timeline: Timeline): void {
    const canvas = this.chartCanvas()?.nativeElement;
    if (!canvas) return;

    if (this.chart) {
      this.chart.destroy();
    }

    const labels = timeline.data.map(d => this.formatDate(d.date));
    const values = timeline.data.map(d => d.value);

    this.chart = new Chart(canvas, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: timeline.metric === 'users' ? 'Nouveaux utilisateurs' : 'Messages',
          data: values,
          borderColor: '#8b5cf6',
          backgroundColor: 'rgba(139, 92, 246, 0.1)',
          fill: true,
          tension: 0.3
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { precision: 0 }
          }
        }
      }
    });
  }

  private formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
  }

  formatCurrency(cents: number): string {
    return (cents / 100).toFixed(2) + ' €';
  }
}

