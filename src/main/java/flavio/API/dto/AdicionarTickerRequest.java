package flavio.API.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdicionarTickerRequest(
    @NotBlank @Email String email,
    @NotBlank String ticker
) {}
