package com.Carteira_de_Acao.demo.integration.cotacao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CotacaoExterna(
		String ticker,
		String nomeEmpresa,
		String moeda,
		BigDecimal preco,
		LocalDateTime dataHoraCotacao
) {
}
