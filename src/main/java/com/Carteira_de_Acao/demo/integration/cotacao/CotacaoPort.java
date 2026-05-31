package com.Carteira_de_Acao.demo.integration.cotacao;

import com.Carteira_de_Acao.demo.enums.Mercado;
import com.Carteira_de_Acao.demo.integration.cotacao.dto.CotacaoExterna;

public interface CotacaoPort {

	boolean suporta(Mercado mercado);

	CotacaoExterna consultar(String ticker);
}
