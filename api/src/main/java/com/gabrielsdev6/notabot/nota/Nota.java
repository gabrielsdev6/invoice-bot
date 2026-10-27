package com.gabrielsdev6.notabot.nota;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Nota(
		String pedido,
		LocalDate dataEmissao,
		Cliente cliente,
		List<ItemNota> itens,
		BigDecimal valorTotal) {
}
