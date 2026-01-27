package com.robertafurucho.order;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO for creating a new order request.
 * 
 * Contains validation constraints matching the frontend form.
 */
public record CreateOrderRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    String name,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    String email,

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 dígitos")
    String phone,

    @NotBlank(message = "Endereço é obrigatório")
    @Size(max = 200, message = "Endereço deve ter no máximo 200 caracteres")
    String address,

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido (formato: 00000-000)")
    String postalCode,

    @NotBlank(message = "Tipo de boneca é obrigatório")
    @Size(max = 100, message = "Tipo deve ter no máximo 100 caracteres")
    String orderScope,

    @NotBlank(message = "Detalhes são obrigatórios")
    @Size(max = 800, message = "Detalhes devem ter no máximo 800 caracteres")
    String orderScopeDetail,

    @NotNull(message = "Data desejada é obrigatória")
    @Future(message = "Data deve ser no futuro")
    LocalDate receiveDate
) {}
