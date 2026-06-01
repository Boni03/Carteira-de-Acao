package com.Carteira_de_Acao.demo.repository;

import com.Carteira_de_Acao.demo.entity.Operacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoRepository extends JpaRepository<Operacao, Long> {

	List<Operacao> findByAcaoIdOrderByDataHoraDesc(Long acaoId);

	List<Operacao> findAllByOrderByDataHoraDesc();
}
