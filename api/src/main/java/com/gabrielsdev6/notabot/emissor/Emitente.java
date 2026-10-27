package com.gabrielsdev6.notabot.emissor;

public record Emitente(String cnpj, String razaoSocial, String inscricaoEstadual, String municipio, String uf,
		int codigoUf) {

	public static final Emitente FICTICIA = new Emitente("11444777000161", "Notabot Comércio de Exemplo Ltda",
			"110042490114", "São Paulo", "SP", 35);

}
