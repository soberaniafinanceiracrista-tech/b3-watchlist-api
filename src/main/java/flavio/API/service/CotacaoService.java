package flavio.API.service;

import flavio.API.dto.BinanceTickerDto;
import flavio.API.dto.BrapiResponse;
import flavio.API.model.Cotacao;
import flavio.API.repository.CarteiraItemRepository;
import flavio.API.repository.CotacaoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CotacaoService {

    // Padrão B3: 4 letras + 1 ou 2 dígitos (ex: PETR4, MXRF11, AAPL34, PETR4F)
    private static final Pattern PADRAO_TICKER_B3 = Pattern.compile("^[A-Z]{4}\\d{1,2}[A-Z]?$");

    private final RestClient brapiClient;
    private final RestClient binanceClient;
    private final CotacaoRepository cotacaoRepository;
    private final CarteiraItemRepository carteiraItemRepository;

        public CotacaoService(CotacaoRepository cotacaoRepository,
                          CarteiraItemRepository carteiraItemRepository,
                          @Value("${brapi.token}") String brapiToken) {
        this.cotacaoRepository = cotacaoRepository;
        this.carteiraItemRepository = carteiraItemRepository;

        String brapiUrl = "https://" + "brapi.dev" + "/api/v2/stocks";
        String binanceUrl = "https://" + "data-api." + "binance.vision" + "/api/v3";

        this.brapiClient = RestClient.builder()
                .baseUrl(brapiUrl)
                .defaultHeader("Authorization", "Bearer " + brapiToken)
                .build();

        this.binanceClient = RestClient.builder()
                .baseUrl(binanceUrl)
                .build();
    }


    @Scheduled(cron = "0 */20 10-18 * * MON-FRI", zone = "America/Sao_Paulo")
    public void atualizarTodasCotacoes() {
        List<String> tickers = carteiraItemRepository.findAllDistinctTickers();
        if (tickers.isEmpty()) {
            return;
        }

        log.info("Iniciando atualização de {} tickers...", tickers.size());
        for (String ticker : tickers) {
            buscarESalvarTicker(ticker);
            pausarEntreRequisicoes();
        }
    }

    @Scheduled(cron = "0 0 0-9,19-23 * * *", zone = "America/Sao_Paulo")
    @Scheduled(cron = "0 0 * * * SAT,SUN", zone = "America/Sao_Paulo")
    public void atualizarCriptosForaDoPregao() {
        List<String> tickers = carteiraItemRepository.findAllDistinctTickers();
        for (String ticker : tickers) {
            String tickerLimpo = ticker.trim().toUpperCase();
            if (isCriptoativo(tickerLimpo)) {
                buscarESalvarCripto(tickerLimpo);
                pausarEntreRequisicoes();
            }
        }
    }

    public Cotacao buscarESalvarTicker(String ticker) {
        String tickerLimpo = ticker.trim().toUpperCase();

        if (isCriptoativo(tickerLimpo)) {
            return buscarESalvarCripto(tickerLimpo);
        } else {
            return buscarESalvarAcao(tickerLimpo);
        }
    }

    private boolean isCriptoativo(String ticker) {
        return !PADRAO_TICKER_B3.matcher(ticker).matches();
    }

    private Cotacao buscarESalvarCripto(String symbol) {
        String parBrl = symbol + "BRL";
        try {
            BinanceTickerDto response = binanceClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ticker/24hr")
                            .queryParam("symbol", parBrl)
                            .build())
                    .retrieve()
                    .body(BinanceTickerDto.class);

            if (response != null && response.lastPrice() != null) {
                Cotacao cotacao = Cotacao.builder()
                        .ticker(symbol)
                        .precoAtual(response.lastPrice())
                        .variacaoDia(response.priceChangePercent())
                        .atualizadoEm(OffsetDateTime.now())
                        .build();
                cotacaoRepository.save(cotacao);
                log.info("Cotação CRIPTO salva para {}: R$ {} ({}%)",
                        symbol, cotacao.getPrecoAtual(), cotacao.getVariacaoDia());
                return cotacao;
            }
        } catch (RestClientException e) {
            log.warn("Erro ao consultar criptoativo {} na Binance: {}", symbol, e.getMessage());
        }
        return null;
    }

    private Cotacao buscarESalvarAcao(String tickerLimpo) {
        try {
            BrapiResponse response = brapiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbols", tickerLimpo)
                            .build())
                    .retrieve()
                    .body(BrapiResponse.class);

            if (response != null && response.results() != null && !response.results().isEmpty()) {
                var item = response.results().getFirst();
                if (item.data() != null) {
                    Cotacao cotacao = Cotacao.builder()
                            .ticker(tickerLimpo)
                            .precoAtual(item.data().regularMarketPrice())
                            .variacaoDia(item.data().regularMarketChangePercent())
                            .atualizadoEm(OffsetDateTime.now())
                            .build();
                    cotacaoRepository.save(cotacao);
                    log.info("Cotação B3 salva para {}: R$ {} ({}%)",
                            tickerLimpo, cotacao.getPrecoAtual(), cotacao.getVariacaoDia());
                    return cotacao;
                }
            }
        } catch (RestClientException e) {
            log.warn("Ativo B3 não encontrado ou erro ao consultar {}: {}", tickerLimpo, e.getMessage());
        }
        return null;
    }

    private void pausarEntreRequisicoes() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
