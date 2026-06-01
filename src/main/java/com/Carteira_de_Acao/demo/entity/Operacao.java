package com.Carteira_de_Acao.demo.entity;

import com.Carteira_de_Acao.demo.enums.TipoOperacao;
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
@Table(name = "operacoes")
public class Operacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "acao_id", nullable = false)
	private Acao acao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TipoOperacao tipo;

	@Column(nullable = false)
	private Integer quantidade;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal precoUnitario;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal valorTotal;

	@Column(nullable = false)
	private LocalDateTime dataHora = LocalDateTime.now();

	/** Preenchido apenas em vendas: lucro (+) ou prejuízo (-) realizado. */
	@Column(precision = 19, scale = 2)
	private BigDecimal resultado;

	/** Preço médio da posição no momento da venda (base de custo). */
	@Column(precision = 19, scale = 4)
	private BigDecimal precoMedioNaOperacao;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Acao getAcao() {
		return acao;
	}

	public void setAcao(Acao acao) {
		this.acao = acao;
	}

	public TipoOperacao getTipo() {
		return tipo;
	}

	public void setTipo(TipoOperacao tipo) {
		this.tipo = tipo;
	}

	public Integer getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(Integer quantidade) {
		this.quantidade = quantidade;
	}

	public BigDecimal getPrecoUnitario() {
		return precoUnitario;
	}

	public void setPrecoUnitario(BigDecimal precoUnitario) {
		this.precoUnitario = precoUnitario;
	}

	public BigDecimal getValorTotal() {
		return valorTotal;
	}

	public void setValorTotal(BigDecimal valorTotal) {
		this.valorTotal = valorTotal;
	}

	public LocalDateTime getDataHora() {
		return dataHora;
	}

	public void setDataHora(LocalDateTime dataHora) {
		this.dataHora = dataHora;
	}

	public BigDecimal getResultado() {
		return resultado;
	}

	public void setResultado(BigDecimal resultado) {
		this.resultado = resultado;
	}

	public BigDecimal getPrecoMedioNaOperacao() {
		return precoMedioNaOperacao;
	}

	public void setPrecoMedioNaOperacao(BigDecimal precoMedioNaOperacao) {
		this.precoMedioNaOperacao = precoMedioNaOperacao;
	}
}
