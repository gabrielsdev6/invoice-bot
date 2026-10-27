package com.gabrielsdev6.notabot.planilha;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ValidadorLinhaTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

	private final ValidadorLinha validador = new ValidadorLinha(
			Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), SAO_PAULO));

	private static Map<String, String> valida() {
		Map<String, String> v = new HashMap<>();
		v.put(ColunasPlanilha.PEDIDO, "1001");
		v.put(ColunasPlanilha.DATA_EMISSAO, "2026-10-20");
		v.put(ColunasPlanilha.CLIENTE_NOME, "Padaria Pão Quente");
		v.put(ColunasPlanilha.CLIENTE_DOCUMENTO, "11.222.333/0001-81");
		v.put(ColunasPlanilha.CLIENTE_UF, "sp");
		v.put(ColunasPlanilha.PRODUTO_CODIGO, "P-01");
		v.put(ColunasPlanilha.PRODUTO_DESCRICAO, "Pão francês");
		v.put(ColunasPlanilha.NCM, "1905.20.90");
		v.put(ColunasPlanilha.CFOP, "5.102");
		v.put(ColunasPlanilha.QUANTIDADE, "2,5");
		v.put(ColunasPlanilha.VALOR_UNITARIO, "18.90");
		return v;
	}

	private ResultadoValidacao validarCom(String coluna, String valor) {
		Map<String, String> v = valida();
		if (valor == null) {
			v.remove(coluna);
		}
		else {
			v.put(coluna, valor);
		}
		return validador.validar(List.of(new LinhaBruta(7, v)));
	}

	@Test
	void linhaValidaSaiNormalizada() {
		ResultadoValidacao r = validador.validar(List.of(new LinhaBruta(2, valida())));

		assertThat(r.erros()).isEmpty();
		LinhaPlanilha l = r.linhas().get(0);
		assertThat(l.numeroLinha()).isEqualTo(2);
		assertThat(l.dataEmissao()).isEqualTo(LocalDate.of(2026, 10, 20));
		assertThat(l.clienteDocumento()).isEqualTo("11222333000181");
		assertThat(l.clienteUf()).isEqualTo("SP");
		assertThat(l.ncm()).isEqualTo("19052090");
		assertThat(l.cfop()).isEqualTo("5102");
		assertThat(l.quantidade()).isEqualByComparingTo("2.5");
		assertThat(l.valorUnitario()).isEqualByComparingTo("18.90");
	}

	@Test
	void aceitaDataNoFormatoBrasileiroECpf() {
		Map<String, String> v = valida();
		v.put(ColunasPlanilha.DATA_EMISSAO, "27/10/2026");
		v.put(ColunasPlanilha.CLIENTE_DOCUMENTO, "529.982.247-25");

		ResultadoValidacao r = validador.validar(List.of(new LinhaBruta(2, v)));

		assertThat(r.erros()).isEmpty();
		assertThat(r.linhas().get(0).dataEmissao()).isEqualTo(LocalDate.of(2026, 10, 27));
		assertThat(r.linhas().get(0).clienteDocumento()).isEqualTo("52998224725");
	}

	@ParameterizedTest(name = "{0} = \"{1}\" → {2}")
	@CsvSource(delimiter = '|', nullValues = "NULO", value = {
			"pedido            | NULO               | vazio",
			"cliente_nome      | '   '              | vazio",
			"data_emissao      | 31/02/2026         | data inválida",
			"data_emissao      | 2026-02-31         | data inválida",
			"data_emissao      | 2026-10-28         | futura",
			"cliente_documento | 529.982.247-24     | CPF inválido",
			"cliente_documento | 11.222.333/0001-80 | CNPJ inválido",
			"cliente_documento | 123                | CPF (11 dígitos) ou CNPJ (14 dígitos)",
			"cliente_uf        | XX                 | UF inválida",
			"ncm               | 1905209            | 8 dígitos",
			"ncm               | abcdefgh           | 8 dígitos",
			"cfop              | 51020              | 4 dígitos",
			"quantidade        | dois               | não é um número",
			"quantidade        | 0                  | maior que zero",
			"quantidade        | 1,23456            | 4 casas decimais",
			"valor_unitario    | -5                 | maior que zero",
			"valor_unitario    | 10,555             | 2 casas decimais" })
	void cadaRegraGeraUmErroComOCampoEALinha(String coluna, String valor, String trechoDaMensagem) {
		ResultadoValidacao r = validarCom(coluna, valor);

		assertThat(r.linhas()).isEmpty();
		assertThat(r.erros()).hasSize(1);
		ErroLinha erro = r.erros().get(0);
		assertThat(erro.linha()).isEqualTo(7);
		assertThat(erro.campo()).isEqualTo(coluna);
		assertThat(erro.mensagem()).contains(trechoDaMensagem);
	}

	@Test
	void linhaComVariosProblemasMostraTodos() {
		Map<String, String> v = valida();
		v.put(ColunasPlanilha.CLIENTE_UF, "ZZ");
		v.put(ColunasPlanilha.NCM, "1");
		v.remove(ColunasPlanilha.PRODUTO_DESCRICAO);

		ResultadoValidacao r = validador.validar(List.of(new LinhaBruta(3, v)));

		assertThat(r.erros()).extracting(ErroLinha::campo)
				.containsExactlyInAnyOrder("cliente_uf", "ncm", "produto_descricao");
	}

	@Test
	void linhasBoasPassamMesmoComOutrasErradas() {
		Map<String, String> ruim = valida();
		ruim.put(ColunasPlanilha.QUANTIDADE, "0");

		ResultadoValidacao r = validador.validar(List.of(new LinhaBruta(2, valida()), new LinhaBruta(3, ruim),
				new LinhaBruta(4, valida())));

		assertThat(r.linhas()).extracting(LinhaPlanilha::numeroLinha).containsExactly(2, 4);
		assertThat(r.erros()).extracting(ErroLinha::linha).containsExactly(3);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "1.234,56 | 1234.56", "10,5 | 10.5", "10.5 | 10.5", "3 | 3" })
	void normalizaNumeroNoFormatoBrasileiro(String entrada, String esperado) {
		assertThat(ValidadorLinha.normalizarNumero(entrada)).isEqualTo(esperado);
	}

}
