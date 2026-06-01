package com.Carteira_de_Acao.demo.dto;

import com.Carteira_de_Acao.demo.entity.Operacao;
import com.Carteira_de_Acao.demo.enums.TipoOperacao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record OperacaoResponse(
		Long id,
		Long acaoId,
		String ticker,
		String moeda,
		TipoOperacao tipo,
		Integer quantidade,
		BigDecimal precoUnitario,
		BigDecimal valorTotal,
		LocalDateTime dataHora,
		BigDecimal resultado,
		BigDecimal resultadoPercentual
) {

	public static OperacaoResponse from(Operacao op) {
		BigDecimal percentual = null;
		if (op.getResultado() != null
				&& op.getPrecoMedioNaOperacao() != null
				&& op.getPrecoMedioNaOperacao().signum() > 0
				&& op.getQuantidade() != null) {
			BigDecimal custoBase = op.getPrecoMedioNaOperacao()
					.multiply(BigDecimal.valueOf(op.getQuantidade()));
			if (custoBase.signum() > 0) {
				percentual = op.getResultado()
						.multiply(BigDecimal.valueOf(100))
						.divide(custoBase, 2, RoundingMode.HALF_UP);
			}
		}
		return new OperacaoResponse(
				op.getId(),
				op.getAcao().getId(),
				op.getAcao().getTicker(),
				op.getAcao().getMoeda(),
				op.getTipo(),
				op.getQuantidade(),
				op.getPrecoUnitario(),
				op.getValorTotal(),
				op.getDataHora(),
				op.getResultado(),
				percentual);
	}
}
