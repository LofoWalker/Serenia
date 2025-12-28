import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { checkPasswordCriteria, PasswordValidationResult } from '../../../core/validators/password.validator';

@Component({
  selector: 'app-password-strength',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './password-strength.component.html'
})
export class PasswordStrengthComponent {
  readonly password = input<string>('');

  protected readonly validation = computed<PasswordValidationResult>(() => {
    return checkPasswordCriteria(this.password());
  });

  protected readonly strengthPercent = computed(() => {
    const v = this.validation();
    let score = 0;

    // Longueur (25%)
    if (v.hasMinLength) score += 25;

    // Chaque critère respecté (jusqu'à 75% pour 4 critères, mais on a besoin de 3)
    score += v.criteriaCount * 18.75;

    return Math.min(100, score);
  });

  protected readonly strengthColor = computed(() => {
    const percent = this.strengthPercent();
    if (percent < 40) return 'bg-red-500';
    if (percent < 70) return 'bg-yellow-500';
    return 'bg-emerald-500';
  });

  protected readonly strengthLabel = computed(() => {
    const percent = this.strengthPercent();
    if (percent < 40) return 'Faible';
    if (percent < 70) return 'Moyen';
    return 'Fort';
  });
}

