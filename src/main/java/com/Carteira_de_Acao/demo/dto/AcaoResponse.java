package com.Carteira_de_Acao.demo.dto;

import com.Carteira_de_Acao.demo.entity.Acao;
import com.Carteira_de_Acao.demo.enums.Mercado;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AcaoResponse(
		Long id,
		String ticker,
		String nomeEmpresa,
		Mercado mercado,
		String moeda,
		BigDecimal cotacaoAtual,
		LocalDateTime dataHoraCotacao,
		Long corretoraId
) {

	public static AcaoResponse from(Acao acao) {
		Long corretoraId = acao.getCorretoraRelacionada() != null
				? acao.getCorretoraRelacionada().getId()
				: null;
		return new AcaoResponse(
				acao.getId(),
				acao.getTicker(),
				acao.getNomeEmpresa(),
				acao.getMercado(),
				acao.getMoeda(),
				acao.getCotacaoAtual(),
				acao.getDataHoraCotacao(),
				corretoraId);
	}
}
