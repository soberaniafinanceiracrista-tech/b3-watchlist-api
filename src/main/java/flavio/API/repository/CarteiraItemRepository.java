package flavio.API.repository;

import flavio.API.dto.AtivoCarteiraDTO;
import flavio.API.model.CarteiraItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarteiraItemRepository extends JpaRepository<CarteiraItem, Long> {

    @Query("SELECT DISTINCT c.ticker FROM CarteiraItem c")
    List<String> findAllDistinctTickers();

    boolean existsByUsuarioIdAndTickerIgnoreCase(UUID usuarioId, String ticker);

    Optional<CarteiraItem> findByUsuarioIdAndTickerIgnoreCase(UUID usuarioId, String ticker);

    void deleteByUsuarioIdAndTickerIgnoreCase(UUID usuarioId, String ticker);

    @Query("""
        SELECT new flavio.API.dto.AtivoCarteiraDTO(
            ci.ticker,
            ci.quantidade,
            cot.precoAtual,
            CAST(0 AS java.math.BigDecimal),
            cot.variacaoDia,
            cot.atualizadoEm
        )
        FROM CarteiraItem ci
        LEFT JOIN Cotacao cot ON UPPER(ci.ticker) = UPPER(cot.ticker)
        WHERE ci.usuario.id = :usuarioId
        ORDER BY ci.ticker ASC
    """)
    List<AtivoCarteiraDTO> buscarCarteiraComCotacoes(@Param("usuarioId") UUID usuarioId);
}
