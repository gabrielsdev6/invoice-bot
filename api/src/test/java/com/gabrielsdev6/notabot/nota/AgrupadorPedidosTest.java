package com.gabrielsdev6.notabot.nota;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gabrielsdev6.notabot.planilha.ErroLinha;
import com.gabrielsdev6.notabot.planilha.LinhaPlanilha;

class AgrupadorPedidosTest {

	private static final LocalDate DATA = LocalDate.of(2026, 10, 20);

	private static LinhaPlanilha linha(int numeroLinha, String pedido, String produto, String qtd, String valor) {
		return new LinhaPlanilha(numeroLinha, pedido, DATA, "Padaria Pão Quente", "11222333000181", "SP",
				produto, "Produto " + produto, "19052090", "5102", new BigDecimal(qtd), new BigDecimal(valor));
	}

	@Test
	void listaVaziaDevolveTudoVazio() {
		ResultadoAgrupamento r = AgrupadorPedidos.agrupar(List.of());

		assertThat(r.notas()).isEmpty();
		assertThat(r.erros()).isEmpty();
	}

	@Test
	void umaLinhaViraUmaNotaComUmItem() {
		ResultadoAgrupamento r = AgrupadorPedidos.agrupar(List.of(linha(2, "P1", "A", "3", "10.00")));

		assertThat(r.notas()).hasSize(1);
		Nota nota = r.notas().get(0);
		assertThat(nota.pedido()).isEqualTo("P1");
		assertThat(nota.dataEmissao()).isEqualTo(DATA);
		assertThat(nota.cliente()).isEqualTo(new Cliente("Padaria Pão Quente", "11222333000181", "SP"));
		assertThat(nota.itens()).hasSize(1);
		ItemNota item = nota.itens().get(0);
		assertThat(item.numeroItem()).isEqualTo(1);
		assertThat(item.produtoCodigo()).isEqualTo("A");
		assertThat(item.produtoDescricao()).isEqualTo("Produto A");
		assertThat(item.ncm()).isEqualTo("19052090");
		assertThat(item.cfop()).isEqualTo("5102");
		assertThat(item.valorTotal()).isEqualByComparingTo("30.00");
		assertThat(nota.valorTotal()).isEqualByComparingTo("30.00");
	}

	@Test
	void linhasDoMesmoPedidoViramUmaNotaSo() {
		ResultadoAgrupamento r = AgrupadorPedidos.agrupar(List.of(
				linha(2, "P1", "A", "1", "5.00"),
				linha(3, "P1", "B", "2", "7.50"),
				linha(4, "P1", "C", "1", "0.99")));

		assertThat(r.notas()).hasSize(1);
		assertThat(r.notas().get(0).itens()).extracting(ItemNota::numeroItem).containsExactly(1, 2, 3);
		assertThat(r.notas().get(0).itens()).extracting(ItemNota::produtoCodigo).containsExactly("A", "B", "C");
		assertThat(r.notas().get(0).valorTotal()).isEqualByComparingTo("20.99");
	}

	@Test
	void pedidosMisturadosSaemNaOrdemDaPrimeiraAparicao() {
		ResultadoAgrupamento r = AgrupadorPedidos.agrupar(List.of(
				linha(2, "P2", "A", "1", "1.00"),
				linha(3, "P1", "B", "1", "1.00"),
				linha(4, "P2", "C", "1", "1.00"),
				linha(5, "P3", "D", "1", "1.00")));

		assertThat(r.notas()).extracting(Nota::pedido).containsExactly("P2", "P1", "P3");
		assertThat(r.notas().get(0).itens()).extracting(ItemNota::produtoCodigo).containsExactly("A", "C");
		assertThat(r.notas().get(0).itens()).extracting(ItemNota::numeroItem).containsExactly(1, 2);
	}

