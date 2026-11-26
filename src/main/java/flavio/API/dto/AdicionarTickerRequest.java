package flavio.API.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdicionarTickerRequest(
    @Email(message = "E-mail inválido") String email,
    @NotBlank(message = "Informe ao menos um ticker") String tickers
) {}
