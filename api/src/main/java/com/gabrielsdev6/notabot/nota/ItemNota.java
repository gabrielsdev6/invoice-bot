package com.gabrielsdev6.notabot.nota;

import java.math.BigDecimal;

/**
 * Um produto dentro da nota. numeroItem começa em 1 (é o nItem do XML).
 */
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
