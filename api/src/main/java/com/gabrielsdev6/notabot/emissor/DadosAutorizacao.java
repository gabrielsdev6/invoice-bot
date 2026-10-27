package com.gabrielsdev6.notabot.emissor;

import java.time.OffsetDateTime;

public record DadosAutorizacao(
		String chave,
		int numero,
		int codigoNumerico,
		OffsetDateTime emitidaEm,
		String protocolo,
		OffsetDateTime autorizadaEm) {
}
