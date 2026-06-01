package com.Carteira_de_Acao.demo.service;

import com.Carteira_de_Acao.demo.dto.OperacaoRequest;
import com.Carteira_de_Acao.demo.dto.OperacaoResponse;
import com.Carteira_de_Acao.demo.entity.Acao;
import com.Carteira_de_Acao.demo.entity.Operacao;
import com.Carteira_de_Acao.demo.enums.TipoOperacao;
import com.Carteira_de_Acao.demo.exception.RecursoNaoEncontradoException;
import com.Carteira_de_Acao.demo.exception.RegraNegocioException;
import com.Carteira_de_Acao.demo.repository.AcaoRepository;
import com.Carteira_de_Acao.demo.repository.OperacaoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperacaoService {

	private final AcaoRepository acaoRepository;
	private final OperacaoRepository operacaoRepository;

	public OperacaoService(AcaoRepository acaoRepository, OperacaoRepository operacaoRepository) {
		this.acaoRepository = acaoRepository;
		this.operacaoRepository = operacaoRepository;
	}

	@Transactional
	public OperacaoResponse comprar(Long acaoId, OperacaoRequest request) {
		Acao acao = buscarAcao(acaoId);
		int quantidade = validarQuantidade(request);
		BigDecimal preco = resolverPreco(request, acao);

		BigDecimal qtdNova = BigDecimal.valueOf(quantidade);
		BigDecimal valorTotal = preco.multiply(qtdNova).setScale(2, RoundingMode.HALF_UP);

		// Recalcula preço médio ponderado
		BigDecimal custoAtual = acao.getPrecoMedio().multiply(BigDecimal.valueOf(acao.getQuantidade()));
		BigDecimal custoCompra = preco.multiply(qtdNova);
		int quantidadeFinal = acao.getQuantidade() + quantidade;
		BigDecimal novoPrecoMedio = custoAtual.add(custoCompra)
				.divide(BigDecimal.valueOf(quantidadeFinal), 4, RoundingMode.HALF_UP);

		acao.setQuantidade(quantidadeFinal);
		acao.setPrecoMedio(novoPrecoMedio);
		acaoRepository.save(acao);

		Operacao op = new Operacao();
		op.setAcao(acao);
		op.setTipo(TipoOperacao.COMPRA);
		op.setQuantidade(quantidade);
		op.setPrecoUnitario(preco.setScale(4, RoundingMode.HALF_UP));
		op.setValorTotal(valorTotal);
		op.setDataHora(LocalDateTime.now());
		op.setPrecoMedioNaOperacao(novoPrecoMedio);

		return OperacaoResponse.from(operacaoRepository.save(op));
	}

	@Transactional
	public OperacaoResponse vender(Long acaoId, OperacaoRequest request) {
		Acao acao = buscarAcao(acaoId);
		int quantidade = validarQuantidade(request);

		if (acao.getQuantidade() <= 0) {
			throw new RegraNegocioException("Não há posição em " + acao.getTicker() + " para vender");
		}
		if (quantidade > acao.getQuantidade()) {
			throw new RegraNegocioException(
					"Quantidade de venda (" + quantidade + ") maior que a posição atual ("
							+ acao.getQuantidade() + ") de " + acao.getTicker());
		}

		BigDecimal preco = resolverPreco(request, acao);
		BigDecimal qtdVenda = BigDecimal.valueOf(quantidade);
		BigDecimal precoMedio = acao.getPrecoMedio();
		BigDecimal valorTotal = preco.multiply(qtdVenda).setScale(2, RoundingMode.HALF_UP);

		// Resultado realizado = (preço de venda - preço médio) * quantidade
		BigDecimal resultado = preco.subtract(precoMedio)
				.multiply(qtdVenda)
				.setScale(2, RoundingMode.HALF_UP);

		int quantidadeFinal = acao.getQuantidade() - quantidade;
		acao.setQuantidade(quantidadeFinal);
		if (quantidadeFinal == 0) {
			acao.setPrecoMedio(BigDecimal.ZERO);
		}
		acao.setLucroPrejuizoRealizado(acao.getLucroPrejuizoRealizado().add(resultado));
		acaoRepository.save(acao);

		Operacao op = new Operacao();
		op.setAcao(acao);
		op.setTipo(TipoOperacao.VENDA);
		op.setQuantidade(quantidade);
		op.setPrecoUnitario(preco.setScale(4, RoundingMode.HALF_UP));
		op.setValorTotal(valorTotal);
		op.setDataHora(LocalDateTime.now());
		op.setResultado(resultado);
		op.setPrecoMedioNaOperacao(precoMedio);

		return OperacaoResponse.from(operacaoRepository.save(op));
	}

	@Transactional(readOnly = true)
	public List<OperacaoResponse> historicoDaAcao(Long acaoId) {
		buscarAcao(acaoId);
		return operacaoRepository.findByAcaoIdOrderByDataHoraDesc(acaoId).stream()
				.map(OperacaoResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<OperacaoResponse> historicoCompleto() {
		return operacaoRepository.findAllByOrderByDataHoraDesc().stream()
				.map(OperacaoResponse::from)
				.toList();
	}

	private Acao buscarAcao(Long acaoId) {
		return acaoRepository.findById(acaoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada: id " + acaoId));
	}

	private int validarQuantidade(OperacaoRequest request) {
		if (request.quantidade() == null || request.quantidade() <= 0) {
			throw new RegraNegocioException("Quantidade deve ser maior que zero");
		}
		return request.quantidade();
	}

	private BigDecimal resolverPreco(OperacaoRequest request, Acao acao) {
		if (request.precoUnitario() != null) {
			if (request.precoUnitario().signum() <= 0) {
				throw new RegraNegocioException("Preço unitário deve ser positivo");
			}
			return request.precoUnitario();
		}
		if (acao.getCotacaoAtual() == null || acao.getCotacaoAtual().signum() <= 0) {
			throw new RegraNegocioException(
					"Cotação atual indisponível para " + acao.getTicker()
							+ ". Informe o preço unitário manualmente.");
		}
		return acao.getCotacaoAtual();
	}
}
