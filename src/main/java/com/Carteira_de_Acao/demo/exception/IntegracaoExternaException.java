package com.Carteira_de_Acao.demo.exception;

public class IntegracaoExternaException extends RuntimeException {

	public IntegracaoExternaException(String message) {
		super(message);
	}

	public IntegracaoExternaException(String message, Throwable cause) {
		super(message, cause);
	}
}
