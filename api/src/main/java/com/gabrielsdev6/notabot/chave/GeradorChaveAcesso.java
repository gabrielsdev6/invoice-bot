package com.gabrielsdev6.notabot.chave;

public final class GeradorChaveAcesso {

	private GeradorChaveAcesso() {
	}

	public static String gerar(DadosChave dados) {
		if (dados == null){
			throw new IllegalArgumentException("dados não pode ser null");
		} else if (dados.anoMes() == null){
			throw new IllegalArgumentException("anoMes não pode ser null");
		} else if (dados.codigoUf() < 11 || dados.codigoUf() > 53){
			throw new IllegalArgumentException("codigoUf deve estar entre 11 e 53, mas veio: " + dados.codigoUf());
		} else if (dados.serie() < 1 || dados.serie() > 999){
			throw new IllegalArgumentException("serie deve estar entre 1 e 999, mas veio: " + dados.serie());
		} else if (dados.numero() < 1 || dados.numero() > 999_999_999){
			throw new IllegalArgumentException("numero deve estar entre 1 e 999_999_999, mas veio: " + dados.numero());
		} else if (dados.cnpjEmitente() == null || !dados.cnpjEmitente().matches("\\d{14}")){
			throw new IllegalArgumentException("cnpjEmitente deve ter exatamente 14 dígitos, mas veio: " + dados.cnpjEmitente());
		} else if (dados.codigoNumerico() < 0 || dados.codigoNumerico() > 99999999){
			throw new IllegalArgumentException("codigoNumerico deve estar entre 0 e 99999999, mas veio: " + dados.codigoNumerico());
		}

		StringBuilder chave = new StringBuilder();
		chave.append(String.format("%02d", dados.codigoUf()));
		chave.append(dados.anoMes().format(java.time.format.DateTimeFormatter.ofPattern("yyMM")));
		chave.append(dados.cnpjEmitente());
		chave.append("55");
		chave.append(String.format("%03d", dados.serie()));
		chave.append(String.format("%09d", dados.numero()));
		chave.append("1");
		chave.append(String.format("%08d", dados.codigoNumerico()));
		int dv = calcularDigitoVerificador(chave.toString());
		chave.append(dv);
		return chave.toString();

	}

	public static int calcularDigitoVerificador(String chaveSemDv) {
		if (chaveSemDv == null) {
			throw new IllegalArgumentException("chaveSemDv não pode ser null");
		}
		if (!chaveSemDv.matches("\\d{43}")) {
			throw new IllegalArgumentException("chaveSemDv deve ter exatamente 43 dígitos, mas veio: " + chaveSemDv);
		}
		int soma = 0;
		for (int i = chaveSemDv.length() - 1, peso = 2; i >= 0; i--, peso++) {
			if (peso > 9) {
				peso = 2;
			}
			int digito = Character.getNumericValue(chaveSemDv.charAt(i));
			soma += digito * peso;
		}
		int resto = soma % 11;
		return resto == 0 || resto == 1 ? 0 : 11 - resto;
	}

}
