package flavio.API.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AtivoCarteiraDTO(
    String ticker,
    BigDecimal quantidade,
    BigDecimal precoAtual,
    BigDecimal valorTotal,
    BigDecimal variacaoDia,
    OffsetDateTime atualizadoEm
) {}
