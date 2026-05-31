package com.Carteira_de_Acao.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

	@GetMapping({"/painel", "/app", "/dashboard"})
	public String painel() {
		return "forward:/index.html";
	}
}
