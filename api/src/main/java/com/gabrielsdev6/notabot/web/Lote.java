package com.gabrielsdev6.notabot.web;

import java.util.List;

import com.gabrielsdev6.notabot.emissor.ResultadoEmissao;
import com.gabrielsdev6.notabot.nota.Nota;
import com.gabrielsdev6.notabot.planilha.ErroLinha;

class Lote {

	private final String id;
	private final List<Nota> notas;
	private final List<ErroLinha> erros;
	private volatile List<ResultadoEmissao> resultados = List.of();

	Lote(String id, List<Nota> notas, List<ErroLinha> erros) {
		this.id = id;
		this.notas = List.copyOf(notas);
		this.erros = List.copyOf(erros);
	}

	String id() {
		return id;
	}

	List<Nota> notas() {
		return notas;
	}

	List<ErroLinha> erros() {
		return erros;
	}

	List<ResultadoEmissao> resultados() {
		return resultados;
	}

	void registrar(List<ResultadoEmissao> resultados) {
		this.resultados = List.copyOf(resultados);
	}

}
