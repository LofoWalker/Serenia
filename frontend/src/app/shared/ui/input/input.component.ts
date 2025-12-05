import { Component, ChangeDetectionStrategy, input, forwardRef, signal, computed } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
type InputType = 'text' | 'email' | 'password';
@Component({
  selector: 'app-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => InputComponent),
      multi: true
    }
  ],
  templateUrl: './input.component.html',
  styleUrl: './input.component.css'
})
export class InputComponent implements ControlValueAccessor {
  readonly label = input<string>('');
  readonly type = input<InputType>('text');
  readonly placeholder = input('');
  readonly error = input<string>('');
  readonly required = input(false);
  readonly autocomplete = input<string>('off');
  readonly inputId = input(`input-${Math.random().toString(36).substring(2, 9)}`);
  protected readonly value = signal('');
  protected readonly isDisabled = signal(false);
  protected readonly showPassword = signal(false);
  protected readonly currentType = computed(() => {
    if (this.type() === 'password') {
      return this.showPassword() ? 'text' : 'password';
    }
    return this.type();
  });
  protected readonly inputClasses = computed(() => {
    const base = 'w-full px-4 py-2.5 bg-primary-800 border rounded-lg text-primary-100 placeholder-primary-500 transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-primary-400 focus:border-transparent';
    const errorClass = this.error() ? 'border-red-500' : 'border-primary-700 hover:border-primary-600';
    const passwordPadding = this.type() === 'password' ? 'pr-12' : '';
    return `${base} ${errorClass} ${passwordPadding}`;
  });
  private onChange: (value: string) => void = () => {};
  protected onTouched: () => void = () => {};
  writeValue(value: string): void {
    this.value.set(value || '');
  }
  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
  setDisabledState(isDisabled: boolean): void {
    this.isDisabled.set(isDisabled);
  }
  protected onInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.value.set(target.value);
    this.onChange(target.value);
  }
  protected togglePasswordVisibility(): void {
    this.showPassword.update(show => !show);
  }
}
