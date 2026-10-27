package com.gabrielsdev6.notabot.planilha;

public class PlanilhaInvalidaException extends RuntimeException {

	public PlanilhaInvalidaException(String mensagem) {
		super(mensagem);
	}

	public PlanilhaInvalidaException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}

}
