package flavio.API.dto;

import java.math.BigDecimal;
import java.util.List;

public record BrapiResponse(List<BrapiQuote> results) {
    public record BrapiQuote(
        String symbol,
        BigDecimal regularMarketPrice,
        BigDecimal regularMarketChangePercent
    ) {}
}
