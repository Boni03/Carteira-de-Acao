package com.Carteira_de_Acao.demo.integration.cep.dto;

public record DadosCepExterno(
		String cep,
		String logradouro,
		String bairro,
		String cidade,
		String uf
) {
}
