package com.Carteira_de_Acao.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CorretoraCadastroRequest(
		@NotBlank(message = "CNPJ é obrigatório")
		@Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 dígitos numéricos")
		String cnpj,

		@NotBlank(message = "CEP é obrigatório")
		@Pattern(regexp = "\\d{8}", message = "CEP deve conter 8 dígitos numéricos")
		String cep,

		@Size(max = 20)
		String numero,

		@Size(max = 100)
		String complemento
) {
}
