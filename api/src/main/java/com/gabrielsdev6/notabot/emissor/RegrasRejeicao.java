package com.gabrielsdev6.notabot.emissor;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;

import com.gabrielsdev6.notabot.nota.Nota;

public class RegrasRejeicao {

	private final Clock clock;

	public RegrasRejeicao(Clock clock) {
		this.clock = clock;
	}

	public Optional<Rejeicao> verificar(Nota nota, Set<String> pedidosAutorizados) {
		throw new UnsupportedOperationException("TODO: tarefa 4");
	}

}
