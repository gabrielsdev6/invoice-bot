package com.gabrielsdev6.notabot.planilha;

import java.util.List;

/**
 * Nomes das colunas da planilha modelo, na ordem em que aparecem.
 */
public final class ColunasPlanilha {

	public static final String PEDIDO = "pedido";
	public static final String DATA_EMISSAO = "data_emissao";
	public static final String CLIENTE_NOME = "cliente_nome";
	public static final String CLIENTE_DOCUMENTO = "cliente_documento";
	public static final String CLIENTE_UF = "cliente_uf";
	public static final String PRODUTO_CODIGO = "produto_codigo";
	public static final String PRODUTO_DESCRICAO = "produto_descricao";
	public static final String NCM = "ncm";
	public static final String CFOP = "cfop";
	public static final String QUANTIDADE = "quantidade";
	public static final String VALOR_UNITARIO = "valor_unitario";

	public static final List<String> TODAS = List.of(PEDIDO, DATA_EMISSAO, CLIENTE_NOME, CLIENTE_DOCUMENTO,
			CLIENTE_UF, PRODUTO_CODIGO, PRODUTO_DESCRICAO, NCM, CFOP, QUANTIDADE, VALOR_UNITARIO);

	private ColunasPlanilha() {
	}

}
