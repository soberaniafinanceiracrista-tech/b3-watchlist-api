package flavio.API.repository;

import flavio.API.model.Cotacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotacaoRepository extends JpaRepository<Cotacao, String> {
}