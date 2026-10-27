package com.gabrielsdev6.notabot.planilha;

import java.util.List;

public record ResultadoValidacao(List<LinhaPlanilha> linhas, List<ErroLinha> erros) {
}
