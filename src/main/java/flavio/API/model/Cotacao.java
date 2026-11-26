package flavio.API.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cotacoes", schema = "watchlist")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Cotacao {

    @Id
    @Column(length = 20)
    private String ticker;

    @Column(name = "preco_atual", precision = 12, scale = 2)
    private BigDecimal precoAtual;

    @Column(name = "variacao_dia", precision = 8, scale = 2)
    private BigDecimal variacaoDia;

    @Column(name = "atualizado_em")
    private OffsetDateTime atualizadoEm;
}
