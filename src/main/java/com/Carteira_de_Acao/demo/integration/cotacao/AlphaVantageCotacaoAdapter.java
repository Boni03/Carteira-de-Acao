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
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AlphaVantageCotacaoAdapter implements CotacaoPort {

	private static final DateTimeFormatter DATA_ALPHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final RestClient restClient;
	private final String apiKey;

	public AlphaVantageCotacaoAdapter(
			RestClient.Builder restClientBuilder,
			@Value("${integracao.alphavantage.base-url}") String baseUrl,
			@Value("${integracao.alphavantage.api-key:}") String apiKey) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
		this.apiKey = apiKey;
	}

	@Override
	public boolean suporta(Mercado mercado) {
		return mercado == Mercado.EUA;
	}

	@Override
	public CotacaoExterna consultar(String ticker) {
		if (!StringUtils.hasText(apiKey)) {
			throw new IntegracaoExternaException("API Key da Alpha Vantage não configurada");
		}
		String tickerNormalizado = ticker.trim().toUpperCase(Locale.US);
		try {
			JsonNode response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/query")
							.queryParam("function", "GLOBAL_QUOTE")
							.queryParam("symbol", tickerNormalizado)
							.queryParam("apikey", apiKey)
							.build())
					.retrieve()
					.body(JsonNode.class);

			if (response == null) {
				throw new IntegracaoExternaException("Resposta vazia da Alpha Vantage");
			}

			if (response.has("Note")) {
				throw new LimiteRequisicoesException(response.get("Note").asText());
			}
			if (response.has("Information")) {
				throw new LimiteRequisicoesException(response.get("Information").asText());
			}
			if (response.has("Error Message")) {
				throw new RecursoNaoEncontradoException(response.get("Error Message").asText());
			}

			JsonNode quote = response.get("Global Quote");
			if (quote == null || quote.isEmpty() || !quote.hasNonNull("05. price")) {
				throw new RecursoNaoEncontradoException("Ticker americano não encontrado: " + tickerNormalizado);
			}

			String precoStr = quote.get("05. price").asText();
			if (precoStr.isBlank() || "0.0000".equals(precoStr)) {
				throw new RecursoNaoEncontradoException("Ticker americano não encontrado: " + tickerNormalizado);
			}

			BigDecimal preco = new BigDecimal(precoStr);
			LocalDateTime dataHora = LocalDateTime.now(ZoneId.of("America/New_York"));
			if (quote.hasNonNull("07. latest trading day")) {
				String dia = quote.get("07. latest trading day").asText();
				dataHora = LocalDateTime.parse(dia + " 16:00:00", DATA_ALPHA);
			}

			return new CotacaoExterna(
					tickerNormalizado,
					null,
					"USD",
					preco,
					dataHora);
		} catch (RecursoNaoEncontradoException | IntegracaoExternaException | LimiteRequisicoesException ex) {
			throw ex;
		} catch (RestClientException ex) {
			throw new IntegracaoExternaException("Falha de comunicação com Alpha Vantage: " + ex.getMessage(), ex);
		}
	}
}
