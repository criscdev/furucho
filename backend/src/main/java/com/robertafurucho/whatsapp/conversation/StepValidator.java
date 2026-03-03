package com.robertafurucho.whatsapp.conversation;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.Optional;

/**
 * Validates user input for each conversation step.
 *
 * <p>Mirrors the frontend rules from {@code useOrderFormValidation.ts}
 * complemented by the backend constraints from
 * {@link com.robertafurucho.order.CreateOrderRequest}.
 *
 * @return {@link Optional#empty()} if valid, or an error message in Portuguese
 */
@Component
public class StepValidator {

    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String PHONE_REGEX = "^\\d{10,11}$";
    private static final String CEP_REGEX = "^\\d{5}-?\\d{3}$";
    private static final String DATE_REGEX = "^\\d{2}/\\d{2}/\\d{4}$";

    /**
     * Validates the given input for the specified conversation step.
     *
     * @param step  the current conversation step
     * @param input the user's raw text input
     * @return empty if valid, or a Portuguese error message
     */
    public Optional<String> validate(ConversationStep step, String input) {
        return switch (step) {
            case ASK_NAME -> validateName(input);
            case ASK_EMAIL -> validateEmail(input);
            case ASK_PHONE -> validatePhone(input);
            case ASK_ADDRESS -> validateAddress(input);
            case ASK_POSTAL_CODE -> validatePostalCode(input);
            case ASK_ORDER_SCOPE -> validateOrderScope(input);
            case ASK_DETAIL -> validateOrderScopeDetail(input);
            case ASK_DATE -> validateReceiveDate(input);
            default -> Optional.empty();
        };
    }

    private Optional<String> validateName(String input) {
        if (input == null || input.isBlank()) {
            return Optional.of("Nome é obrigatório");
        }
        if (input.length() > 200) {
            return Optional.of("Nome deve ter no máximo 200 caracteres");
        }
        return Optional.empty();
    }

    private Optional<String> validateEmail(String input) {
        if (input == null || input.isBlank()) {
            return Optional.of("Email é obrigatório");
        }
        if (!input.matches(EMAIL_REGEX)) {
            return Optional.of("Email inválido");
        }
        if (input.length() > 100) {
            return Optional.of("Email deve ter no máximo 100 caracteres");
        }
        return Optional.empty();
    }

    private Optional<String> validatePhone(String input) {
        if (input == null || input.isBlank()) {
            return Optional.of("Telefone é obrigatório");
        }
        String digits = input.replaceAll("\\D", "");
        if (!digits.matches(PHONE_REGEX)) {
            return Optional.of("Telefone deve ter 10 ou 11 dígitos");
        }
        return Optional.empty();
    }

    private Optional<String> validateAddress(String input) {
        if (input == null || input.isBlank()) {
            return Optional.of("Endereço é obrigatório");
        }
        if (input.length() > 200) {
            return Optional.of("Endereço deve ter no máximo 200 caracteres");
        }
        return Optional.empty();
    }

    private Optional<String> validatePostalCode(String input) {
        if (input == null || input.isBlank()) {
            return Optional.of("CEP é obrigatório");
        }
        if (!input.matches(CEP_REGEX)) {
            return Optional.of("CEP inválido (formato: 00000-000)");
        }
        return Optional.empty();
    }

    private Optional<String> validateOrderScope(String input) {
        if (input == null || input.isBlank()) {
            return Optional.of("Tipo de boneca é obrigatório");
        }
        if (input.length() > 100) {
            return Optional.of("Tipo deve ter no máximo 100 caracteres");
        }
        return Optional.empty();
    }

    private Optional<String> validateOrderScopeDetail(String input) {
        if (input == null || input.isBlank()) {
            return Optional.of("Detalhes do pedido são obrigatórios");
        }
        if (input.length() > 800) {
            return Optional.of("Detalhes devem ter no máximo 800 caracteres");
        }
        return Optional.empty();
    }

    private Optional<String> validateReceiveDate(String input) {
        if (input == null || input.isBlank()) {
            return Optional.of("Data de entrega é obrigatória");
        }
        if (!input.matches(DATE_REGEX)) {
            return Optional.of("Data inválida (formato: DD/MM/AAAA)");
        }
        try {
            String[] parts = input.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            LocalDate date = LocalDate.of(year, month, day);

            // Verify parsed date matches input (catches 31/02, 32/01, etc.)
            if (date.getDayOfMonth() != day || date.getMonthValue() != month || date.getYear() != year) {
                return Optional.of("Data inválida");
            }
            if (!date.isAfter(LocalDate.now())) {
                return Optional.of("Data deve ser no futuro");
            }
        } catch (DateTimeException | NumberFormatException e) {
            return Optional.of("Data inválida");
        }
        return Optional.empty();
    }
}
