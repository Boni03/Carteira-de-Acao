package com.Carteira_de_Acao.demo.controller;

import com.Carteira_de_Acao.demo.dto.AcaoCadastroRequest;
import com.Carteira_de_Acao.demo.dto.AcaoResponse;
import com.Carteira_de_Acao.demo.service.AcaoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/acoes")
public class AcaoController {

	private final AcaoService acaoService;

	public AcaoController(AcaoService acaoService) {
		this.acaoService = acaoService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AcaoResponse cadastrar(@Valid @RequestBody AcaoCadastroRequest request) {
		return acaoService.cadastrar(request);
	}

	@GetMapping
	public List<AcaoResponse> listar() {
		return acaoService.listar();
	}

	@GetMapping("/{id}")
	public AcaoResponse buscarPorId(@PathVariable Long id) {
		return acaoService.buscarPorId(id);
	}

	@GetMapping("/ticker/{ticker}")
	public AcaoResponse buscarPorTicker(@PathVariable String ticker) {
		return acaoService.buscarPorTicker(ticker);
	}

	@PutMapping("/{id}/atualizar-cotacao")
	public AcaoResponse atualizarCotacao(@PathVariable Long id) {
		return acaoService.atualizarCotacao(id);
	}
}
