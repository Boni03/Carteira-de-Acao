package com.Carteira_de_Acao.demo.service;

import com.Carteira_de_Acao.demo.dto.AcaoCadastroRequest;
import com.Carteira_de_Acao.demo.dto.AcaoResponse;
import com.Carteira_de_Acao.demo.entity.Acao;
import com.Carteira_de_Acao.demo.entity.Corretora;
import com.Carteira_de_Acao.demo.enums.Mercado;
import com.Carteira_de_Acao.demo.exception.ConflitoException;
import com.Carteira_de_Acao.demo.exception.RecursoNaoEncontradoException;
import com.Carteira_de_Acao.demo.exception.RegraNegocioException;
import com.Carteira_de_Acao.demo.integration.cotacao.CotacaoServiceFacade;
import com.Carteira_de_Acao.demo.integration.cotacao.dto.CotacaoExterna;
import com.Carteira_de_Acao.demo.repository.AcaoRepository;
import com.Carteira_de_Acao.demo.repository.CorretoraRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcaoService {

	private final AcaoRepository acaoRepository;
	private final CorretoraRepository corretoraRepository;
	private final CotacaoServiceFacade cotacaoServiceFacade;

	public AcaoService(
			AcaoRepository acaoRepository,
			CorretoraRepository corretoraRepository,
			CotacaoServiceFacade cotacaoServiceFacade) {
		this.acaoRepository = acaoRepository;
		this.corretoraRepository = corretoraRepository;
		this.cotacaoServiceFacade = cotacaoServiceFacade;
	}

	@Transactional
	public AcaoResponse cadastrar(AcaoCadastroRequest request) {
		String ticker = normalizarTicker(request.ticker(), request.mercado());
		validarMercadoTicker(request.mercado(), ticker);

		if (acaoRepository.existsByTickerIgnoreCase(ticker)) {
			throw new ConflitoException("Ação já cadastrada para o ticker: " + ticker);
		}

		CotacaoExterna cotacao = cotacaoServiceFacade.consultar(request.mercado(), ticker);

		Acao acao = new Acao();
		acao.setTicker(ticker);
		acao.setMercado(request.mercado());
		acao.setNomeEmpresa(cotacao.nomeEmpresa());
		acao.setMoeda(cotacao.moeda());
		acao.setCotacaoAtual(cotacao.preco());
		acao.setDataHoraCotacao(cotacao.dataHoraCotacao());

		if (request.corretoraId() != null) {
			Corretora corretora = corretoraRepository.findById(request.corretoraId())
					.orElseThrow(() -> new RecursoNaoEncontradoException(
							"Corretora não encontrada: id " + request.corretoraId()));
			acao.setCorretoraRelacionada(corretora);
		}

		return AcaoResponse.from(acaoRepository.save(acao));
	}

	@Transactional(readOnly = true)
	public List<AcaoResponse> listar() {
		return acaoRepository.findAll().stream().map(AcaoResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public AcaoResponse buscarPorId(Long id) {
		return acaoRepository.findById(id)
				.map(AcaoResponse::from)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada: id " + id));
	}

	@Transactional(readOnly = true)
	public AcaoResponse buscarPorTicker(String ticker) {
		return acaoRepository.findByTickerIgnoreCase(ticker.trim())
				.map(AcaoResponse::from)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada: ticker " + ticker));
	}

	@Transactional
	public AcaoResponse atualizarCotacao(Long id) {
		Acao acao = acaoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada: id " + id));

		CotacaoExterna cotacao = cotacaoServiceFacade.consultar(acao.getMercado(), acao.getTicker());
		acao.setCotacaoAtual(cotacao.preco());
		acao.setDataHoraCotacao(cotacao.dataHoraCotacao());
		if (cotacao.nomeEmpresa() != null && !cotacao.nomeEmpresa().isBlank()) {
			acao.setNomeEmpresa(cotacao.nomeEmpresa());
		}

		return AcaoResponse.from(acaoRepository.save(acao));
	}

	private static String normalizarTicker(String ticker, Mercado mercado) {
		if (ticker == null || ticker.isBlank()) {
			throw new RegraNegocioException("Ticker é obrigatório");
		}
		return mercado == Mercado.BRASIL
				? ticker.trim().toUpperCase(Locale.ROOT)
				: ticker.trim().toUpperCase(Locale.US);
	}

	private static void validarMercadoTicker(Mercado mercado, String ticker) {
		if (mercado == Mercado.BRASIL && !ticker.matches("^[A-Z]{4}[0-9]{1,2}$")) {
			throw new RegraNegocioException("Ticker brasileiro inválido. Exemplo: PETR4, VALE3");
		}
		if (mercado == Mercado.EUA && !ticker.matches("^[A-Z]{1,5}$")) {
			throw new RegraNegocioException("Ticker americano inválido. Exemplo: AAPL, MSFT");
		}
	}
}
