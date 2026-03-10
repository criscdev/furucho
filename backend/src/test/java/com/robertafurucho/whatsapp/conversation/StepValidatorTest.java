package com.robertafurucho.whatsapp.conversation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StepValidator}.
 *
 * <p>Verifies that all 8 order fields are validated with the same rules
 * as the frontend {@code useOrderFormValidation.ts} and backend
 * {@code CreateOrderRequest} constraints.
 */
@DisplayName("StepValidator")
class StepValidatorTest {

    private final StepValidator validator = new StepValidator();

    // ── Name ──────────────────────────────────────

    @Nested
    @DisplayName("ASK_NAME")
    class AskName {

        @Test
        @DisplayName("accepts a valid name")
        void validName() {
            assertThat(validator.validate(ConversationStep.ASK_NAME, "Roberta Furucho"))
                .isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        @DisplayName("rejects blank names")
        void blankName(String input) {
            assertThat(validator.validate(ConversationStep.ASK_NAME, input))
                .isPresent()
                .hasValueSatisfying(msg -> assertThat(msg).contains("Nome"));
        }

        @Test
        @DisplayName("rejects names longer than 200 chars")
        void tooLongName() {
            String longName = "A".repeat(201);
            assertThat(validator.validate(ConversationStep.ASK_NAME, longName))
                .isPresent()
                .hasValueSatisfying(msg -> assertThat(msg).contains("200"));
        }
    }

    // ── Email ─────────────────────────────────────

    @Nested
    @DisplayName("ASK_EMAIL")
    class AskEmail {

        @Test
        @DisplayName("accepts a valid email")
        void validEmail() {
            assertThat(validator.validate(ConversationStep.ASK_EMAIL, "roberta@example.com"))
                .isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"notanemail", "missing@", "@missing.com", "has spaces@x.com"})
        @DisplayName("rejects invalid email formats")
        void invalidEmail(String input) {
            assertThat(validator.validate(ConversationStep.ASK_EMAIL, input))
                .isPresent()
                .hasValueSatisfying(msg -> assertThat(msg).containsIgnoringCase("email"));
        }

        @Test
        @DisplayName("rejects emails longer than 100 chars")
        void tooLongEmail() {
            String longEmail = "a".repeat(91) + "@test.com"; // 100 chars OK
            assertThat(validator.validate(ConversationStep.ASK_EMAIL, longEmail)).isEmpty();

            String tooLong = "a".repeat(92) + "@test.com"; // 101 chars
            assertThat(validator.validate(ConversationStep.ASK_EMAIL, tooLong)).isPresent();
        }
    }

    // ── Phone ─────────────────────────────────────

    @Nested
    @DisplayName("ASK_PHONE")
    class AskPhone {

        @ParameterizedTest
        @ValueSource(strings = {"11987654321", "1199887766", "(11) 98765-4321"})
        @DisplayName("accepts valid phones (10-11 digits after stripping)")
        void validPhone(String input) {
            assertThat(validator.validate(ConversationStep.ASK_PHONE, input)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"123456789", "123456789012", ""})
        @DisplayName("rejects invalid phone digits")
        void invalidPhone(String input) {
            assertThat(validator.validate(ConversationStep.ASK_PHONE, input)).isPresent();
        }
    }

    // ── Address ───────────────────────────────────

    @Nested
    @DisplayName("ASK_ADDRESS")
    class AskAddress {

        @Test
        @DisplayName("accepts a valid address")
        void validAddress() {
            assertThat(validator.validate(ConversationStep.ASK_ADDRESS, "Rua das Bonecas, 42"))
                .isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("rejects blank addresses")
        void blankAddress(String input) {
            assertThat(validator.validate(ConversationStep.ASK_ADDRESS, input)).isPresent();
        }

        @Test
        @DisplayName("rejects addresses longer than 200 chars")
        void tooLongAddress() {
            assertThat(validator.validate(ConversationStep.ASK_ADDRESS, "X".repeat(201)))
                .isPresent();
        }
    }

