package flavio.API.dto;

import java.util.List;

public record BrapiCryptoAvailableResponse(
        List<String> coins
) {}
