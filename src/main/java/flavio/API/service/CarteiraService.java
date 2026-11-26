package flavio.API.service;

import flavio.API.dto.AtivoCarteiraDTO;
import flavio.API.model.CarteiraItem;
import flavio.API.model.Usuario;
import flavio.API.repository.CarteiraItemRepository;
import flavio.API.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarteiraService {

    private final UsuarioRepository usuarioRepository;
    private final CarteiraItemRepository carteiraItemRepository;
    private final CotacaoService cotacaoService;

    // Busca usuário pelo e-mail; se não existir, cria automaticamente (prático para uso pessoal)
    private Usuario obterOuCriarUsuario(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email.trim())
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .email(email.trim().toLowerCase())
                                .senhaHash("uso-pessoal")
                                .build()
                ));
    }

    @Transactional
    public void adicionarTicker(String email, String ticker) {
        Usuario usuario = obterOuCriarUsuario(email);
        String tickerLimpo = ticker.trim().toUpperCase();

        if (!carteiraItemRepository.existsByUsuarioIdAndTickerIgnoreCase(usuario.getId(), tickerLimpo)) {
            CarteiraItem item = CarteiraItem.builder()
                    .usuario(usuario)
                    .ticker(tickerLimpo)
                    .build();
            carteiraItemRepository.save(item);
        }

        // Já busca a cotação imediatamente ao adicionar o ticker
        cotacaoService.buscarESalvarTicker(tickerLimpo);
    }

    @Transactional(readOnly = true)
    public List<AtivoCarteiraDTO> listarCarteira(String email) {
        Usuario usuario = obterOuCriarUsuario(email);
        return carteiraItemRepository.buscarCarteiraComCotacoes(usuario.getId());
    }

    @Transactional
    public void removerTicker(String email, String ticker) {
        Usuario usuario = obterOuCriarUsuario(email);
        carteiraItemRepository.deleteByUsuarioIdAndTickerIgnoreCase(usuario.getId(), ticker.trim());
    }
}
