package flavio.API.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoCarteiraDTO(
    BigDecimal valorTotalCarteira,
    List<AtivoCarteiraDTO> ativos
) {}
