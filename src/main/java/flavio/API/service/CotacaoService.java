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

    public CotacaoService(CotacaoRepository cotacaoRepository,
                          CarteiraItemRepository carteiraItemRepository,
                          @Value("${brapi.token}") String brapiToken) {
        this.cotacaoRepository = cotacaoRepository;
        this.carteiraItemRepository = carteiraItemRepository;
        this.restClient = RestClient.builder()
                .baseUrl("https://brapi.dev/api/v2/stocks")
                .defaultHeader("Authorization", brapiToken)
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
        // Como a rota v2 aceita vários tickers por vírgula, busca em lote ou individualmente
        for (String ticker : tickers) {
            buscarESalvarTicker(ticker);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void buscarESalvarTicker(String ticker) {
        String tickerLimpo = ticker.trim().toUpperCase();
        try {
            BrapiResponse response = restClient.get()
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
                    log.info("Cotação salva para {}: R$ {} ({}%)",
                            tickerLimpo, cotacao.getPrecoAtual(), cotacao.getVariacaoDia());
                }
            }
        } catch (RestClientException e) {
            log.warn("Ticker não encontrado ou erro ao consultar {}: {}", tickerLimpo, e.getMessage());
        }
    }
}
