package com.Carteira_de_Acao.demo.dto;

import com.Carteira_de_Acao.demo.entity.Corretora;
import java.time.LocalDateTime;

public record CorretoraResponse(
		Long id,
		String cnpj,
		String razaoSocial,
		String nomeFantasia,
		String email,
		String telefone,
		String cep,
		String logradouro,
		String numero,
		String complemento,
		String bairro,
		String cidade,
		String uf,
		String situacaoCadastral,
		boolean validadaNaCvm,
		LocalDateTime dataCadastro
) {

	public static CorretoraResponse from(Corretora corretora) {
		return new CorretoraResponse(
				corretora.getId(),
				corretora.getCnpj(),
				corretora.getRazaoSocial(),
				corretora.getNomeFantasia(),
				corretora.getEmail(),
				corretora.getTelefone(),
				corretora.getCep(),
				corretora.getLogradouro(),
				corretora.getNumero(),
				corretora.getComplemento(),
				corretora.getBairro(),
				corretora.getCidade(),
				corretora.getUf(),
				corretora.getSituacaoCadastral(),
				corretora.isValidadaNaCvm(),
				corretora.getDataCadastro());
	}
}
