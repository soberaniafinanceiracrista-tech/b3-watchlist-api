package flavio.API.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AtivoCarteiraDTO(
    String ticker,
    BigDecimal precoAtual,
    BigDecimal variacaoDia,
    OffsetDateTime atualizadoEm
) {}
