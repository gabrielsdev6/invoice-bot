package com.gabrielsdev6.notabot.chave;

import java.time.YearMonth;

/**
 * Tudo que entra na chave de acesso, menos o que é fixo (modelo 55 e tipo de emissão 1).
 *
 * @param codigoUf        código IBGE da UF da emitente (SP = 35, RJ = 33...)
 * @param anoMes          ano e mês de emissão
 * @param cnpjEmitente    CNPJ da emitente, só dígitos
 * @param serie           série da nota (1 a 999)
 * @param numero          número da nota (1 a 999.999.999)
 * @param codigoNumerico  número aleatório de 8 dígitos (cNF)
 */
public record DadosChave(
		int codigoUf,
		YearMonth anoMes,
		String cnpjEmitente,
		int serie,
		int numero,
		int codigoNumerico) {
}
