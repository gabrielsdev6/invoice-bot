package com.gabrielsdev6.notabot.emissor;

import java.time.OffsetDateTime;

public record ResultadoEmissao(
		String pedido,
		StatusEmissao status,
		int codigo,
		String motivo,
		String chave,
		String protocolo,
		OffsetDateTime autorizadaEm,
		String xml) {

	public static final int AUTORIZADO = 100;

	public static ResultadoEmissao rejeitada(String pedido, Rejeicao rejeicao) {
		return new ResultadoEmissao(pedido, StatusEmissao.REJEITADA, rejeicao.codigo(), rejeicao.motivo(), null,
				null, null, null);
	}

	public boolean isAutorizada() {
		return status == StatusEmissao.AUTORIZADA;
	}

}
