package flavio.API.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "carteira_itens", schema = "watchlist")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CarteiraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal quantidade = BigDecimal.ONE;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private OffsetDateTime criadoEm;
}
