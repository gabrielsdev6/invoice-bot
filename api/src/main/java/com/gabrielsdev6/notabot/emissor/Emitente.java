package com.gabrielsdev6.notabot.emissor;

/**
 * Quem emite as notas. Na v1 é sempre a mesma empresa fictícia, do Simples Nacional.
 *
 * @param codigoUf código IBGE da UF (SP = 35), que vai na chave de acesso
 */
public record Emitente(String cnpj, String razaoSocial, String inscricaoEstadual, String municipio, String uf,
		int codigoUf) {

	public static final Emitente FICTICIA = new Emitente("11444777000161", "Notabot Comércio de Exemplo Ltda",
			"110042490114", "São Paulo", "SP", 35);

}
