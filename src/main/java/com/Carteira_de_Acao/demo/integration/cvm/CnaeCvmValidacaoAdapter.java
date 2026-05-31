package com.Carteira_de_Acao.demo.integration.cvm;

import com.Carteira_de_Acao.demo.integration.cnpj.dto.DadosCnpjExterno;
import org.springframework.stereotype.Component;

/**
 * Valida compatibilidade com o mercado financeiro via CNAE da Receita Federal.
 * CNAEs do grupo 6612-x referem-se a corretoras e atividades correlatas.
 * Fonte pública equivalente à exigência de validação CVM do trabalho acadêmico.
 */
@Component
public class CnaeCvmValidacaoAdapter implements CvmValidacaoPort {

	private static final String[] CNAES_FINANCEIROS_PREFIXOS = {
			"6612601", "6612602", "6612603", "6612604", "6612605",
			"6612301", "6612302", "6611801", "6611802", "6611803",
			"6619302", "6619303", "6619304", "6422100", "6423900"
	};

	@Override
	public boolean instituicaoValidaNoMercado(DadosCnpjExterno dadosCnpj) {
		if (dadosCnpj.cnaePrincipal() == null) {
			return false;
		}
		String cnae = dadosCnpj.cnaePrincipal().replaceAll("\\D", "");
		for (String prefixo : CNAES_FINANCEIROS_PREFIXOS) {
			if (cnae.startsWith(prefixo) || cnae.equals(prefixo)) {
				return true;
			}
		}
		return cnae.startsWith("6612") || cnae.startsWith("6611") || cnae.startsWith("6619");
	}
}
