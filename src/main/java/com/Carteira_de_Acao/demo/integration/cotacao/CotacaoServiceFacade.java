package com.Carteira_de_Acao.demo.integration.cotacao;

import com.Carteira_de_Acao.demo.enums.Mercado;
import com.Carteira_de_Acao.demo.exception.IntegracaoExternaException;
import com.Carteira_de_Acao.demo.integration.cotacao.dto.CotacaoExterna;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CotacaoServiceFacade {

	private final List<CotacaoPort> provedores;

	public CotacaoServiceFacade(List<CotacaoPort> provedores) {
		this.provedores = provedores;
	}

	public CotacaoExterna consultar(Mercado mercado, String ticker) {
		return provedores.stream()
				.filter(p -> p.suporta(mercado))
				.findFirst()
				.orElseThrow(() -> new IntegracaoExternaException("Nenhum provedor de cotação para o mercado: " + mercado))
				.consultar(ticker);
	}
}
