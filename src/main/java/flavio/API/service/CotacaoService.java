package flavio.API.service;

import flavio.API.dto.BrapiCryptoAvailableResponse;
import flavio.API.dto.BrapiCryptoResponse;
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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class CotacaoService {

    private final RestClient restClient;
    private final CotacaoRepository cotacaoRepository;
    private final CarteiraItemRepository carteiraItemRepository;

    private Set<String> criptosDisponiveisCache = Collections.emptySet();
    private Instant ultimaAtualizacaoCacheCripto = Instant.EPOCH;

    public CotacaoService(CotacaoRepository cotacaoRepository,
                          CarteiraItemRepository carteiraItemRepository,
                          @Value("${brapi.token}") String brapiToken) {
        this.cotacaoRepository = cotacaoRepository;
        this.carteiraItemRepository = carteiraItemRepository;
        this.restClient = RestClient.builder()
                .baseUrl("https://brapi.dev/api/v2")
                .defaultHeader("Authorization", "Bearer " + brapiToken)
                .build();
    }

    // Atualiza a cada 20 min durante o pregão da B3 (10h às 18h, seg a sex)
    @Scheduled(cron = "0 */20 10-18 * * MON-FRI", zone = "America/Sao_Paulo")
    public void atualizarTodasCotacoes() {
        List<String> tickers = carteiraItemRepository.findAllDistinctTickers();
        if (tickers.isEmpty()) {
            return;
        }

        log.info("Iniciando atualização de {} tickers...", tickers.size());
        for (String ticker : tickers) {
            buscarESalvarTicker(ticker);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Opcional: Atualiza apenas criptoativos fora do horário da B3 (noite e fins de semana)
    @Scheduled(cron = "0 0 * * * *", zone = "America/Sao_Paulo")
    public void atualizarCriptosForaDoPregao() {
        List<String> tickers = carteiraItemRepository.findAllDistinctTickers();
        for (String ticker : tickers) {
            String tickerLimpo = ticker.trim().toUpperCase();
            if (isCriptoativo(tickerLimpo)) {
                buscarESalvarCripto(tickerLimpo);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
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
        // Atualiza o cache da lista de criptos a cada 24 horas
        if (criptosDisponiveisCache.isEmpty() ||
                Instant.now().isAfter(ultimaAtualizacaoCacheCripto.plus(24, ChronoUnit.HOURS))) {
            carregarCriptosDisponiveis();
        }
        return criptosDisponiveisCache.contains(ticker);
    }

    private void carregarCriptosDisponiveis() {
        try {
            BrapiCryptoAvailableResponse response = restClient.get()
                    .uri("/crypto/available")
                    .retrieve()
                    .body(BrapiCryptoAvailableResponse.class);

            if (response != null && response.coins() != null) {
                Set<String> moedas = new HashSet<>();
                for (String coin : response.coins()) {
                    moedas.add(coin.trim().toUpperCase());
                }
                this.criptosDisponiveisCache = moedas;
                this.ultimaAtualizacaoCacheCripto = Instant.now();
                log.info("Cache de criptoativos atualizado com {} moedas.", moedas.size());
            }
        } catch (RestClientException e) {
            log.error("Erro ao carregar lista de criptoativos disponíveis: {}", e.getMessage());
        }
    }

    private void buscarESalvarCripto(String coin) {
        try {
            BrapiCryptoResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/crypto")
                            .queryParam("coin", coin)
                            .queryParam("currency", "BRL")
                            .build())
                    .retrieve()
                    .body(BrapiCryptoResponse.class);

            if (response != null && response.coins() != null && !response.coins().isEmpty()) {
                var item = response.coins().getFirst();
                Cotacao cotacao = Cotacao.builder()
                        .ticker(coin)
                        .precoAtual(item.regularMarketPrice())
                        .variacaoDia(item.regularMarketChangePercent())
                        .atualizadoEm(OffsetDateTime.now())
                        .build();
                cotacaoRepository.save(cotacao);
                log.info("Cotação CRIPTO salva para {}: R$ {} ({}%)",
                        coin, cotacao.getPrecoAtual(), cotacao.getVariacaoDia());
            }
        } catch (RestClientException e) {
            log.warn("Criptoativo não encontrado ou erro ao consultar {}: {}", coin, e.getMessage());
        }
    }

    private void buscarESalvarAcao(String tickerLimpo) {
        try {
            BrapiResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stocks/quote")
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
}


