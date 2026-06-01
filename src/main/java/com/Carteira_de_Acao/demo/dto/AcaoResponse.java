package com.Carteira_de_Acao.demo.dto;

import com.Carteira_de_Acao.demo.entity.Acao;
import com.Carteira_de_Acao.demo.enums.Mercado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record AcaoResponse(
		Long id,
		String ticker,
		String nomeEmpresa,
		Mercado mercado,
		String moeda,
		BigDecimal cotacaoAtual,
		LocalDateTime dataHoraCotacao,
		Long corretoraId,
		Integer quantidade,
		BigDecimal precoMedio,
		BigDecimal valorInvestido,
		BigDecimal valorAtual,
		BigDecimal lucroPrejuizoNaoRealizado,
		BigDecimal lucroPrejuizoNaoRealizadoPercentual,
		BigDecimal lucroPrejuizoRealizado
) {

	public static AcaoResponse from(Acao acao) {
		Long corretoraId = acao.getCorretoraRelacionada() != null
				? acao.getCorretoraRelacionada().getId()
				: null;

		int quantidade = acao.getQuantidade();
		BigDecimal precoMedio = acao.getPrecoMedio();
		BigDecimal cotacao = acao.getCotacaoAtual() != null ? acao.getCotacaoAtual() : BigDecimal.ZERO;

		BigDecimal qtd = BigDecimal.valueOf(quantidade);
		BigDecimal valorInvestido = precoMedio.multiply(qtd).setScale(2, RoundingMode.HALF_UP);
		BigDecimal valorAtual = cotacao.multiply(qtd).setScale(2, RoundingMode.HALF_UP);
		BigDecimal lucroNaoRealizado = valorAtual.subtract(valorInvestido).setScale(2, RoundingMode.HALF_UP);

		BigDecimal percentual = null;
		if (valorInvestido.signum() > 0) {
			percentual = lucroNaoRealizado
					.multiply(BigDecimal.valueOf(100))
					.divide(valorInvestido, 2, RoundingMode.HALF_UP);
		}

		return new AcaoResponse(
				acao.getId(),
				acao.getTicker(),
				acao.getNomeEmpresa(),
				acao.getMercado(),
				acao.getMoeda(),
				acao.getCotacaoAtual(),
				acao.getDataHoraCotacao(),
				corretoraId,
				quantidade,
				precoMedio.setScale(4, RoundingMode.HALF_UP),
				valorInvestido,
				valorAtual,
				lucroNaoRealizado,
				percentual,
				acao.getLucroPrejuizoRealizado().setScale(2, RoundingMode.HALF_UP));
	}
}
