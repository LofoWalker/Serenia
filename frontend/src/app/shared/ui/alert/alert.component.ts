import {ChangeDetectionStrategy, Component, input} from '@angular/core';

type AlertType = 'success' | 'error' | 'warning' | 'info';
@Component({
  selector: 'app-alert',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './alert.component.html',
  styleUrl: './alert.component.css'
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
