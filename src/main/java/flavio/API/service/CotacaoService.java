package flavio.API.service;

import flavio.API.dto.BrapiResponse;
import flavio.API.dto.CoinGeckoCoinDto;
import flavio.API.dto.CoinGeckoPriceDto;
import flavio.API.model.Cotacao;
import flavio.API.repository.CarteiraItemRepository;
import flavio.API.repository.CotacaoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CotacaoService {

    // Padrão B3: 4 letras + 1 ou 2 dígitos (ex: PETR4, MXRF11, AAPL34, PETR4F)
    private static final Pattern PADRAO_TICKER_B3 = Pattern.compile("^[A-Z]{4}\\d{1,2}[A-Z]?$");

    private final RestClient brapiClient;
    private final RestClient coinGeckoClient;
    private final CotacaoRepository cotacaoRepository;
    private final CarteiraItemRepository carteiraItemRepository;

    private Set<String> simbolosCriptoCache = Collections.emptySet();
    private Instant ultimaAtualizacaoCacheCripto = Instant.EPOCH;

    public CotacaoService(CotacaoRepository cotacaoRepository,
                          CarteiraItemRepository carteiraItemRepository,
                          @Value("${brapi.token}") String brapiToken,
                          @Value("${coingecko.api-key}") String coinGeckoApiKey) {
        this.cotacaoRepository = cotacaoRepository;
        this.carteiraItemRepository = carteiraItemRepository;

        this.brapiClient = RestClient.builder()
                .baseUrl("https://brapi.dev")
                .defaultHeader("Authorization", "Bearer " + brapiToken)
                .build();

        this.coinGeckoClient = RestClient.builder()
                .baseUrl("https://coingecko.com")
                .defaultHeader("x-cg-demo-api-key", coinGeckoApiKey)
                .build();
    }

    // Atualiza todos os ativos durante o pregão da B3 (10h às 18h, seg a sex)
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

    // Atualiza apenas criptoativos a cada hora fora do pregão (noite e fins de semana)
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

    public void buscarESalvarTicker(String ticker) {
        String tickerLimpo = ticker.trim().toUpperCase();

        if (isCriptoativo(tickerLimpo)) {
            buscarESalvarCripto(tickerLimpo);
        } else {
            buscarESalvarAcao(tickerLimpo);
        }
    }

    private boolean isCriptoativo(String ticker) {
        // 1. Se segue o padrão da B3 (ex: PETR4, HGLG11), não é cripto
        if (PADRAO_TICKER_B3.matcher(ticker).matches()) {
            return false;
        }

        // 2. Carrega ou atualiza o cache de criptomoedas da CoinGecko a cada 24h
        if (simbolosCriptoCache.isEmpty() ||
                Instant.now().isAfter(ultimaAtualizacaoCacheCripto.plus(24, ChronoUnit.HOURS))) {
            carregarListaCriptos();
        }

        // 3. Verifica se o ticker está na lista da CoinGecko (ou assume cripto se o cache falhar e não for B3)
        return simbolosCriptoCache.isEmpty() || simbolosCriptoCache.contains(ticker);
    }

    private void carregarListaCriptos() {
        try {
            List<CoinGeckoCoinDto> coins = coinGeckoClient.get()
                    .uri("/coins/list")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (coins != null && !coins.isEmpty()) {
                Set<String> simbolos = new HashSet<>();
                for (CoinGeckoCoinDto coin : coins) {
                    if (coin.symbol() != null) {
                        simbolos.add(coin.symbol().trim().toUpperCase());
                    }
                }
                this.simbolosCriptoCache = simbolos;
                this.ultimaAtualizacaoCacheCripto = Instant.now();
                log.info("Cache da CoinGecko atualizado com {} símbolos de criptoativos.", simbolos.size());
            }
        } catch (RestClientException e) {
            log.error("Erro ao carregar lista de criptos da CoinGecko: {}", e.getMessage());
        }
    }

    private void buscarESalvarCripto(String symbol) {
        String symbolLower = symbol.toLowerCase();
        try {
            Map<String, CoinGeckoPriceDto> response = coinGeckoClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/simple/price")
                            .queryParam("symbols", symbolLower)
                            .queryParam("vs_currencies", "brl")
                            .queryParam("include_24hr_change", "true")
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.containsKey(symbolLower)) {
                CoinGeckoPriceDto dados = response.get(symbolLower);
                if (dados != null && dados.brl() != null) {
                    Cotacao cotacao = Cotacao.builder()
                            .ticker(symbol)
                            .precoAtual(dados.brl())
                            .variacaoDia(dados.brl24hChange())
                            .atualizadoEm(OffsetDateTime.now())
                            .build();
                    cotacaoRepository.save(cotacao);
                    log.info("Cotação CRIPTO salva para {}: R$ {} ({}%)",
                            symbol, cotacao.getPrecoAtual(), cotacao.getVariacaoDia());
                }
            } else {
                log.warn("Criptoativo não encontrado na CoinGecko: {}", symbol);
            }
        } catch (RestClientException e) {
            log.warn("Erro ao consultar criptoativo {} na CoinGecko: {}", symbol, e.getMessage());
        }
    }

    private void buscarESalvarAcao(String tickerLimpo) {
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
                }
            }
        } catch (RestClientException e) {
            log.warn("Ativo B3 não encontrado ou erro ao consultar {}: {}", tickerLimpo, e.getMessage());
        }
    }

    private void pausarEntreRequisicoes() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
