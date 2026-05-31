package com.Carteira_de_Acao.demo.util;

public final class CnpjUtil {

	private CnpjUtil() {
	}

	public static String apenasDigitos(String cnpj) {
		if (cnpj == null) {
			return "";
		}
		return cnpj.replaceAll("\\D", "");
	}

	public static boolean formatoValido(String cnpj) {
		String digitos = apenasDigitos(cnpj);
		if (digitos.length() != 14 || digitos.chars().distinct().count() == 1) {
			return false;
		}
		return validarDigitosVerificadores(digitos);
	}

	private static boolean validarDigitosVerificadores(String cnpj) {
		int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
		int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

		int soma = 0;
		for (int i = 0; i < 12; i++) {
			soma += Character.getNumericValue(cnpj.charAt(i)) * pesos1[i];
		}
		int digito1 = soma % 11 < 2 ? 0 : 11 - (soma % 11);
		if (digito1 != Character.getNumericValue(cnpj.charAt(12))) {
			return false;
		}

		soma = 0;
		for (int i = 0; i < 13; i++) {
			soma += Character.getNumericValue(cnpj.charAt(i)) * pesos2[i];
		}
		int digito2 = soma % 11 < 2 ? 0 : 11 - (soma % 11);
		return digito2 == Character.getNumericValue(cnpj.charAt(13));
	}
}
