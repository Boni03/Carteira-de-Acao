package com.Carteira_de_Acao.demo.exception;

import com.Carteira_de_Acao.demo.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ResponseEntity<ErroResponse> handleNaoEncontrado(
			RecursoNaoEncontradoException ex, HttpServletRequest request) {
		return resposta(HttpStatus.NOT_FOUND, "Não encontrado", ex.getMessage(), request, null);
	}

	@ExceptionHandler(ConflitoException.class)
	public ResponseEntity<ErroResponse> handleConflito(ConflitoException ex, HttpServletRequest request) {
		return resposta(HttpStatus.CONFLICT, "Conflito", ex.getMessage(), request, null);
	}

	@ExceptionHandler(RegraNegocioException.class)
	public ResponseEntity<ErroResponse> handleRegraNegocio(RegraNegocioException ex, HttpServletRequest request) {
		return resposta(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio", ex.getMessage(), request, null);
	}

	@ExceptionHandler(LimiteRequisicoesException.class)
	public ResponseEntity<ErroResponse> handleLimite(LimiteRequisicoesException ex, HttpServletRequest request) {
		return resposta(HttpStatus.TOO_MANY_REQUESTS, "Limite excedido", ex.getMessage(), request, null);
	}

	@ExceptionHandler(IntegracaoExternaException.class)
	public ResponseEntity<ErroResponse> handleIntegracao(IntegracaoExternaException ex, HttpServletRequest request) {
		return resposta(HttpStatus.BAD_GATEWAY, "Integração externa", ex.getMessage(), request, null);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErroResponse> handleRotaInexistente(
			NoResourceFoundException ex, HttpServletRequest request) {
		return resposta(HttpStatus.NOT_FOUND, "Não encontrado", "Rota não encontrada", request, null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponse> handleValidacao(
			MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.toList();
		return resposta(HttpStatus.BAD_REQUEST, "Validação", "Dados de entrada inválidos", request, detalhes);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErroResponse> handleGenerico(Exception ex, HttpServletRequest request) {
		return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", ex.getMessage(), request, null);
	}

	private ResponseEntity<ErroResponse> resposta(
			HttpStatus status,
			String erro,
			String mensagem,
			HttpServletRequest request,
			List<String> detalhes) {
		ErroResponse body = new ErroResponse(
				LocalDateTime.now(),
				status.value(),
				erro,
				mensagem,
				request.getRequestURI(),
				detalhes);
		return ResponseEntity.status(status).body(body);
	}
}
