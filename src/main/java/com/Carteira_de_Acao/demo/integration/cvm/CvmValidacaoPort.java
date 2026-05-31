package com.Carteira_de_Acao.demo.integration.cvm;

import com.Carteira_de_Acao.demo.integration.cnpj.dto.DadosCnpjExterno;

public interface CvmValidacaoPort {

	boolean instituicaoValidaNoMercado(DadosCnpjExterno dadosCnpj);
}
