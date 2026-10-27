package com.gabrielsdev6.notabot.nota;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.gabrielsdev6.notabot.planilha.ErroLinha;
import com.gabrielsdev6.notabot.planilha.LinhaPlanilha;

public final class AgrupadorPedidos {

	private AgrupadorPedidos() {
	}

	public static ResultadoAgrupamento agrupar(List<LinhaPlanilha> linhas) {
		Map<String, List<LinhaPlanilha>> grupos = new LinkedHashMap<>();
		for (LinhaPlanilha linha : linhas) {
			grupos.computeIfAbsent(linha.pedido(), k -> new ArrayList<>()).add(linha);
		}

		List<Nota> notas = new ArrayList<>();
		List<ErroLinha> erros = new ArrayList<>();
		for (List<LinhaPlanilha> grupo : grupos.values()) {
			if (isConsistente(grupo)) {
				notas.add(montarNota(grupo));
			} else {
				for (LinhaPlanilha linha : grupo) {
					erros.add(new ErroLinha(linha.numeroLinha(), "pedido",
							"As linhas do pedido " + linha.pedido() + " têm cliente ou data de emissão diferentes"));
				}
			}
		}

		return new ResultadoAgrupamento(notas, erros);
	}

	private static boolean isConsistente(List<LinhaPlanilha> grupo) {
		LinhaPlanilha primeira = grupo.get(0);
		for (LinhaPlanilha linha : grupo) {
			if (!Objects.equals(linha.dataEmissao(), primeira.dataEmissao())
					|| !Objects.equals(linha.clienteNome(), primeira.clienteNome())
					|| !Objects.equals(linha.clienteDocumento(), primeira.clienteDocumento())
					|| !Objects.equals(linha.clienteUf(), primeira.clienteUf())) {
				return false;
			}
		}
		return true;
	}

	private static Nota montarNota(List<LinhaPlanilha> grupo) {
		LinhaPlanilha primeira = grupo.get(0);
		Cliente cliente = new Cliente(primeira.clienteNome(), primeira.clienteDocumento(), primeira.clienteUf());

		List<ItemNota> itens = new ArrayList<>();
		BigDecimal totalNota = BigDecimal.ZERO;
		int numero = 1;
		for (LinhaPlanilha linha : grupo) {
			BigDecimal totalItem = linha.quantidade().multiply(linha.valorUnitario())
					.setScale(2, RoundingMode.HALF_UP);
			itens.add(new ItemNota(numero, linha.produtoCodigo(), linha.produtoDescricao(),
					linha.ncm(), linha.cfop(), linha.quantidade(), linha.valorUnitario(), totalItem));
			totalNota = totalNota.add(totalItem);
			numero++;
		}

		return new Nota(primeira.pedido(), primeira.dataEmissao(), cliente, itens, totalNota);
	}

}
