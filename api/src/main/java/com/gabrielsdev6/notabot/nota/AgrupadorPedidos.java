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

/**
 * 👤 TAREFA 3 — Transformar as linhas da planilha em notas.
 *
 * Testes: src/test/java/.../nota/AgrupadorPedidosTest.java
 * Rodar só eles:  ./mvnw test -Dtest=AgrupadorPedidosTest
 *
 * As linhas que chegam aqui já foram validadas uma a uma (documento só com
 * dígitos, UF em maiúsculas, quantidades e valores positivos). Seu trabalho é
 * juntar as que têm o mesmo pedido.
 *
 * REGRAS
 * 1. Cada pedido diferente vira uma Nota.
 * 2. As notas saem na ordem em que o pedido aparece pela primeira vez na planilha.
 * 3. Os itens de cada nota saem na ordem das linhas, numerados 1, 2, 3...
 * 4. valorTotal do item = quantidade × valorUnitario, arredondado pra 2 casas
 *    com RoundingMode.HALF_UP.
 * 5. valorTotal da nota = soma dos valorTotal dos itens.
 * 6. O cliente e a data da nota vêm das linhas do pedido. Se as linhas do mesmo
 *    pedido NÃO concordarem em dataEmissao, clienteNome, clienteDocumento ou
 *    clienteUf, esse pedido NÃO vira nota: cada linha dele gera um ErroLinha
 *    com campo "pedido" e uma mensagem que cite o número do pedido.
 * 7. Lista vazia → resultado com as duas listas vazias.
 *
 * DICAS (só leia se travar)
 * - Dica 1: um LinkedHashMap<String, List<LinhaPlanilha>> agrupa mantendo a
 *           ordem de chegada. Olhe o método computeIfAbsent.
 * - Dica 2: com streams também dá: Collectors.groupingBy(LinhaPlanilha::pedido,
 *           LinkedHashMap::new, Collectors.toList()).
 * - Dica 3: BigDecimal não usa * nem +. É a.multiply(b), a.add(b) e
 *           valor.setScale(2, RoundingMode.HALF_UP). Comece a soma com BigDecimal.ZERO.
 * - Dica 4: pra regra 6, compare cada linha com a primeira do grupo.
 *           Records já têm equals pronto, e Objects.equals evita erro com null.
 * - Dica 5: separe em métodos private: um que verifica se o grupo é consistente
 *           e um que monta a Nota. O agrupar fica curtinho.
 */
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

	/** Todas as linhas do grupo têm a mesma data e o mesmo cliente da primeira? */
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
