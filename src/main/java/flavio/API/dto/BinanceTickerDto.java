package flavio.API.dto;

import java.math.BigDecimal;

public record BinanceTickerDto(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal priceChangePercent
) {}
