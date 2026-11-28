package flavio.API.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record AdicionarTickerRequest(
    String email,

    @NotBlank(message = "Informe pelo menos um ticker")
    String tickers,

    @DecimalMin(value = "0.00000001", message = "A quantidade deve ser maior que zero")
    BigDecimal quantidade
) {}
