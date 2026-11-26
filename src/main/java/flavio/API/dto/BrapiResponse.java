package flavio.API.dto;

import java.math.BigDecimal;
import java.util.List;

public record BrapiResponse(List<BrapiQuoteItem> results) {

    public record BrapiQuoteItem(
        String symbol,
        BrapiQuoteData data
    ) {}

    public record BrapiQuoteData(
        BigDecimal regularMarketPrice,
        BigDecimal regularMarketChangePercent
    ) {}
}
