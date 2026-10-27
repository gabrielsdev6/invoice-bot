package com.gabrielsdev6.notabot.web;

import java.time.OffsetDateTime;
import java.util.List;

import com.gabrielsdev6.notabot.emissor.ResultadoEmissao;
import com.gabrielsdev6.notabot.emissor.StatusEmissao;
import com.gabrielsdev6.notabot.nota.Nota;
import com.gabrielsdev6.notabot.planilha.ErroLinha;

/**
 * O formato do JSON que a API devolve. Fica separado dos records internos pra
 * gente poder mudar um sem quebrar o outro (o XML, por exemplo, não vai no JSON).
 */
final class Respostas {

	private Respostas() {
	}

	record Preview(String loteId, List<Nota> notas, List<ErroLinha> erros) {

		static Preview de(Lote lote) {
			return new Preview(lote.id(), lote.notas(), lote.erros());
		}

	}

	record Emissao(int autorizadas, int rejeitadas, List<NotaEmitida> notas) {

		static Emissao de(List<ResultadoEmissao> resultados) {
			List<NotaEmitida> notas = resultados.stream().map(NotaEmitida::de).toList();
			int autorizadas = (int) resultados.stream().filter(ResultadoEmissao::isAutorizada).count();
			return new Emissao(autorizadas, resultados.size() - autorizadas, notas);
		}

	}

	record NotaEmitida(String pedido, StatusEmissao status, int codigo, String motivo, String chave,
			String protocolo, OffsetDateTime autorizadaEm) {

		static NotaEmitida de(ResultadoEmissao r) {
			return new NotaEmitida(r.pedido(), r.status(), r.codigo(), r.motivo(), r.chave(), r.protocolo(),
					r.autorizadaEm());
		}

	}

	record Erro(String mensagem) {
	}

}
