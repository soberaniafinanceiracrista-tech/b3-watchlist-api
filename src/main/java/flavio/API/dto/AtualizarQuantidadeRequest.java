package flavio.API.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AtualizarQuantidadeRequest(
    @NotBlank(message = "Informe o e-mail")
    String email,

    @NotNull(message = "Informe a nova quantidade")
    @DecimalMin(value = "0.00000001", message = "A quantidade deve ser maior que zero")
    BigDecimal quantidade
) {}
