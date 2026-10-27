package com.gabrielsdev6.notabot.validacao;
import java.util.regex.Pattern;

public final class DocumentoValidator {

	private static final int[] PESOS_CPF_DV1 = { 10, 9, 8, 7, 6, 5, 4, 3, 2 };
	private static final int[] PESOS_CPF_DV2 = { 11, 10, 9, 8, 7, 6, 5, 4, 3, 2 };
	private static final int[] PESOS_CNPJ_DV1 = { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
	private static final int[] PESOS_CNPJ_DV2 = { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };

	private DocumentoValidator() {
	}

	public static String somenteDigitos(String valor) {
		if (valor == null) {
			return "";
		}
		return NAO_DIGITOS.matcher(valor).replaceAll("");
	}

	private static final Pattern NAO_DIGITOS = Pattern.compile("\\D");
	
	public static boolean isCpfValido(String cpf) {
		String digitos = somenteDigitos(cpf);
		if (digitos.length() != 11 || todosIguais(digitos)) {
			return false;
		}
		return calcularDv(digitos, PESOS_CPF_DV1) == digito(digitos, 9)
				&& calcularDv(digitos, PESOS_CPF_DV2) == digito(digitos, 10);
	}

	public static boolean isCnpjValido(String cnpj) {
		String digitos = somenteDigitos(cnpj);
		if (digitos.length() != 14 || todosIguais(digitos)) {
			return false;
		}
		return calcularDv(digitos, PESOS_CNPJ_DV1) == digito(digitos, 12)
				&& calcularDv(digitos, PESOS_CNPJ_DV2) == digito(digitos, 13);
	}

	private static int calcularDv(String digitos, int[] pesos) {
		int soma = 0;
		for (int i = 0; i < pesos.length; i++) {
			soma += digito(digitos, i) * pesos[i];
		}
		int resto = soma % 11;
		return resto < 2 ? 0 : 11 - resto;
	}

	private static int digito(String digitos, int posicao) {
		return digitos.charAt(posicao) - '0';
	}

	private static boolean todosIguais(String digitos) {
		return digitos.chars().distinct().count() == 1;
	}

}
