package com.Carteira_de_Acao.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponse(
		LocalDateTime timestamp,
		int status,
		String erro,
		String mensagem,
		String path,
		List<String> detalhes
) {
}
