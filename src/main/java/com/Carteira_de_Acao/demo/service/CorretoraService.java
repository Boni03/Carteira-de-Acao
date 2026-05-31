package com.Carteira_de_Acao.demo.service;

import com.Carteira_de_Acao.demo.dto.CorretoraCadastroRequest;
import com.Carteira_de_Acao.demo.dto.CorretoraResponse;
import com.Carteira_de_Acao.demo.entity.Corretora;
import com.Carteira_de_Acao.demo.exception.ConflitoException;
import com.Carteira_de_Acao.demo.exception.RecursoNaoEncontradoException;
import com.Carteira_de_Acao.demo.exception.RegraNegocioException;
import com.Carteira_de_Acao.demo.integration.cep.CepConsultaPort;
import com.Carteira_de_Acao.demo.integration.cep.dto.DadosCepExterno;
import com.Carteira_de_Acao.demo.integration.cnpj.CnpjConsultaPort;
import com.Carteira_de_Acao.demo.integration.cnpj.dto.DadosCnpjExterno;
import com.Carteira_de_Acao.demo.integration.cvm.CvmValidacaoPort;
import com.Carteira_de_Acao.demo.repository.CorretoraRepository;
import com.Carteira_de_Acao.demo.util.CepUtil;
import com.Carteira_de_Acao.demo.util.CnpjUtil;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorretoraService {

	private final CorretoraRepository corretoraRepository;
	private final CnpjConsultaPort cnpjConsultaPort;
	private final CepConsultaPort cepConsultaPort;
	private final CvmValidacaoPort cvmValidacaoPort;

	public CorretoraService(
			CorretoraRepository corretoraRepository,
			CnpjConsultaPort cnpjConsultaPort,
			CepConsultaPort cepConsultaPort,
			CvmValidacaoPort cvmValidacaoPort) {
		this.corretoraRepository = corretoraRepository;
		this.cnpjConsultaPort = cnpjConsultaPort;
		this.cepConsultaPort = cepConsultaPort;
		this.cvmValidacaoPort = cvmValidacaoPort;
	}

	@Transactional
	public CorretoraResponse cadastrar(CorretoraCadastroRequest request) {
		String cnpj = CnpjUtil.apenasDigitos(request.cnpj());
		String cep = CepUtil.apenasDigitos(request.cep());

		if (!CnpjUtil.formatoValido(cnpj)) {
			throw new RegraNegocioException("CNPJ inválido no formato");
		}
		if (!CepUtil.formatoValido(cep)) {
			throw new RegraNegocioException("CEP inválido no formato");
		}
		if (corretoraRepository.existsByCnpj(cnpj)) {
			throw new ConflitoException("Corretora já cadastrada para o CNPJ: " + cnpj);
		}

		DadosCnpjExterno dadosCnpj = cnpjConsultaPort.consultar(cnpj);
		if (dadosCnpj.razaoSocial() == null || dadosCnpj.razaoSocial().isBlank()) {
			throw new RegraNegocioException("CNPJ sem razão social na base consultada");
		}
		if (dadosCnpj.situacaoCadastral() != null
				&& !dadosCnpj.situacaoCadastral().equalsIgnoreCase("ATIVA")) {
			throw new RegraNegocioException(
					"Instituição com situação cadastral não ativa: " + dadosCnpj.situacaoCadastral());
		}

		boolean validadaCvm = cvmValidacaoPort.instituicaoValidaNoMercado(dadosCnpj);
		if (!validadaCvm) {
			throw new RegraNegocioException(
					"Instituição não identificada como participante válida do mercado financeiro (CNAE/CVM)");
		}

		DadosCepExterno dadosCep = cepConsultaPort.consultar(cep);

		Corretora corretora = new Corretora();
		corretora.setCnpj(cnpj);
		corretora.setRazaoSocial(dadosCnpj.razaoSocial());
		corretora.setNomeFantasia(dadosCnpj.nomeFantasia());
		corretora.setEmail(dadosCnpj.email());
		corretora.setTelefone(dadosCnpj.telefone());
		corretora.setCep(cep);
		corretora.setLogradouro(dadosCep.logradouro());
		corretora.setNumero(request.numero());
		corretora.setComplemento(request.complemento());
		corretora.setBairro(dadosCep.bairro());
		corretora.setCidade(dadosCep.cidade());
		corretora.setUf(dadosCep.uf());
		corretora.setSituacaoCadastral(dadosCnpj.situacaoCadastral());
		corretora.setValidadaNaCvm(true);

		return CorretoraResponse.from(corretoraRepository.save(corretora));
	}

	@Transactional(readOnly = true)
	public List<CorretoraResponse> listar() {
		return corretoraRepository.findAll().stream().map(CorretoraResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public CorretoraResponse buscarPorId(Long id) {
		return corretoraRepository.findById(id)
				.map(CorretoraResponse::from)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada: id " + id));
	}

	@Transactional(readOnly = true)
	public CorretoraResponse buscarPorCnpj(String cnpj) {
		String cnpjDigitos = CnpjUtil.apenasDigitos(cnpj);
		return corretoraRepository.findByCnpj(cnpjDigitos)
				.map(CorretoraResponse::from)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada: CNPJ " + cnpjDigitos));
	}
}
