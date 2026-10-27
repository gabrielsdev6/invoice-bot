package com.gabrielsdev6.notabot.nota;

import java.math.BigDecimal;

public record ItemNota(
		int numeroItem,
		String produtoCodigo,
		String produtoDescricao,
		String ncm,
		String cfop,
		BigDecimal quantidade,
		BigDecimal valorUnitario,
		BigDecimal valorTotal) {
}
