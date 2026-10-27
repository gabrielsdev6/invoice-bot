package com.gabrielsdev6.notabot.emissor;

import java.time.OffsetDateTime;

/**
 * O que o emissor decide na hora de autorizar e que vai no XML junto com a nota.
 */
public record DadosAutorizacao(
		String chave,
		int numero,
		int codigoNumerico,
		OffsetDateTime emitidaEm,
		String protocolo,
		OffsetDateTime autorizadaEm) {
}
