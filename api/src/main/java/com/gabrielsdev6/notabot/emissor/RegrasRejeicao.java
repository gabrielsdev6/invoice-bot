package com.gabrielsdev6.notabot.emissor;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.gabrielsdev6.notabot.nota.Nota;

public class RegrasRejeicao {

	private static final int DIAS_MAXIMOS_DE_ATRASO = 30;

	private final Clock clock;

	public RegrasRejeicao(Clock clock) {
		this.clock = clock;
	}

	public Optional<Rejeicao> verificar(Nota nota, Set<String> pedidosAutorizados) {
		if (pedidosAutorizados.contains(nota.pedido())) {
			return Optional.of(new Rejeicao(204, "Duplicidade de NF-e"));
		}
		LocalDate limite = LocalDate.now(clock).minusDays(DIAS_MAXIMOS_DE_ATRASO);
		if (nota.dataEmissao().isBefore(limite)) {
			return Optional.of(new Rejeicao(228, "Data de Emissão muito atrasada"));
		}
		return Optional.empty();
	}

}
