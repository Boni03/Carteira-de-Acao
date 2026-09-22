package com.Carteira_de_Acao.demo.repository;

import com.Carteira_de_Acao.demo.entity.Acao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

	Optional<Acao> findByTickerIgnoreCase(String ticker);

	boolean existsByTickerIgnoreCase(String ticker);

	boolean existsByCorretoraRelacionadaId(Long corretoraId);
}
