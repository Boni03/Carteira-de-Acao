package com.Carteira_de_Acao.demo.integration.cnpj;

import com.Carteira_de_Acao.demo.integration.cnpj.dto.DadosCnpjExterno;

public interface CnpjConsultaPort {

	DadosCnpjExterno consultar(String cnpj);
}
