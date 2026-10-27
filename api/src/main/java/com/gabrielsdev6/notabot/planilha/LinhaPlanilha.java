package com.gabrielsdev6.notabot.planilha;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LinhaPlanilha(
		int numeroLinha,
		String pedido,
		LocalDate dataEmissao,
		String clienteNome,
		String clienteDocumento,
		String clienteUf,
		String produtoCodigo,
		String produtoDescricao,
		String ncm,
		String cfop,
		BigDecimal quantidade,
		BigDecimal valorUnitario) {
}
