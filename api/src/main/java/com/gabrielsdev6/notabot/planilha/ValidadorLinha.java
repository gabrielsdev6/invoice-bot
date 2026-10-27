package com.gabrielsdev6.notabot.planilha;

import static com.gabrielsdev6.notabot.planilha.ColunasPlanilha.CFOP;
import static com.gabrielsdev6.notabot.planilha.ColunasPlanilha.CLIENTE_DOCUMENTO;
import static com.gabrielsdev6.notabot.planilha.ColunasPlanilha.CLIENTE_NOME;
import static com.gabrielsdev6.notabot.planilha.ColunasPlanilha.CLIENTE_UF;
import static com.gabrielsdev6.notabot.planilha.ColunasPlanilha.DATA_EMISSAO;
import static com.gabrielsdev6.notabot.planilha.ColunasPlanilha.NCM;
import static com.gabrielsdev6.notabot.planilha.ColunasPlanilha.PEDIDO;
import static com.gabrielsdev6.notabot.planilha.ColunasPlanilha.PRODUTO_CODIGO;
import static com.gabrielsdev6.notabot.planilha.ColunasPlanilha.PRODUTO_DESCRICAO;
import static com.gabrielsdev6.notabot.planilha.ColunasPlanilha.QUANTIDADE;
import static com.gabrielsdev6.notabot.planilha.ColunasPlanilha.VALOR_UNITARIO;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.gabrielsdev6.notabot.validacao.DocumentoValidator;

/**
 * Aplica as regras da planilha modelo em cada linha. Uma linha com qualquer erro
 * fica de fora; as outras saem normalizadas (documento só com dígitos, UF em
 * maiúsculas, NCM e CFOP sem pontos).
 */
public class ValidadorLinha {

	private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT",
			"MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

	// STRICT: sem isso, 31/02 vira 28/02 em silêncio
	private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/uuuu")
			.withResolverStyle(ResolverStyle.STRICT);

	private final Clock clock;

	public ValidadorLinha(Clock clock) {
		this.clock = clock;
	}

	public ResultadoValidacao validar(List<LinhaBruta> brutas) {
		List<LinhaPlanilha> validas = new ArrayList<>();
		List<ErroLinha> erros = new ArrayList<>();
		for (LinhaBruta bruta : brutas) {
			List<ErroLinha> errosDaLinha = new ArrayList<>();
			LinhaPlanilha linha = validarLinha(bruta, errosDaLinha);
			if (errosDaLinha.isEmpty()) {
				validas.add(linha);
			}
			else {
				erros.addAll(errosDaLinha);
			}
		}
		return new ResultadoValidacao(validas, erros);
	}

	private LinhaPlanilha validarLinha(LinhaBruta bruta, List<ErroLinha> erros) {
		Checador c = new Checador(bruta, erros);

		String pedido = c.obrigatorio(PEDIDO);
		LocalDate data = c.data(DATA_EMISSAO);
		String nome = c.obrigatorio(CLIENTE_NOME);
		String documento = c.documento(CLIENTE_DOCUMENTO);
		String uf = c.uf(CLIENTE_UF);
		String produtoCodigo = c.obrigatorio(PRODUTO_CODIGO);
		String produtoDescricao = c.obrigatorio(PRODUTO_DESCRICAO);
		String ncm = c.digitos(NCM, 8);
		String cfop = c.digitos(CFOP, 4);
		BigDecimal quantidade = c.positivo(QUANTIDADE, 4); // a NF-e aceita até 4 casas na quantidade
		BigDecimal valorUnitario = c.positivo(VALOR_UNITARIO, 2);

		return new LinhaPlanilha(bruta.numeroLinha(), pedido, data, nome, documento, uf, produtoCodigo,
				produtoDescricao, ncm, cfop, quantidade, valorUnitario);
	}

	/** Lê cada campo de uma linha e anota os erros. Os métodos devolvem null quando o campo é inválido. */
	private final class Checador {

		private final LinhaBruta bruta;
		private final List<ErroLinha> erros;

		Checador(LinhaBruta bruta, List<ErroLinha> erros) {
			this.bruta = bruta;
			this.erros = erros;
		}

		private void erro(String campo, String mensagem) {
			erros.add(new ErroLinha(bruta.numeroLinha(), campo, mensagem));
		}

		String obrigatorio(String campo) {
			String valor = bruta.valor(campo);
			if (valor.isBlank()) {
				erro(campo, "não pode ficar vazio");
				return null;
			}
			return valor;
		}

		LocalDate data(String campo) {
			String valor = obrigatorio(campo);
			if (valor == null) {
				return null;
			}
			LocalDate data;
			try {
				data = valor.contains("/") ? LocalDate.parse(valor, DATA_BR) : LocalDate.parse(valor);
			}
			catch (DateTimeParseException e) {
				erro(campo, "data inválida: \"" + valor + "\" (use dd/mm/aaaa)");
				return null;
			}
			if (data.isAfter(LocalDate.now(clock))) {
				erro(campo, "não pode ser uma data futura");
				return null;
			}
			return data;
		}

		String documento(String campo) {
			String valor = obrigatorio(campo);
			if (valor == null) {
				return null;
			}
			String digitos = DocumentoValidator.somenteDigitos(valor);
			if (digitos.length() == 11) {
				if (!DocumentoValidator.isCpfValido(digitos)) {
					erro(campo, "CPF inválido: " + valor);
				}
			}
			else if (digitos.length() == 14) {
				if (!DocumentoValidator.isCnpjValido(digitos)) {
					erro(campo, "CNPJ inválido: " + valor);
				}
			}
			else {
				erro(campo, "precisa ser um CPF (11 dígitos) ou CNPJ (14 dígitos), mas veio \"" + valor + "\"");
			}
			return digitos;
		}

		String uf(String campo) {
			String valor = obrigatorio(campo);
			if (valor == null) {
				return null;
			}
			String uf = valor.toUpperCase(Locale.ROOT);
			if (!UFS.contains(uf)) {
				erro(campo, "UF inválida: \"" + valor + "\"");
				return null;
			}
			return uf;
		}

		/** Aceita com ou sem pontos (1905.20.90), mas tem que ter exatamente {@code tamanho} dígitos. */
		String digitos(String campo, int tamanho) {
			String valor = obrigatorio(campo);
			if (valor == null) {
				return null;
			}
			String digitos = valor.replace(".", "");
			if (!digitos.matches("\\d{" + tamanho + "}")) {
				erro(campo, "precisa ter " + tamanho + " dígitos, mas veio \"" + valor + "\"");
				return null;
			}
			return digitos;
		}

		BigDecimal positivo(String campo, int casasDecimais) {
			String valor = obrigatorio(campo);
			if (valor == null) {
				return null;
			}
			BigDecimal numero;
			try {
				numero = new BigDecimal(normalizarNumero(valor));
			}
			catch (NumberFormatException e) {
				erro(campo, "não é um número: \"" + valor + "\"");
				return null;
			}
			if (numero.signum() <= 0) {
				erro(campo, "precisa ser maior que zero");
				return null;
			}
			if (numero.stripTrailingZeros().scale() > casasDecimais) {
				erro(campo, "pode ter no máximo " + casasDecimais + " casas decimais");
				return null;
			}
			return numero;
		}

	}

	/** "1.234,56" → "1234.56"; "10,5" → "10.5"; "10.5" continua igual. */
	static String normalizarNumero(String valor) {
		String semEspaco = valor.replace(" ", "");
		if (semEspaco.contains(",")) {
			return semEspaco.replace(".", "").replace(",", ".");
		}
		return semEspaco;
	}

}
