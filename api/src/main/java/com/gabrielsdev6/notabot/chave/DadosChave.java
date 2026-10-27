package com.gabrielsdev6.notabot.chave;

import java.time.YearMonth;

public record DadosChave(
		int codigoUf,
		YearMonth anoMes,
		String cnpjEmitente,
		int serie,
		int numero,
		int codigoNumerico) {
}
