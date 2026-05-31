package com.Carteira_de_Acao.demo.dto;

import com.Carteira_de_Acao.demo.enums.Mercado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AcaoCadastroRequest(
		@NotBlank(message = "Ticker é obrigatório")
		String ticker,

		@NotNull(message = "Mercado é obrigatório (BRASIL ou EUA)")
		Mercado mercado,

		Long corretoraId
) {
}
