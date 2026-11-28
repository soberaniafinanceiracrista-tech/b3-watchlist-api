package flavio.API.controller;

import flavio.API.dto.AdicionarTickerRequest;
import flavio.API.dto.ResumoCarteiraDTO;
import flavio.API.service.CarteiraService;
import flavio.API.service.CotacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import flavio.API.dto.AtualizarQuantidadeRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carteira")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Carteira B3", description = "Gerenciamento de tickers e consulta de cotações")
public class CarteiraController {

    private final CarteiraService carteiraService;
    private final CotacaoService cotacaoService;

    @PostMapping("/ativos")
    @Operation(summary = "Consulta ou adiciona tickers com quantidade opcional")
    public ResponseEntity<ResumoCarteiraDTO> consultarOuAdicionar(@RequestBody @Valid AdicionarTickerRequest request) {
        return ResponseEntity.ok(
            carteiraService.consultarOuAdicionarTickers(request.email(), request.tickers(), request.quantidade())
        );
    }

    @GetMapping
    @Operation(summary = "Lista todos os ativos e o valor total da carteira do e-mail informado")
    public ResponseEntity<ResumoCarteiraDTO> listarCarteira(@RequestParam(required = false) String email) {
        return ResponseEntity.ok(carteiraService.listarCarteira(email));
    }

    @DeleteMapping("/ativos/{ticker}")
    @Operation(summary = "Remove um ticker da carteira do e-mail informado")
    public ResponseEntity<Void> removerAtivo(@RequestParam String email, @PathVariable String ticker) {
        carteiraService.removerTicker(email, ticker);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/atualizar-agora")
    @Operation(summary = "Força a atualização imediata de todos os tickers cadastrados")
    public ResponseEntity<Void> forcarAtualizacao() {
        cotacaoService.atualizarTodasCotacoes();
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/ativos/{ticker}")
    @Operation(summary = "Atualiza apenas a quantidade de um ativo já salvo na carteira")
    public ResponseEntity<ResumoCarteiraDTO> atualizarQuantidade(
            @PathVariable String ticker,
            @RequestBody @Valid AtualizarQuantidadeRequest request) {
        return ResponseEntity.ok(
            carteiraService.atualizarQuantidade(request.email(), ticker, request.quantidade())
        );
    }

}
