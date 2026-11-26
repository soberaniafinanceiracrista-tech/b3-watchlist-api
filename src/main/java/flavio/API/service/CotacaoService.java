package flavio.API.service;

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

@Service
@Slf4j
public class CotacaoService {

    private final RestClient restClient;
    private final CotacaoRepository cotacaoRepository;
    private final CarteiraItemRepository carteiraItemRepository;
    private final String brapiToken;

    public CotacaoService(CotacaoRepository cotacaoRepository,
                          CarteiraItemRepository carteiraItemRepository,
                          @Value("${brapi.token}") String brapiToken) {
        this.cotacaoRepository = cotacaoRepository;
        this.carteiraItemRepository = carteiraItemRepository;
        this.brapiToken = brapiToken;
        this.restClient = RestClient.builder()
                .baseUrl("https://brapi.dev")
                .build();
    }

    // Atualiza a cada 20 min durante o pregão da B3 (10h às 18h, seg a sex)
    @Scheduled(cron = "0 */20 10-18 * * MON-FRI", zone = "America/Sao_Paulo")
    public void atualizarTodasCotacoes() {
        List<String> tickers = carteiraItemRepository.findAllDistinctTickers();
        log.info("Iniciando atualização de {} tickers...", tickers.size());

        for (String ticker : tickers) {
            buscarESalvarTicker(ticker);
            try {
                Thread.sleep(1500); // Respeita rate limit do plano grátis
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void buscarESalvarTicker(String ticker) {
        String tickerLimpo = ticker.trim().toUpperCase();
        try {
            BrapiResponse response = restClient.get()
                    .uri("/quote/{ticker}?token={token}", tickerLimpo, brapiToken)
                    .retrieve()
                    .body(BrapiResponse.class);

            if (response != null && response.results() != null && !response.results().isEmpty()) {
                var dado = response.results().getFirst();
                Cotacao cotacao = Cotacao.builder()
                        .ticker(tickerLimpo)
                        .precoAtual(dado.regularMarketPrice())
                        .variacaoDia(dado.regularMarketChangePercent())
                        .atualizadoEm(OffsetDateTime.now())
                        .build();
                cotacaoRepository.save(cotacao);
            }
        } catch (RestClientException e) {
            log.warn("Ticker não encontrado ou erro na API para {}: {}", tickerLimpo, e.getMessage());
        }
    }
}