    // ── Postal Code (CEP) ─────────────────────────

    @Nested
    @DisplayName("ASK_POSTAL_CODE")
    class AskPostalCode {

        @ParameterizedTest
        @ValueSource(strings = {"01234-567", "01234567"})
        @DisplayName("accepts valid CEP formats")
        void validCep(String input) {
            assertThat(validator.validate(ConversationStep.ASK_POSTAL_CODE, input)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"0123-567", "012345678", "abcde-fgh", ""})
        @DisplayName("rejects invalid CEP formats")
        void invalidCep(String input) {
            assertThat(validator.validate(ConversationStep.ASK_POSTAL_CODE, input)).isPresent();
        }
    }

    // ── Order Scope ───────────────────────────────

    @Nested
    @DisplayName("ASK_ORDER_SCOPE")
    class AskOrderScope {

        @Test
        @DisplayName("accepts a valid scope")
        void validScope() {
            assertThat(validator.validate(ConversationStep.ASK_ORDER_SCOPE, "Amigurumi"))
                .isEmpty();
        }

        @Test
        @DisplayName("rejects blank scope")
        void blankScope() {
            assertThat(validator.validate(ConversationStep.ASK_ORDER_SCOPE, "")).isPresent();
        }

        @Test
        @DisplayName("rejects scope longer than 100 chars")
        void tooLongScope() {
            assertThat(validator.validate(ConversationStep.ASK_ORDER_SCOPE, "X".repeat(101)))
                .isPresent();
        }
    }

    // ── Order Detail ──────────────────────────────

    @Nested
    @DisplayName("ASK_DETAIL")
    class AskDetail {

        @Test
        @DisplayName("accepts valid detail text")
        void validDetail() {
            assertThat(validator.validate(ConversationStep.ASK_DETAIL,
                "Boneca com vestido azul e cabelo cacheado")).isEmpty();
        }

        @Test
        @DisplayName("rejects blank detail")
        void blankDetail() {
            assertThat(validator.validate(ConversationStep.ASK_DETAIL, "")).isPresent();
        }

        @Test
        @DisplayName("rejects detail longer than 800 chars")
        void tooLongDetail() {
            assertThat(validator.validate(ConversationStep.ASK_DETAIL, "D".repeat(801)))
                .isPresent();
        }
    }

    // ── Receive Date ──────────────────────────────

    @Nested
    @DisplayName("ASK_DATE")
    class AskDate {

        @Test
        @DisplayName("accepts a valid future date")
        void validFutureDate() {
            LocalDate future = LocalDate.now().plusMonths(2);
            String formatted = future.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            assertThat(validator.validate(ConversationStep.ASK_DATE, formatted)).isEmpty();
        }

        @Test
        @DisplayName("rejects past dates")
        void pastDate() {
            assertThat(validator.validate(ConversationStep.ASK_DATE, "01/01/2020"))
                .isPresent()
                .hasValueSatisfying(msg -> assertThat(msg).contains("futuro"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"31/02/2026", "32/01/2026", "00/01/2026", "01/13/2026"})
        @DisplayName("rejects invalid calendar dates")
        void invalidCalendarDate(String input) {
            assertThat(validator.validate(ConversationStep.ASK_DATE, input)).isPresent();
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-a-date", "2026-01-01", "01-01-2026"})
        @DisplayName("rejects wrong date formats")
        void wrongFormat(String input) {
            assertThat(validator.validate(ConversationStep.ASK_DATE, input)).isPresent();
        }
    }

    // ── Non-data steps ────────────────────────────

    @Test
    @DisplayName("returns empty for non-data steps (GREETING, CONFIRM, etc.)")
    void nonDataSteps() {
        assertThat(validator.validate(ConversationStep.GREETING, "anything")).isEmpty();
        assertThat(validator.validate(ConversationStep.CONFIRM, "anything")).isEmpty();
        assertThat(validator.validate(ConversationStep.CORRECTION, "anything")).isEmpty();
    }
}
