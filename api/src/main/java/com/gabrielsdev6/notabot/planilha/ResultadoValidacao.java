package com.gabrielsdev6.notabot.planilha;

import java.util.List;

/**
 * Linhas que passaram (já normalizadas) e os erros das que não passaram.
 */
public record ResultadoValidacao(List<LinhaPlanilha> linhas, List<ErroLinha> erros) {
}
