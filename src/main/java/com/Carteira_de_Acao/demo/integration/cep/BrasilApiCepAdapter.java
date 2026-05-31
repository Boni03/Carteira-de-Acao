package com.Carteira_de_Acao.demo.integration.cep;

import com.Carteira_de_Acao.demo.exception.IntegracaoExternaException;
import com.Carteira_de_Acao.demo.exception.RecursoNaoEncontradoException;
import com.Carteira_de_Acao.demo.integration.cep.dto.DadosCepExterno;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BrasilApiCepAdapter implements CepConsultaPort {

	private final RestClient restClient;

	public BrasilApiCepAdapter(
			RestClient.Builder restClientBuilder,
			@Value("${integracao.brasil-api.base-url}") String baseUrl) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
	}

	@Override
	public DadosCepExterno consultar(String cep) {
		try {
			JsonNode response = restClient.get()
					.uri("/cep/v2/{cep}", cep)
					.retrieve()
					.onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
						if (res.getStatusCode().value() == 404) {
							throw new RecursoNaoEncontradoException("CEP não encontrado: " + cep);
						}
						throw new IntegracaoExternaException("Erro ao consultar CEP na Brasil API");
					})
					.onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
						throw new IntegracaoExternaException("Brasil API indisponível para consulta de CEP");
					})
					.body(JsonNode.class);

			if (response == null) {
				throw new IntegracaoExternaException("Resposta vazia da Brasil API para CEP");
			}

			String cidade = texto(response, "city");
			String uf = texto(response, "state");
			if (response.has("location") && response.get("location").has("coordinates")) {
				// v2 pode trazer city/state em location em alguns casos; campos principais já vêm no root
			}

			return new DadosCepExterno(
					cep,
					texto(response, "street"),
					texto(response, "neighborhood"),
					cidade,
					uf);
		} catch (RecursoNaoEncontradoException | IntegracaoExternaException ex) {
			throw ex;
		} catch (RestClientException ex) {
			throw new IntegracaoExternaException("Falha de comunicação com Brasil API (CEP): " + ex.getMessage(), ex);
		}
	}

	private static String texto(JsonNode node, String campo) {
		if (node.hasNonNull(campo) && !node.get(campo).asText().isBlank()) {
			return node.get(campo).asText();
		}
		return null;
	}
}
