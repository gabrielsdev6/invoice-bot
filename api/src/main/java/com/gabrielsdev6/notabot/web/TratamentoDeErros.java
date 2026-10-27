package com.gabrielsdev6.notabot.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.gabrielsdev6.notabot.planilha.PlanilhaInvalidaException;

@RestControllerAdvice
class TratamentoDeErros {

	@ExceptionHandler(PlanilhaInvalidaException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	Respostas.Erro planilhaInvalida(PlanilhaInvalidaException e) {
		return new Respostas.Erro(e.getMessage());
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	Respostas.Erro semArquivo() {
		return new Respostas.Erro("Envie a planilha no campo \"arquivo\" (multipart/form-data).");
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	@ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
	Respostas.Erro arquivoGrande() {
		return new Respostas.Erro("A planilha passou do limite de 5 MB.");
	}

	@ExceptionHandler(UnsupportedOperationException.class)
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	Respostas.Erro naoImplementado(UnsupportedOperationException e) {
		return new Respostas.Erro("Ainda não implementado: " + e.getMessage());
	}

	@ExceptionHandler(NaoEncontradoException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	Respostas.Erro naoEncontrado(NaoEncontradoException e) {
		return new Respostas.Erro(e.getMessage());
	}

}
