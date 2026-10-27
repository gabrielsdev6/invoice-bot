package com.gabrielsdev6.notabot.web;

/** Lote ou nota que não existe (ou sumiu porque a API reiniciou). Vira HTTP 404. */
class NaoEncontradoException extends RuntimeException {

	NaoEncontradoException(String mensagem) {
		super(mensagem);
	}

}
