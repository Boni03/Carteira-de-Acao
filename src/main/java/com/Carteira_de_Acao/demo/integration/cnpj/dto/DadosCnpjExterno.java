package com.Carteira_de_Acao.demo.integration.cnpj.dto;

public record DadosCnpjExterno(
		String cnpj,
		String razaoSocial,
		String nomeFantasia,
		String email,
		String telefone,
		String situacaoCadastral,
		String cnaePrincipal
) {
}
