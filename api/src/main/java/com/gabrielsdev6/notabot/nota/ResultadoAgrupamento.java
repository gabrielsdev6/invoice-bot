package com.gabrielsdev6.notabot.nota;

import java.util.List;

import com.gabrielsdev6.notabot.planilha.ErroLinha;

public record ResultadoAgrupamento(List<Nota> notas, List<ErroLinha> erros) {
}
