package com.gabrielsdev6.notabot.emissor;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;

import com.gabrielsdev6.notabot.nota.Nota;

/**
 * 👤 TAREFA 4 — Decidir se a SEFAZ (simulada) recusa a nota.
 *
 * Testes: src/test/java/.../emissor/RegrasRejeicaoTest.java
 * Rodar só eles:  ./mvnw test -Dtest=RegrasRejeicaoTest
 *
 * Diferente das outras tarefas, esta classe NÃO é estática: ela recebe um
 * Clock no construtor. É assim que o teste consegue fingir que hoje é um dia
 * fixo; se você usasse LocalDate.now() direto, o teste quebraria com o tempo.
 *
 * REGRAS (nesta ordem; a primeira que bater é a que volta)
 * 1. O pedido já está em pedidosAutorizados
 *      → new Rejeicao(204, "Duplicidade de NF-e")
 * 2. A data de emissão é de MAIS de 30 dias atrás (hoje vem do clock)
 *      → new Rejeicao(228, "Data de Emissão muito atrasada")
 *    Exatamente 30 dias atrás ainda passa.
 * 3. Nenhuma das duas → Optional.empty() (a nota é autorizada).
 *
 * Este método só DECIDE. Quem adiciona o pedido em pedidosAutorizados depois
 * de autorizar é o EmissorSimulado (parte 🤖), então não mexa no Set.
 *
 * DICAS (só leia se travar)
 * - Dica 1: LocalDate.now(clock) dá o "hoje" do teste.
 * - Dica 2: LocalDate tem minusDays, isBefore e isAfter.
 * - Dica 3: Optional.of(...) quando tem rejeição, Optional.empty() quando não tem.
 */
public class RegrasRejeicao {

	private final Clock clock;

	public RegrasRejeicao(Clock clock) {
		this.clock = clock;
	}

	public Optional<Rejeicao> verificar(Nota nota, Set<String> pedidosAutorizados) {
		throw new UnsupportedOperationException("TODO: tarefa 4");
	}

}
