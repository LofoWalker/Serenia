import { Component, ChangeDetectionStrategy, input } from '@angular/core';
type AlertType = 'success' | 'error' | 'warning' | 'info';
@Component({
  selector: 'app-alert',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div 
      [class]="alertClasses[type()]"
      role="alert"
      [attr.aria-live]="type() === 'error' ? 'assertive' : 'polite'"
    >
      <div class="flex items-start gap-3">
        @switch (type()) {
          @case ('success') {
            <svg class="h-5 w-5 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          }
          @case ('error') {
            <svg class="h-5 w-5 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          }
          @case ('warning') {
            <svg class="h-5 w-5 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          }
          @case ('info') {
            <svg class="h-5 w-5 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          }
        }
        <p class="text-sm font-medium">{{ message() }}</p>
      </div>
    </div>
  `
})
export class AlertComponent {
  readonly type = input<AlertType>('info');
  readonly message = input.required<string>();
  protected readonly alertClasses: Record<AlertType, string> = {
    success: 'p-4 rounded-lg bg-green-900/50 border border-green-700 text-green-300',
    error: 'p-4 rounded-lg bg-red-900/50 border border-red-700 text-red-300',
    warning: 'p-4 rounded-lg bg-yellow-900/50 border border-yellow-700 text-yellow-300',
    info: 'p-4 rounded-lg bg-blue-900/50 border border-blue-700 text-blue-300'
  };
}
