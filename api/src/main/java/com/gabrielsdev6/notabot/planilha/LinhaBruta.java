package com.gabrielsdev6.notabot.planilha;

import java.util.Map;

/**
 * Uma linha da planilha do jeito que veio, antes de validar: tudo como texto.
 *
 * @param numeroLinha número da linha no Excel (o cabeçalho é a linha 1)
 * @param valores     coluna → valor, sem espaços nas pontas
 */
public record LinhaBruta(int numeroLinha, Map<String, String> valores) {

	/** Valor da coluna, ou "" se a célula estiver vazia. */
	public String valor(String coluna) {
		return valores.getOrDefault(coluna, "");
	}

}
