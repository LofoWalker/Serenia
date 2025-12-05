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
  template: `
    <div class="w-full">
      @if (label()) {
        <label 
          [attr.for]="inputId()" 
          class="block text-sm font-medium text-primary-200 mb-1.5"
        >
          {{ label() }}
          @if (required()) {
            <span class="text-red-400" aria-hidden="true">*</span>
          }
        </label>
      }
      <div class="relative">
        <input
          [id]="inputId()"
          [type]="currentType()"
          [placeholder]="placeholder()"
          [disabled]="isDisabled()"
          [attr.aria-invalid]="error() ? 'true' : null"
          [attr.aria-describedby]="error() ? inputId() + '-error' : null"
          [attr.autocomplete]="autocomplete()"
          [class]="inputClasses()"
          [value]="value()"
          (input)="onInput($event)"
          (blur)="onTouched()"
        />
        @if (type() === 'password') {
          <button
            type="button"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-primary-400 hover:text-primary-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-400 rounded"
            (click)="togglePasswordVisibility()"
            [attr.aria-label]="showPassword() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'"
          >
            @if (showPassword()) {
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
              </svg>
            } @else {
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            }
          </button>
        }
      </div>
      @if (error()) {
        <p 
          [id]="inputId() + '-error'" 
          class="mt-1.5 text-sm text-red-400"
          role="alert"
        >
          {{ error() }}
        </p>
      }
    </div>
  `
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
