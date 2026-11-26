package flavio.API.service;
import flavio.API.dto.AtivoCarteiraDTO;
import flavio.API.model.CarteiraItem;
import flavio.API.model.Usuario;
import flavio.API.repository.CarteiraItemRepository;
import flavio.API.repository.CotacaoRepository;
import flavio.API.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<AtivoCarteiraDTO> consultarOuAdicionarTickers(String email, String tickersTexto) {
        List<String> listaTickers = extrairTickers(tickersTexto);
        boolean temEmail = email != null && !email.isBlank();

        Usuario usuario = temEmail ? obterOuCriarUsuario(email) : null;

        for (String ticker : listaTickers) {
            // Se informou e-mail, salva na carteira permanente do banco
            if (usuario != null && !carteiraItemRepository.existsByUsuarioIdAndTickerIgnoreCase(usuario.getId(), ticker)) {
                carteiraItemRepository.save(CarteiraItem.builder()
                        .usuario(usuario)
                        .ticker(ticker)
                        .build());
            }
            // Busca/atualiza a cotação na Brapi e salva na tabela cotacoes
            cotacaoService.buscarESalvarTicker(ticker);
        }

        // Se tem e-mail, retorna toda a carteira salva dele; senão, retorna apenas os tickers pesquisados agora
        if (usuario != null) {
            return carteiraItemRepository.buscarCarteiraComCotacoes(usuario.getId());
        } else {
            return listaTickers.stream()
                    .map(t -> cotacaoRepository.findById(t)
                            .map(c -> new AtivoCarteiraDTO(c.getTicker(), c.getPrecoAtual(), c.getVariacaoDia(), c.getAtualizadoEm()))
                            .orElse(new AtivoCarteiraDTO(t, null, null, null)))
                    .toList();
        }
    }

    @Transactional(readOnly = true)
    public List<AtivoCarteiraDTO> listarCarteira(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        Usuario usuario = obterOuCriarUsuario(email);
        return carteiraItemRepository.buscarCarteiraComCotacoes(usuario.getId());
    }

    @Transactional
    public void removerTicker(String email, String ticker) {
        if (email == null || email.isBlank()) return;
        Usuario usuario = obterOuCriarUsuario(email);
        carteiraItemRepository.deleteByUsuarioIdAndTickerIgnoreCase(usuario.getId(), ticker.trim());
    }
}
