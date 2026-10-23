package com.gabrielsdev6.notabot.nota;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Uma nota pronta pra emitir: um pedido da planilha com todos os seus itens.
 */
public record Nota(
		String pedido,
		LocalDate dataEmissao,
		Cliente cliente,
		List<ItemNota> itens,
		BigDecimal valorTotal) {
}
