package flavio.API.dto;

import java.math.BigDecimal;
import java.util.List;

public record BrapiCryptoResponse(
        List<CryptoItem> coins
) {
    public record CryptoItem(
            String coin,
            String coinName,
            String currency,
            BigDecimal regularMarketPrice,
            BigDecimal regularMarketChangePercent
    ) {}
}

