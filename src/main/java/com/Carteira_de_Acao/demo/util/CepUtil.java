package com.Carteira_de_Acao.demo.util;

public final class CepUtil {

	private CepUtil() {
	}

	public static String apenasDigitos(String cep) {
		if (cep == null) {
			return "";
		}
		return cep.replaceAll("\\D", "");
	}

	public static boolean formatoValido(String cep) {
		return apenasDigitos(cep).length() == 8;
	}
}
