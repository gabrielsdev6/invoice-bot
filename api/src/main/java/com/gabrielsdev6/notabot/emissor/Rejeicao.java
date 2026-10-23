package com.gabrielsdev6.notabot.emissor;

/**
 * Motivo pelo qual a SEFAZ (simulada) recusou a nota. O código é o mesmo da tabela real.
 */
public record Rejeicao(int codigo, String motivo) {
}
