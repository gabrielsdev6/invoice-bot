package com.gabrielsdev6.notabot.validacao;
import java.util.regex.Pattern;


/**
 * 👤 TAREFA 1 — Validar CPF e CNPJ pelo dígito verificador.
 *
 * Testes: src/test/java/.../validacao/DocumentoValidatorTest.java
 * Rodar só eles:  ./mvnw test -Dtest=DocumentoValidatorTest
 *
 * REGRAS
 * - Aceita o documento com ou sem máscara ("529.982.247-25" ou "52998224725").
 * - null, vazio ou tamanho errado → false (nunca lança exceção).
 * - Documento com todos os dígitos iguais (111.111.111-11) → false, mesmo que
 *   a conta "feche". A Receita não emite esses números.
 *
 * COMO O CPF FUNCIONA (11 dígitos: 9 de base + 2 verificadores)
 * - 1º DV: multiplique os 9 primeiros dígitos por 10, 9, 8 ... 2 e some.
 *          resto = soma % 11. Se resto < 2, DV = 0; senão DV = 11 - resto.
 * - 2º DV: mesma coisa com os 10 primeiros dígitos (já com o 1º DV),
 *          pesos 11, 10, 9 ... 2.
 *
 * COMO O CNPJ FUNCIONA (14 dígitos: 12 de base + 2 verificadores)
 * - 1º DV: pesos 5,4,3,2,9,8,7,6,5,4,3,2 nos 12 primeiros dígitos.
 * - 2º DV: pesos 6,5,4,3,2,9,8,7,6,5,4,3,2 nos 13 primeiros.
 * - Mesma regra do resto: resto < 2 → 0, senão 11 - resto.
 *
 * DICAS (só leia se travar)
 * - Dica 1: comece pelo somenteDigitos, os outros dois métodos usam ele.
 *           Procure por String.replaceAll e a regex "\\D".
 * - Dica 2: pra transformar o caractere '7' no número 7: Character.getNumericValue(c)
 *           ou c - '0'.
 * - Dica 3: o cálculo do DV é igual nos dois, só mudam os pesos. Dá pra
 *           escrever um método private que recebe os dígitos e um int[] de pesos
 *           e devolve o DV. Aí CPF e CNPJ viram poucas linhas cada.
 * - Dica 4: "todos iguais" → compare cada caractere com o primeiro, ou
 *           use doc.chars().distinct().count().
 *
 * DESAFIO EXTRA (opcional, depois dos testes verdes)
 * - Desde julho de 2026 a Receita emite CNPJ ALFANUMÉRICO (ex.: 12.ABC.345/01DE-35).
 *   Pesquise como fica o cálculo (cada caractere vale código ASCII - 48) e
 *   escreva seus próprios testes pra isso.
 */
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

	/** Usa só os primeiros pesos.length dígitos. */
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
