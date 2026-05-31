package com.Carteira_de_Acao.demo.controller;

import com.Carteira_de_Acao.demo.dto.CorretoraCadastroRequest;
import com.Carteira_de_Acao.demo.dto.CorretoraResponse;
import com.Carteira_de_Acao.demo.service.CorretoraService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/corretoras")
public class CorretoraController {

	private final CorretoraService corretoraService;

	public CorretoraController(CorretoraService corretoraService) {
		this.corretoraService = corretoraService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CorretoraResponse cadastrar(@Valid @RequestBody CorretoraCadastroRequest request) {
		return corretoraService.cadastrar(request);
	}

	@GetMapping
	public List<CorretoraResponse> listar() {
		return corretoraService.listar();
	}

	@GetMapping("/{id}")
	public CorretoraResponse buscarPorId(@PathVariable Long id) {
		return corretoraService.buscarPorId(id);
	}

	@GetMapping("/cnpj/{cnpj}")
	public CorretoraResponse buscarPorCnpj(@PathVariable String cnpj) {
		return corretoraService.buscarPorCnpj(cnpj);
	}
}
