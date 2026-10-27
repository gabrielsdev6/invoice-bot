package com.gabrielsdev6.notabot.planilha;

/**
 * O arquivo inteiro não dá pra usar (não é Excel, falta coluna...). Vira HTTP 400.
 * Problema numa linha só não é isso: vira ErroLinha.
 */
public class PlanilhaInvalidaException extends RuntimeException {

	public PlanilhaInvalidaException(String mensagem) {
		super(mensagem);
	}

	public PlanilhaInvalidaException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}

}
