package flavio.API.controller;

import flavio.API.service.CotacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cotacoes")
@RequiredArgsConstructor
@Tag(name = "Cotações", description = "Gerenciamento e atualização de cotações da B3 e Criptoativos")
public class CotacaoController {

    private final CotacaoService cotacaoService;

    @PostMapping("/atualizar/{ticker}")
    @Operation(summary = "Atualiza manualmente a cotação de um ticker (ex: PETR4 ou BTC)")
    public ResponseEntity<String> atualizarTicker(@PathVariable String ticker) {
        cotacaoService.buscarESalvarTicker(ticker);
        return ResponseEntity.ok("Solicitação de atualização concluída para o ticker: " + ticker.toUpperCase());
    }

    @PostMapping("/atualizar-todas")
    @Operation(summary = "Força a atualização de todos os tickers cadastrados na carteira")
    public ResponseEntity<String> atualizarTodas() {
        cotacaoService.atualizarTodasCotacoes();
        return ResponseEntity.ok("Atualização em lote concluída.");
    }
}
