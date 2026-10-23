package com.gabrielsdev6.notabot.emissor;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gabrielsdev6.notabot.nota.Cliente;
import com.gabrielsdev6.notabot.nota.Nota;

class RegrasRejeicaoTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
	private static final LocalDate HOJE = LocalDate.of(2026, 10, 22);

	// "hoje" fica congelado em 22/10/2026 pra todos os testes
	private final RegrasRejeicao regras = new RegrasRejeicao(
			Clock.fixed(HOJE.atStartOfDay(SAO_PAULO).plusHours(12).toInstant(), SAO_PAULO));

	private static Nota nota(String pedido, LocalDate data) {
		return new Nota(pedido, data, new Cliente("Padaria Pão Quente", "11222333000181", "SP"), List.of(),
				BigDecimal.TEN);
	}

	@Test
	void notaNormalPassa() {
		assertThat(regras.verificar(nota("P1", HOJE), Set.of())).isEmpty();
	}

	@Test
	void pedidoJaAutorizadoEhDuplicidade() {
		assertThat(regras.verificar(nota("P1", HOJE), Set.of("P0", "P1")))
				.contains(new Rejeicao(204, "Duplicidade de NF-e"));
	}

	@Test
	void outroPedidoAutorizadoNaoAtrapalha() {
		assertThat(regras.verificar(nota("P2", HOJE), Set.of("P1"))).isEmpty();
	}

	@Test
	void trintaEUmDiasAtrasEhMuitoAtrasada() {
		assertThat(regras.verificar(nota("P1", HOJE.minusDays(31)), Set.of()))
				.contains(new Rejeicao(228, "Data de Emissão muito atrasada"));
	}

	@Test
	void exatamenteTrintaDiasAtrasAindaPassa() {
		assertThat(regras.verificar(nota("P1", HOJE.minusDays(30)), Set.of())).isEmpty();
	}

	@Test
	void duplicidadeGanhaDeDataAtrasada() {
		assertThat(regras.verificar(nota("P1", HOJE.minusDays(90)), Set.of("P1")))
				.get().extracting(Rejeicao::codigo).isEqualTo(204);
	}

	@Test
	void naoMexeNoSetDePedidosAutorizados() {
		Set<String> autorizados = new HashSet<>(Set.of("P9"));

		regras.verificar(nota("P1", HOJE), autorizados);

		assertThat(autorizados).containsExactly("P9");
	}

}
