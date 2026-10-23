package com.gabrielsdev6.notabot.planilha;

/**
 * Problema encontrado numa linha da planilha. Vai pro front do jeito que está.
 */
public record ErroLinha(int linha, String campo, String mensagem) {
}
