package flavio.API.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CoinGeckoPriceDto(
        BigDecimal brl,
        @JsonProperty("brl_24h_change")
        BigDecimal brl24hChange
) {}
