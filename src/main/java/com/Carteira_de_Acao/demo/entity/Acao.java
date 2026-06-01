package com.Carteira_de_Acao.demo.entity;

import com.Carteira_de_Acao.demo.enums.Mercado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "acoes")
public class Acao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String ticker;

	private String nomeEmpresa;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Mercado mercado;

	@Column(nullable = false, length = 5)
	private String moeda;

	private BigDecimal cotacaoAtual;

	private LocalDateTime dataHoraCotacao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "corretora_id")
	private Corretora corretoraRelacionada;

	/** Quantidade de papéis atualmente em carteira. */
	@Column(nullable = false)
	private Integer quantidade = 0;

	/** Preço médio de compra da posição atual (base de custo). */
	@Column(precision = 19, scale = 4)
	private BigDecimal precoMedio = BigDecimal.ZERO;

	/** Lucro/prejuízo já realizado acumulado (somatório das vendas). */
	@Column(precision = 19, scale = 2)
	private BigDecimal lucroPrejuizoRealizado = BigDecimal.ZERO;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public String getNomeEmpresa() {
		return nomeEmpresa;
	}

	public void setNomeEmpresa(String nomeEmpresa) {
		this.nomeEmpresa = nomeEmpresa;
	}

	public Mercado getMercado() {
		return mercado;
	}

	public void setMercado(Mercado mercado) {
		this.mercado = mercado;
	}

	public String getMoeda() {
		return moeda;
	}

	public void setMoeda(String moeda) {
		this.moeda = moeda;
	}

	public BigDecimal getCotacaoAtual() {
		return cotacaoAtual;
	}

	public void setCotacaoAtual(BigDecimal cotacaoAtual) {
		this.cotacaoAtual = cotacaoAtual;
	}

	public LocalDateTime getDataHoraCotacao() {
		return dataHoraCotacao;
	}

	public void setDataHoraCotacao(LocalDateTime dataHoraCotacao) {
		this.dataHoraCotacao = dataHoraCotacao;
	}

	public Corretora getCorretoraRelacionada() {
		return corretoraRelacionada;
	}

	public void setCorretoraRelacionada(Corretora corretoraRelacionada) {
		this.corretoraRelacionada = corretoraRelacionada;
	}

	public Integer getQuantidade() {
		return quantidade == null ? 0 : quantidade;
	}

	public void setQuantidade(Integer quantidade) {
		this.quantidade = quantidade;
	}

	public BigDecimal getPrecoMedio() {
		return precoMedio == null ? BigDecimal.ZERO : precoMedio;
	}

	public void setPrecoMedio(BigDecimal precoMedio) {
		this.precoMedio = precoMedio;
	}

	public BigDecimal getLucroPrejuizoRealizado() {
		return lucroPrejuizoRealizado == null ? BigDecimal.ZERO : lucroPrejuizoRealizado;
	}

	public void setLucroPrejuizoRealizado(BigDecimal lucroPrejuizoRealizado) {
		this.lucroPrejuizoRealizado = lucroPrejuizoRealizado;
	}
}
