package flavio.API.service;

import flavio.API.dto.AtivoCarteiraDTO;
import flavio.API.dto.ResumoCarteiraDTO;
import flavio.API.model.CarteiraItem;
import flavio.API.model.Usuario;
import flavio.API.repository.CarteiraItemRepository;
import flavio.API.repository.CotacaoRepository;
import flavio.API.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarteiraService {

    private final UsuarioRepository usuarioRepository;
    private final CarteiraItemRepository carteiraItemRepository;
    private final CotacaoRepository cotacaoRepository;
    private final CotacaoService cotacaoService;

    private Usuario obterOuCriarUsuario(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email.trim())
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .email(email.trim().toLowerCase())
                                .senhaHash("uso-pessoal")
                                .build()
                ));
    }

    private List<String> extrairTickers(String tickersTexto) {
        return Arrays.stream(tickersTexto.split(","))
                .map(t -> t.trim().toUpperCase())
                .filter(t -> !t.isEmpty())
                .distinct()
                .toList();
    }

    @Transactional
    public ResumoCarteiraDTO consultarOuAdicionarTickers(String email, String tickersTexto, BigDecimal quantidadeInformada) {
        List<String> listaTickers = extrairTickers(tickersTexto);
        boolean temEmail = email != null && !email.isBlank();
        BigDecimal qtd = (quantidadeInformada != null && quantidadeInformada.compareTo(BigDecimal.ZERO) > 0)
                ? quantidadeInformada
                : BigDecimal.ONE;

        Usuario usuario = temEmail ? obterOuCriarUsuario(email) : null;

        for (String ticker : listaTickers) {
            if (usuario != null) {
                CarteiraItem item = carteiraItemRepository
                        .findByUsuarioIdAndTickerIgnoreCase(usuario.getId(), ticker)
                        .orElseGet(() -> CarteiraItem.builder()
                                .usuario(usuario)
                                .ticker(ticker)
                                .build());

                item.setQuantidade(qtd);
                carteiraItemRepository.save(item);
            }
            cotacaoService.buscarESalvarTicker(ticker);
        }

        List<AtivoCarteiraDTO> ativosBrutos;
        if (usuario != null) {
            ativosBrutos = carteiraItemRepository.buscarCarteiraComCotacoes(usuario.getId());
        } else {
            ativosBrutos = listaTickers.stream()
                    .map(t -> cotacaoRepository.findById(t)
                            .map(c -> new AtivoCarteiraDTO(c.getTicker(), qtd, c.getPrecoAtual(), BigDecimal.ZERO, c.getVariacaoDia(), c.getAtualizadoEm()))
                            .orElse(new AtivoCarteiraDTO(t, qtd, null, BigDecimal.ZERO, null, null)))
                    .toList();
        }

        return calcularTotais(ativosBrutos);
    }

        @Transactional
    public ResumoCarteiraDTO atualizarQuantidade(String email, String ticker, BigDecimal novaQuantidade) {
        Usuario usuario = obterOuCriarUsuario(email);
        CarteiraItem item = carteiraItemRepository
                .findByUsuarioIdAndTickerIgnoreCase(usuario.getId(), ticker.trim())
                .orElseThrow(() -> new IllegalArgumentException("Ativo não encontrado na carteira: " + ticker));

        item.setQuantidade(novaQuantidade);
        carteiraItemRepository.save(item);

        List<AtivoCarteiraDTO> ativosBrutos = carteiraItemRepository.buscarCarteiraComCotacoes(usuario.getId());
        return calcularTotais(ativosBrutos);
    }


    @Transactional(readOnly = true)
    public ResumoCarteiraDTO listarCarteira(String email) {
        if (email == null || email.isBlank()) {
            return new ResumoCarteiraDTO(BigDecimal.ZERO, List.of());
        }
        Usuario usuario = obterOuCriarUsuario(email);
        List<AtivoCarteiraDTO> ativosBrutos = carteiraItemRepository.buscarCarteiraComCotacoes(usuario.getId());
        return calcularTotais(ativosBrutos);
    }

    private ResumoCarteiraDTO calcularTotais(List<AtivoCarteiraDTO> ativosBrutos) {
        List<AtivoCarteiraDTO> ativosCalculados = ativosBrutos.stream()
                .map(ativo -> {
                    BigDecimal totalAtivo = (ativo.precoAtual() != null && ativo.quantidade() != null)
                            ? ativo.precoAtual().multiply(ativo.quantidade()).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return new AtivoCarteiraDTO(
                            ativo.ticker(),
                            ativo.quantidade(),
                            ativo.precoAtual(),
                            totalAtivo,
                            ativo.variacaoDia(),
                            ativo.atualizadoEm()
                    );
                })
                .toList();

        BigDecimal totalCarteira = ativosCalculados.stream()
                .map(AtivoCarteiraDTO::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new ResumoCarteiraDTO(totalCarteira, ativosCalculados);
    }

    @Transactional
    public void removerTicker(String email, String ticker) {
        if (email == null || email.isBlank()) return;
        Usuario usuario = obterOuCriarUsuario(email);
        carteiraItemRepository.deleteByUsuarioIdAndTickerIgnoreCase(usuario.getId(), ticker.trim());
    }
}
