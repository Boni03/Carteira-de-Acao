package com.Carteira_de_Acao.demo.integration.cnpj;

import com.Carteira_de_Acao.demo.exception.IntegracaoExternaException;
import com.Carteira_de_Acao.demo.exception.RecursoNaoEncontradoException;
import com.Carteira_de_Acao.demo.integration.cnpj.dto.DadosCnpjExterno;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BrasilApiCnpjAdapter implements CnpjConsultaPort {

	private final RestClient restClient;

	public BrasilApiCnpjAdapter(
			RestClient.Builder restClientBuilder,
			@Value("${integracao.brasil-api.base-url}") String baseUrl) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
	}

	@Override
	public DadosCnpjExterno consultar(String cnpj) {
		try {
			JsonNode response = restClient.get()
					.uri("/cnpj/v1/{cnpj}", cnpj)
					.retrieve()
					.onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
						if (res.getStatusCode().value() == 404) {
							throw new RecursoNaoEncontradoException("CNPJ não encontrado na Receita Federal: " + cnpj);
						}
						throw new IntegracaoExternaException("Erro ao consultar CNPJ na Brasil API");
					})
					.onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
						throw new IntegracaoExternaException("Brasil API indisponível para consulta de CNPJ");
					})
					.body(JsonNode.class);

			if (response == null) {
				throw new IntegracaoExternaException("Resposta vazia da Brasil API para CNPJ");
			}

			String cnae = null;
			if (response.hasNonNull("cnae_fiscal")) {
				cnae = String.valueOf(response.get("cnae_fiscal").asInt());
			}

			return new DadosCnpjExterno(
					cnpj,
					texto(response, "razao_social"),
					texto(response, "nome_fantasia"),
					email(response),
					telefone(response),
					texto(response, "descricao_situacao_cadastral"),
					cnae);
		} catch (RecursoNaoEncontradoException | IntegracaoExternaException ex) {
			throw ex;
		} catch (RestClientException ex) {
			throw new IntegracaoExternaException("Falha de comunicação com Brasil API (CNPJ): " + ex.getMessage(), ex);
		}
	}

	private static String email(JsonNode node) {
		if (node.hasNonNull("email") && !node.get("email").asText().isBlank()) {
			return node.get("email").asText();
		}
		if (node.has("qsa") && node.get("qsa").isArray() && !node.get("qsa").isEmpty()) {
			JsonNode socio = node.get("qsa").get(0);
			if (socio.hasNonNull("email")) {
				return socio.get("email").asText();
			}
		}
		return null;
	}

	private static String telefone(JsonNode node) {
		String ddd = texto(node, "ddd_telefone_1");
		if (ddd == null || ddd.isBlank()) {
			return null;
		}
		return ddd.replaceAll("\\D", "");
	}

	private static String texto(JsonNode node, String campo) {
		if (node.hasNonNull(campo) && !node.get(campo).asText().isBlank()) {
			return node.get(campo).asText();
		}
		return null;
	}
}
