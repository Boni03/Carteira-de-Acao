package com.Carteira_de_Acao.demo.repository;

import com.Carteira_de_Acao.demo.entity.Corretora;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorretoraRepository extends JpaRepository<Corretora, Long> {

	Optional<Corretora> findByCnpj(String cnpj);

	boolean existsByCnpj(String cnpj);
}
