package com.Carteira_de_Acao.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Requisição de compra ou venda. O preço unitário é opcional: quando ausente,
 * usa-se a cotação atual da ação.
 */
public record OperacaoRequest(
		@NotNull(message = "Quantidade é obrigatória")
		@Positive(message = "Quantidade deve ser maior que zero")
		Integer quantidade,

		@Positive(message = "Preço unitário, quando informado, deve ser positivo")
		BigDecimal precoUnitario
) {
}
