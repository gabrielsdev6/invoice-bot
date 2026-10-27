package com.gabrielsdev6.notabot.emissor;

import com.gabrielsdev6.notabot.nota.Nota;

/**
 * Manda a nota pra SEFAZ e devolve a resposta. Hoje só existe o EmissorSimulado;
 * uma emissão de verdade (API externa ou SEFAZ direto) entra implementando esta interface.
 */
public interface Emissor {

	ResultadoEmissao emitir(Nota nota);

}
