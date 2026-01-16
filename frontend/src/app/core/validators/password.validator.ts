import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Résultat de la validation du mot de passe avec les critères détaillés
 */
export interface PasswordValidationResult {
  isValid: boolean;
  hasMinLength: boolean;
  hasUppercase: boolean;
  hasLowercase: boolean;
  hasDigit: boolean;
  hasSymbol: boolean;
  criteriaCount: number;
}

const MIN_LENGTH = 8;
const MIN_CRITERIA = 3;
const SYMBOLS = '!@#$%^&*()_+-=[]{}|;\':\",./<>?`~\\';

/**
 * Vérifie les critères du mot de passe et retourne le résultat détaillé
 */
export function checkPasswordCriteria(password: string): PasswordValidationResult {
  if (!password) {
    return {
      isValid: false,
      hasMinLength: false,
      hasUppercase: false,
      hasLowercase: false,
      hasDigit: false,
      hasSymbol: false,
      criteriaCount: 0,
    };
  }

  const hasMinLength = password.length >= MIN_LENGTH;
  const hasUppercase = /[A-Z]/.test(password);
  const hasLowercase = /[a-z]/.test(password);
  const hasDigit = /[0-9]/.test(password);
  const hasSymbol = [...password].some((c) => SYMBOLS.includes(c));

  const criteriaCount = [hasUppercase, hasLowercase, hasDigit, hasSymbol].filter(Boolean).length;
  const isValid = hasMinLength && criteriaCount >= MIN_CRITERIA;

  return {
    isValid,
    hasMinLength,
    hasUppercase,
    hasLowercase,
    hasDigit,
    hasSymbol,
    criteriaCount,
  };
}

/**
 * Validateur Angular pour la politique de mot de passe
 *
 * Règles :
 * - Minimum 8 caractères
 * - Au moins 3 critères parmi 4 : majuscule, minuscule, chiffre, symbole
 */
export function passwordValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const password = control.value;
    const result = checkPasswordCriteria(password);

    if (result.isValid) {
      return null;
    }

    return {
      passwordPolicy: {
        message: 'Le mot de passe ne respecte pas la politique de sécurité',
        ...result,
      },
    };
  };
}
