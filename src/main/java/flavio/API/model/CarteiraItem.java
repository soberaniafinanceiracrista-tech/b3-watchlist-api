package flavio.API.model;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(name = "criado_em", insertable = false, updatable = false)
    private OffsetDateTime criadoEm;
}
