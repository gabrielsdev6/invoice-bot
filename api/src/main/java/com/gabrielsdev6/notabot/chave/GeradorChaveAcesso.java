package com.gabrielsdev6.notabot.chave;

/**
 * 👤 TAREFA 2 — Montar a chave de acesso da NF-e (44 dígitos).
 *
 * Testes: src/test/java/.../chave/GeradorChaveAcessoTest.java
 * Rodar só eles:  ./mvnw test -Dtest=GeradorChaveAcessoTest
 *
 * O QUE TEM NA CHAVE, NESTA ORDEM (todos com zeros à esquerda)
 *
 *   campo          tamanho  exemplo
 *   cUF               2     35            (SP)
 *   AAMM              4     2610          (outubro de 2026)
 *   CNPJ emitente    14     11222333000181
 *   modelo            2     55            (sempre 55 = NF-e)
 *   série             3     001
 *   número            9     000000001
 *   tpEmis            1     1             (sempre 1 = emissão normal)
 *   cNF               8     12345678
 *   DV                1     0
 *                    --
 *                    44
 *
 * COMO O DV FUNCIONA (módulo 11, parecido com o CPF mas não igual)
 * - Pegue os 43 primeiros dígitos e multiplique cada um por um peso,
 *   começando DA DIREITA com peso 2 e subindo até 9. Depois do 9, volta pro 2.
 * - resto = soma % 11. Se resto for 0 ou 1, DV = 0. Senão, DV = 11 - resto.
 * - Exemplo do Manual de Orientação do Contribuinte:
 *   5206043300991100250655012000000780026730161 → DV 5
 *
 * REGRAS DE ENTRADA (se quebrar, lance IllegalArgumentException)
 * - dados e anoMes não podem ser null.
 * - codigoUf entre 11 e 53 (é a faixa dos códigos IBGE de UF).
 * - cnpjEmitente com exatamente 14 dígitos (só dígitos; não precisa validar o DV aqui).
 * - série entre 1 e 999; número entre 1 e 999.999.999; cNF entre 0 e 99.999.999.
 * - calcularDigitoVerificador só aceita exatamente 43 dígitos.
 *
 * DICAS (só leia se travar)
 * - Dica 1: String.format("%03d", 1) devolve "001". Funciona pra todos os campos numéricos.
 * - Dica 2: o AAMM: anoMes.getYear() % 100 e anoMes.getMonthValue().
 *           Ou procure por DateTimeFormatter.ofPattern("yyMM").
 * - Dica 3: monte os 43 primeiros dígitos com um StringBuilder, calcule o DV
 *           com o outro método e cole no final.
 * - Dica 4: pra andar da direita pra esquerda: for (int i = s.length() - 1; i >= 0; i--).
 *           O peso pode ser uma variável que você incrementa e "reseta" pra 2.
 */
public final class GeradorChaveAcesso {

	private GeradorChaveAcesso() {
	}

	public static String gerar(DadosChave dados) {
		throw new UnsupportedOperationException("TODO: tarefa 2");
	}

	public static int calcularDigitoVerificador(String chaveSemDv) {
		throw new UnsupportedOperationException("TODO: tarefa 2");
	}

}
