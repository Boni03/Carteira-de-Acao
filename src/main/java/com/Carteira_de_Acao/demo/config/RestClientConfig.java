package com.Carteira_de_Acao.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	public RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}
}
