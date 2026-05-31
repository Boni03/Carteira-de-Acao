package com.Carteira_de_Acao.demo.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

	@GetMapping("/")
	public Map<String, Object> inicio() {
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("aplicacao", "Carteira de Ação API");
		info.put("documentacao", "/swagger-ui/index.html");
		info.put("endpoints", Map.of(
				"corretoras", "/corretoras",
				"acoes", "/acoes"));
		return info;
	}
}
