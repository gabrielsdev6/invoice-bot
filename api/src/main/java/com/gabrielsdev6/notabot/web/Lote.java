package com.gabrielsdev6.notabot.web;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.gabrielsdev6.notabot.emissor.ResultadoEmissao;
import com.gabrielsdev6.notabot.nota.Nota;
import com.gabrielsdev6.notabot.planilha.ErroLinha;

class Lote {

	private final String id;
	private final List<Nota> notas;
	private final List<ErroLinha> erros;
	private final List<String> chavesAutorizadas = new CopyOnWriteArrayList<>();

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

	List<String> chavesAutorizadas() {
		return List.copyOf(chavesAutorizadas);
	}

	void registrar(List<ResultadoEmissao> resultados) {
		for (ResultadoEmissao resultado : resultados) {
			if (resultado.isAutorizada()) {
				chavesAutorizadas.add(resultado.chave());
			}
		}
	}

}
