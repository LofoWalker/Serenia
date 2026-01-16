package com.lofo.serenia.validation.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordConstraintValidator unit tests")
class PasswordConstraintValidatorTest {

    private PasswordConstraintValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new PasswordConstraintValidator();
    }

    private void setupMocksForCustomMessage() {
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Nested
    @DisplayName("Null and empty passwords")
    class NullAndEmptyPasswords {

        @ParameterizedTest
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should_return_false_when_password_is_blank")
        void should_return_false_when_password_is_blank(String password) {
            assertThat(validator.isValid(password, context)).isFalse();
        }

        @Test
        @DisplayName("should_return_false_when_password_is_null")
        void should_return_false_when_password_is_null() {
            assertThat(validator.isValid(null, context)).isFalse();
        }
    }

    @Nested
    @DisplayName("Minimum length validation")
    class MinimumLengthValidation {

        @ParameterizedTest
        @ValueSource(strings = {"Abc1!", "Ab1@", "A1@bcde"})
        @DisplayName("should_return_false_when_password_is_shorter_than_8_characters")
        void should_return_false_when_password_is_shorter_than_8_characters(String password) {
            setupMocksForCustomMessage();

            assertThat(validator.isValid(password, context)).isFalse();
            verify(context).buildConstraintViolationWithTemplate(
                    "Password must contain at least 8 characters");
        }

        @Test
        @DisplayName("should_return_true_when_password_has_exactly_8_characters_and_meets_criteria")
        void should_return_true_when_password_has_exactly_8_characters_and_meets_criteria() {
            assertThat(validator.isValid("Abcd12!@", context)).isTrue();
        }
    }

    @Nested
    @DisplayName("Criteria validation")
    class CriteriaValidation {

        @ParameterizedTest
        @ValueSource(strings = {
                "Abcdefg1",
                "Abcdefg!",
                "ABCDEFG1!",
                "abcdefg1!",
                "Abcdefg1!"
        })
        @DisplayName("should_return_true_when_password_meets_at_least_3_criteria")
        void should_return_true_when_password_meets_at_least_3_criteria(String password) {
            assertThat(validator.isValid(password, context)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Abcdefgh", "abcdefg1", "ABCDEFG1", "abcdefgh"})
        @DisplayName("should_return_false_when_password_meets_only_2_criteria")
        void should_return_false_when_password_meets_only_2_criteria(String password) {
            setupMocksForCustomMessage();

            assertThat(validator.isValid(password, context)).isFalse();
            verify(context).buildConstraintViolationWithTemplate(
                    "Password must meet at least 3 criteria among: uppercase, lowercase, digit, symbol");
        }

        @ParameterizedTest
        @ValueSource(strings = {"abcdefgh", "12345678", "ABCDEFGH", "!@#$%^&*"})
        @DisplayName("should_return_false_when_password_meets_only_1_criterion")
        void should_return_false_when_password_meets_only_1_criterion(String password) {
            setupMocksForCustomMessage();

            assertThat(validator.isValid(password, context)).isFalse();
        }
    }

    @Nested
    @DisplayName("Symbol recognition")
    class SymbolRecognition {

        @ParameterizedTest
        @ValueSource(strings = {
                "Abcdefg!", "Abcdefg@", "Abcdefg#", "Abcdefg$", "Abcdefg%",
                "Abcdefg^", "Abcdefg&", "Abcdefg*", "Abcdefg(", "Abcdefg)",
                "Abcdefg_", "Abcdefg+", "Abcdefg-", "Abcdefg=", "Abcdefg[",
                "Abcdefg]", "Abcdefg{", "Abcdefg}", "Abcdefg|", "Abcdefg;",
                "Abcdefg:", "Abcdefg'", "Abcdefg,", "Abcdefg.", "Abcdefg/",
                "Abcdefg<", "Abcdefg>", "Abcdefg?", "Abcdefg`", "Abcdefg~"
        })
        @DisplayName("should_return_true_when_password_contains_recognized_symbol")
        void should_return_true_when_password_contains_recognized_symbol(String password) {
            assertThat(validator.isValid(password, context)).isTrue();
        }
    }

    @Nested
    @DisplayName("Real-world examples")
    class RealWorldExamples {

        @ParameterizedTest
        @ValueSource(strings = {"MyP@ssw0rd", "Secur3P@ss!", "Test1234!", "Welcome1@", "P@ssword123"})
        @DisplayName("should_return_true_when_password_is_strong")
        void should_return_true_when_password_is_strong(String password) {
            assertThat(validator.isValid(password, context)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"password", "12345678", "ABCDEFGH", "abcdefgh", "!@#$%^&*"})
        @DisplayName("should_return_false_when_password_is_weak")
        void should_return_false_when_password_is_weak(String password) {
            setupMocksForCustomMessage();

            assertThat(validator.isValid(password, context)).isFalse();
        }
    }
}

