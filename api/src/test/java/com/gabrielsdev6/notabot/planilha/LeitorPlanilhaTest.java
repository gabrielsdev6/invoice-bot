package com.gabrielsdev6.notabot.planilha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class LeitorPlanilhaTest {

	private static List<LinhaBruta> ler(byte[] bytes) {
		return LeitorPlanilha.ler(new ByteArrayInputStream(bytes));
	}

	@Test
	void leCadaLinhaComONumeroDaLinhaDoExcel() {
		List<LinhaBruta> linhas = ler(PlanilhaTeste.comColunasPadrao()
				.linhaValida("1001", "A")
				.linhaValida("1001", "B")
				.bytes());

		assertThat(linhas).extracting(LinhaBruta::numeroLinha).containsExactly(2, 3);
		assertThat(linhas.get(0).valor(ColunasPlanilha.PEDIDO)).isEqualTo("1001");
		assertThat(linhas.get(1).valor(ColunasPlanilha.PRODUTO_CODIGO)).isEqualTo("B");
	}

	@Test
	void numerosNaoGanhamPontoZeroNemNotacaoCientifica() {
		List<LinhaBruta> linhas = ler(PlanilhaTeste.comColunasPadrao()
				.linha(1001, LocalDate.of(2026, 10, 20), "Fulano", 11222333000181L, "SP", "A", "Produto", 19052090,
						5102, 3, 10.5)
				.bytes());

		LinhaBruta linha = linhas.get(0);
		assertThat(linha.valor(ColunasPlanilha.PEDIDO)).isEqualTo("1001");
		assertThat(linha.valor(ColunasPlanilha.CLIENTE_DOCUMENTO)).isEqualTo("11222333000181");
		assertThat(linha.valor(ColunasPlanilha.QUANTIDADE)).isEqualTo("3");
		assertThat(linha.valor(ColunasPlanilha.VALOR_UNITARIO)).isEqualTo("10.5");
	}

	@Test
	void celulaDeDataViraIso() {
		List<LinhaBruta> linhas = ler(PlanilhaTeste.comColunasPadrao().linhaValida("1", "A").bytes());

		assertThat(linhas.get(0).valor(ColunasPlanilha.DATA_EMISSAO)).isEqualTo("2026-10-20");
	}

	@Test
	void textoPerdeEspacosNasPontas() {
		List<LinhaBruta> linhas = ler(PlanilhaTeste.comColunasPadrao()
				.linha("  1001 ", "20/10/2026", "  Fulano  ", "", "sp", "A", "X", "1", "1", "1", "1")
				.bytes());

		assertThat(linhas.get(0).valor(ColunasPlanilha.PEDIDO)).isEqualTo("1001");
		assertThat(linhas.get(0).valor(ColunasPlanilha.CLIENTE_NOME)).isEqualTo("Fulano");
		assertThat(linhas.get(0).valor(ColunasPlanilha.CLIENTE_DOCUMENTO)).isEmpty();
	}

	@Test
	void pulaLinhasEmBranco() {
		List<LinhaBruta> linhas = ler(PlanilhaTeste.comColunasPadrao()
				.linhaValida("1", "A")
				.linhaEmBranco()
				.linhaEmBranco()
				.linhaValida("2", "B")
				.bytes());

		assertThat(linhas).extracting(LinhaBruta::numeroLinha).containsExactly(2, 5);
	}

	@Test
	void cabecalhoAceitaMaiusculasEColunasForaDeOrdemEIgnoraColunasExtras() {
		List<String> invertidas = new java.util.ArrayList<>(ColunasPlanilha.TODAS.reversed());
		invertidas.add("observacao");
		String[] cabecalho = invertidas.stream().map(String::toUpperCase).toArray(String[]::new);

		List<LinhaBruta> linhas = ler(PlanilhaTeste.comColunas(cabecalho)
				.linha("10.5", "2", "5102", "19052090", "Produto", "A", "SP", "doc", "Fulano", "2026-10-20", "77",
						"não importa")
				.bytes());

		assertThat(linhas.get(0).valor(ColunasPlanilha.PEDIDO)).isEqualTo("77");
		assertThat(linhas.get(0).valor(ColunasPlanilha.VALOR_UNITARIO)).isEqualTo("10.5");
		assertThat(linhas.get(0).valores()).doesNotContainKey("observacao");
	}

	@Test
	void faltandoColunaEhPlanilhaInvalida() {
		byte[] bytes = PlanilhaTeste.comColunas("pedido", "data_emissao").linha("1", "2026-10-20").bytes();

		assertThatThrownBy(() -> ler(bytes))
				.isInstanceOf(PlanilhaInvalidaException.class)
				.hasMessageContaining("cliente_nome")
				.hasMessageContaining("valor_unitario");
	}

	@Test
	void arquivoQueNaoEhExcelEhPlanilhaInvalida() {
		byte[] bytes = "pedido;cliente\n1;Fulano".getBytes(StandardCharsets.UTF_8);

		assertThatThrownBy(() -> ler(bytes))
				.isInstanceOf(PlanilhaInvalidaException.class)
				.hasMessageContaining(".xlsx");
	}

	@Test
	void oModeloTemTodasAsColunasEDuasNotasDeExemplo() {
		List<LinhaBruta> linhas = ler(ModeloPlanilha.gerar());

		assertThat(linhas).hasSize(3);
		assertThat(linhas).extracting(l -> l.valor(ColunasPlanilha.PEDIDO)).containsExactly("1001", "1001", "1002");
		assertThat(linhas.get(1).valor(ColunasPlanilha.NCM)).as("o zero da esquerda do NCM").isEqualTo("09012100");
		assertThat(linhas.get(0).valor(ColunasPlanilha.DATA_EMISSAO)).isEqualTo(LocalDate.now().toString());
	}

}