	@Test
	void arredondaOTotalDoItemPraDuasCasasHalfUp() {
		// 3 × 3.335 = 10.005 → 10.01 (HALF_UP); 0.5 × 1.25 = 0.625 → 0.63
		ResultadoAgrupamento r = AgrupadorPedidos.agrupar(List.of(
				linha(2, "P1", "A", "3", "3.335"),
				linha(3, "P1", "B", "0.5", "1.25")));

		Nota nota = r.notas().get(0);
		assertThat(nota.itens().get(0).valorTotal()).isEqualByComparingTo("10.01");
		assertThat(nota.itens().get(1).valorTotal()).isEqualByComparingTo("0.63");
		assertThat(nota.itens().get(0).valorTotal().scale()).isEqualTo(2);
		assertThat(nota.valorTotal()).isEqualByComparingTo("10.64");
	}

	@Test
	void totalDaNotaNaoTemErroDeDouble() {
		// com double, 0.1 + 0.2 dá 0.30000000000000004
		ResultadoAgrupamento r = AgrupadorPedidos.agrupar(List.of(
				linha(2, "P1", "A", "1", "0.10"),
				linha(3, "P1", "B", "1", "0.20")));

		assertThat(r.notas().get(0).valorTotal()).isEqualByComparingTo("0.30");
	}

	@Test
	void pedidoComClienteDiferenteEntreLinhasViraErroEmTodasAsLinhas() {
		LinhaPlanilha outroCliente = new LinhaPlanilha(3, "P1", DATA, "Mercado Bom Preço", "11444777000161", "SP",
				"B", "Produto B", "19052090", "5102", BigDecimal.ONE, BigDecimal.TEN);

		ResultadoAgrupamento r = AgrupadorPedidos.agrupar(List.of(
				linha(2, "P1", "A", "1", "1.00"),
				outroCliente,
				linha(4, "P2", "C", "1", "1.00")));

		assertThat(r.notas()).extracting(Nota::pedido).containsExactly("P2");
		assertThat(r.erros()).extracting(ErroLinha::linha).containsExactlyInAnyOrder(2, 3);
		assertThat(r.erros()).allSatisfy(e -> {
			assertThat(e.campo()).isEqualTo("pedido");
			assertThat(e.mensagem()).contains("P1");
		});
	}

	@Test
	void pedidoComDataDiferenteEntreLinhasTambemViraErro() {
		LinhaPlanilha outraData = new LinhaPlanilha(3, "P1", DATA.plusDays(1), "Padaria Pão Quente",
				"11222333000181", "SP", "B", "Produto B", "19052090", "5102", BigDecimal.ONE, BigDecimal.TEN);

		ResultadoAgrupamento r = AgrupadorPedidos.agrupar(List.of(linha(2, "P1", "A", "1", "1.00"), outraData));

		assertThat(r.notas()).isEmpty();
		assertThat(r.erros()).hasSize(2);
	}

	@Test
	void pedidoComUfDiferenteEntreLinhasTambemViraErro() {
		LinhaPlanilha outraUf = new LinhaPlanilha(3, "P1", DATA, "Padaria Pão Quente", "11222333000181", "RJ",
				"B", "Produto B", "19052090", "5102", BigDecimal.ONE, BigDecimal.TEN);

		ResultadoAgrupamento r = AgrupadorPedidos.agrupar(List.of(linha(2, "P1", "A", "1", "1.00"), outraUf));

		assertThat(r.notas()).isEmpty();
		assertThat(r.erros()).hasSize(2);
	}

	@Test
	void produtosDiferentesNoMesmoPedidoNaoSaoErro() {
		// só cliente e data precisam bater; produto, NCM e CFOP podem variar
		LinhaPlanilha outroNcm = new LinhaPlanilha(3, "P1", DATA, "Padaria Pão Quente", "11222333000181", "SP",
				"B", "Café", "09012100", "5101", BigDecimal.ONE, BigDecimal.TEN);

		ResultadoAgrupamento r = AgrupadorPedidos.agrupar(List.of(linha(2, "P1", "A", "1", "1.00"), outroNcm));

		assertThat(r.erros()).isEmpty();
		assertThat(r.notas()).hasSize(1);
		assertThat(r.notas().get(0).itens()).hasSize(2);
	}

}
