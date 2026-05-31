package com.Carteira_de_Acao.demo.integration.cep;

import com.Carteira_de_Acao.demo.integration.cep.dto.DadosCepExterno;

public interface CepConsultaPort {

	DadosCepExterno consultar(String cep);
}
