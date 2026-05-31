package com.Carteira_de_Acao.demo.integration.cotacao;

import com.Carteira_de_Acao.demo.enums.Mercado;
import com.Carteira_de_Acao.demo.exception.IntegracaoExternaException;
import com.Carteira_de_Acao.demo.exception.LimiteRequisicoesException;
import com.Carteira_de_Acao.demo.exception.RecursoNaoEncontradoException;
import com.Carteira_de_Acao.demo.integration.cotacao.dto.CotacaoExterna;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BrapiCotacaoAdapter implements CotacaoPort {

	private final RestClient restClient;
	private final String token;

	public BrapiCotacaoAdapter(
			RestClient.Builder restClientBuilder,
			@Value("${integracao.brapi.base-url}") String baseUrl,
			@Value("${integracao.brapi.token:}") String token) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
		this.token = token;
	}

	@Override
	public boolean suporta(Mercado mercado) {
		return mercado == Mercado.BRASIL;
	}

	@Override
	public CotacaoExterna consultar(String ticker) {
		if (!StringUtils.hasText(token)) {
			throw new IntegracaoExternaException("Token da Brapi não configurado (integracao.brapi.token)");
		}
		String tickerNormalizado = ticker.trim().toUpperCase();
		try {
			JsonNode response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/quote/{ticker}")
							.queryParam("token", token)
							.build(tickerNormalizado))
					.retrieve()
					.onStatus(status -> status.value() == 429, (req, res) -> {
						throw new LimiteRequisicoesException("Limite de requisições excedido na Brapi");
					})
					.onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
						if (res.getStatusCode().value() == 404) {
							throw new RecursoNaoEncontradoException("Ticker brasileiro não encontrado: " + tickerNormalizado);
						}
						throw new IntegracaoExternaException("Erro ao consultar cotação na Brapi");
					})
					.onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
						throw new IntegracaoExternaException("Brapi indisponível");
					})
					.body(JsonNode.class);

			if (response == null || !response.has("results") || !response.get("results").isArray()
					|| response.get("results").isEmpty()) {
				throw new RecursoNaoEncontradoException("Ticker brasileiro não encontrado: " + tickerNormalizado);
			}

			JsonNode ativo = response.get("results").get(0);
			BigDecimal preco = null;
			if (ativo.hasNonNull("regularMarketPrice")) {
				preco = BigDecimal.valueOf(ativo.get("regularMarketPrice").asDouble());
			}
			if (preco == null) {
				throw new RecursoNaoEncontradoException("Cotação indisponível para o ticker: " + tickerNormalizado);
			}

			String moeda = ativo.hasNonNull("currency") ? ativo.get("currency").asText() : "BRL";
			String nome = ativo.hasNonNull("longName") ? ativo.get("longName").asText()
					: (ativo.hasNonNull("shortName") ? ativo.get("shortName").asText() : null);

			long epoch = ativo.hasNonNull("regularMarketTime")
					? ativo.get("regularMarketTime").asLong()
					: System.currentTimeMillis() / 1000;

			return new CotacaoExterna(
					tickerNormalizado,
					nome,
					moeda,
					preco,
					LocalDateTime.ofInstant(
							java.time.Instant.ofEpochSecond(epoch),
							ZoneId.of("America/Sao_Paulo")));
		} catch (RecursoNaoEncontradoException | IntegracaoExternaException | LimiteRequisicoesException ex) {
			throw ex;
		} catch (RestClientException ex) {
			throw new IntegracaoExternaException("Falha de comunicação com Brapi: " + ex.getMessage(), ex);
		}
	}
}
