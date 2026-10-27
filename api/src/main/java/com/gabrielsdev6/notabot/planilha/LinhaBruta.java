package com.gabrielsdev6.notabot.planilha;

import java.util.Map;

public record LinhaBruta(int numeroLinha, Map<String, String> valores) {

	public String valor(String coluna) {
		return valores.getOrDefault(coluna, "");
	}

}
