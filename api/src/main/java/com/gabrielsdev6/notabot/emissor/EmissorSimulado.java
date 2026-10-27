package com.gabrielsdev6.notabot.emissor;

import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

import com.gabrielsdev6.notabot.chave.DadosChave;
import com.gabrielsdev6.notabot.chave.GeradorChaveAcesso;
import com.gabrielsdev6.notabot.nota.Nota;

/**
 * Faz o papel da SEFAZ: decide se autoriza (usando RegrasRejeicao), numera a nota,
 * gera a chave, o protocolo e o XML. Guarda os pedidos já autorizados enquanto a API
 * estiver rodando, então emitir o mesmo pedido duas vezes dá duplicidade.
 */
public class EmissorSimulado implements Emissor {

	private static final int SERIE = 1;

	private final RegrasRejeicao regras;
	private final Clock clock;
	private final Emitente emitente;
	private final RandomGenerator aleatorio;

	private final Set<String> pedidosAutorizados = new HashSet<>();
	private int proximoNumero = 1;

	public EmissorSimulado(RegrasRejeicao regras, Clock clock, Emitente emitente, RandomGenerator aleatorio) {
		this.regras = regras;
		this.clock = clock;
		this.emitente = emitente;
		this.aleatorio = aleatorio;
	}

	/** synchronized: duas emissões ao mesmo tempo não podem pegar o mesmo número nem furar a duplicidade. */
	@Override
	public synchronized ResultadoEmissao emitir(Nota nota) {
		Optional<Rejeicao> rejeicao = regras.verificar(nota, Collections.unmodifiableSet(pedidosAutorizados));
		if (rejeicao.isPresent()) {
			return ResultadoEmissao.rejeitada(nota.pedido(), rejeicao.get());
		}

		int numero = proximoNumero++;
		int codigoNumerico = aleatorio.nextInt(100_000_000);
		String chave = GeradorChaveAcesso.gerar(new DadosChave(emitente.codigoUf(),
				YearMonth.from(nota.dataEmissao()), emitente.cnpj(), SERIE, numero, codigoNumerico));

		OffsetDateTime agora = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
		OffsetDateTime emitidaEm = nota.dataEmissao().atTime(LocalTime.from(agora)).atOffset(agora.getOffset());
		String protocolo = gerarProtocolo(agora, numero);

		DadosAutorizacao autorizacao = new DadosAutorizacao(chave, numero, codigoNumerico, emitidaEm, protocolo,
				agora);
		String xml = GeradorXmlNfe.gerar(nota, emitente, autorizacao);

		pedidosAutorizados.add(nota.pedido());
		return new ResultadoEmissao(nota.pedido(), StatusEmissao.AUTORIZADA, ResultadoEmissao.AUTORIZADO,
				"Autorizado o uso da NF-e", chave, protocolo, agora, xml);
	}

	/** 15 dígitos, no formato da SEFAZ: 1 + cUF + ano(2) + sequencial(10). */
	private String gerarProtocolo(OffsetDateTime agora, int numero) {
		return String.format("1%02d%02d%010d", emitente.codigoUf(), agora.getYear() % 100, numero);
	}

}
