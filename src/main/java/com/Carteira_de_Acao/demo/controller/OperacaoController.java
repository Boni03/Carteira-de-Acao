package com.Carteira_de_Acao.demo.controller;

import com.Carteira_de_Acao.demo.dto.OperacaoRequest;
import com.Carteira_de_Acao.demo.dto.OperacaoResponse;
import com.Carteira_de_Acao.demo.service.OperacaoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OperacaoController {

	private final OperacaoService operacaoService;

	public OperacaoController(OperacaoService operacaoService) {
		this.operacaoService = operacaoService;
	}

	@PostMapping("/acoes/{id}/comprar")
	@ResponseStatus(HttpStatus.CREATED)
	public OperacaoResponse comprar(@PathVariable Long id, @Valid @RequestBody OperacaoRequest request) {
		return operacaoService.comprar(id, request);
	}

	@PostMapping("/acoes/{id}/vender")
	@ResponseStatus(HttpStatus.CREATED)
	public OperacaoResponse vender(@PathVariable Long id, @Valid @RequestBody OperacaoRequest request) {
		return operacaoService.vender(id, request);
	}

	@GetMapping("/acoes/{id}/operacoes")
	public List<OperacaoResponse> historicoDaAcao(@PathVariable Long id) {
		return operacaoService.historicoDaAcao(id);
	}

	@GetMapping("/operacoes")
	public List<OperacaoResponse> historicoCompleto() {
		return operacaoService.historicoCompleto();
	}
}
