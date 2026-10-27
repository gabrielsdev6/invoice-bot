package com.gabrielsdev6.notabot.emissor;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.gabrielsdev6.notabot.nota.ItemNota;
import com.gabrielsdev6.notabot.nota.Nota;

/**
 * Monta o XML da NF-e autorizada (nfeProc = NFe + protocolo) num subconjunto do layout 4.00.
 *
 * O que fica de fora de propósito: assinatura digital (Signature), endereço completo,
 * PIS/COFINS, pagamento e transporte detalhado. O suficiente pra entender a estrutura,
 * não pra validar no schema oficial.
 */
public final class GeradorXmlNfe {

	public static final String NAMESPACE = "http://www.portalfiscal.inf.br/nfe";

	private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

	/** tpAmb 2 = homologação: nota de teste, sem valor fiscal. */
	private static final String AMBIENTE_HOMOLOGACAO = "2";

	private GeradorXmlNfe() {
	}

	public static String gerar(Nota nota, Emitente emitente, DadosAutorizacao autorizacao) {
		StringWriter saida = new StringWriter();
		try {
			XMLStreamWriter x = XMLOutputFactory.newInstance().createXMLStreamWriter(saida);
			x.writeStartDocument("UTF-8", "1.0");
			x.writeStartElement("nfeProc");
			x.writeDefaultNamespace(NAMESPACE);
			x.writeAttribute("versao", "4.00");

			x.writeStartElement("NFe");
			x.writeStartElement("infNFe");
			x.writeAttribute("versao", "4.00");
			x.writeAttribute("Id", "NFe" + autorizacao.chave());
			escreverIde(x, nota, emitente, autorizacao);
			escreverEmitente(x, emitente);
			escreverDestinatario(x, nota);
			for (ItemNota item : nota.itens()) {
				escreverItem(x, item);
			}
			escreverTotal(x, nota);
			x.writeStartElement("transp");
			campo(x, "modFrete", "9"); // 9 = sem frete
			x.writeEndElement();
			x.writeEndElement(); // infNFe
			x.writeEndElement(); // NFe

			escreverProtocolo(x, autorizacao);
			x.writeEndElement(); // nfeProc
			x.writeEndDocument();
			x.close();
		}
		catch (XMLStreamException e) {
			throw new IllegalStateException("Erro ao montar o XML da nota " + nota.pedido(), e);
		}
		return indentar(saida.toString());
	}

	private static void escreverIde(XMLStreamWriter x, Nota nota, Emitente emitente, DadosAutorizacao a)
			throws XMLStreamException {
		boolean mesmaUf = emitente.uf().equals(nota.cliente().uf());
		boolean consumidorFinal = nota.cliente().documento().length() == 11; // CPF

		x.writeStartElement("ide");
		campo(x, "cUF", String.valueOf(emitente.codigoUf()));
		campo(x, "cNF", String.format("%08d", a.codigoNumerico()));
		campo(x, "natOp", "Venda de mercadoria");
		campo(x, "mod", "55");
		campo(x, "serie", "1");
		campo(x, "nNF", String.valueOf(a.numero()));
		campo(x, "dhEmi", DATA_HORA.format(a.emitidaEm()));
		campo(x, "tpNF", "1"); // 1 = saída
		campo(x, "idDest", mesmaUf ? "1" : "2"); // 1 = dentro do estado, 2 = interestadual
		campo(x, "tpImp", "1"); // DANFE retrato
		campo(x, "tpEmis", "1");
		campo(x, "cDV", a.chave().substring(43));
		campo(x, "tpAmb", AMBIENTE_HOMOLOGACAO);
		campo(x, "finNFe", "1"); // nota normal
		campo(x, "indFinal", consumidorFinal ? "1" : "0");
		campo(x, "procEmi", "0");
		campo(x, "verProc", "notabot 0.1");
		x.writeEndElement();
	}

	private static void escreverEmitente(XMLStreamWriter x, Emitente e) throws XMLStreamException {
		x.writeStartElement("emit");
		campo(x, "CNPJ", e.cnpj());
		campo(x, "xNome", e.razaoSocial());
		x.writeStartElement("enderEmit");
		campo(x, "xMun", e.municipio());
		campo(x, "UF", e.uf());
		x.writeEndElement();
		campo(x, "IE", e.inscricaoEstadual());
		campo(x, "CRT", "1"); // 1 = Simples Nacional
		x.writeEndElement();
	}

	private static void escreverDestinatario(XMLStreamWriter x, Nota nota) throws XMLStreamException {
		String documento = nota.cliente().documento();
		x.writeStartElement("dest");
		campo(x, documento.length() == 11 ? "CPF" : "CNPJ", documento);
		campo(x, "xNome", nota.cliente().nome());
		x.writeStartElement("enderDest");
		campo(x, "UF", nota.cliente().uf());
		x.writeEndElement();
		campo(x, "indIEDest", "9"); // 9 = não contribuinte
		x.writeEndElement();
	}

	private static void escreverItem(XMLStreamWriter x, ItemNota item) throws XMLStreamException {
		x.writeStartElement("det");
		x.writeAttribute("nItem", String.valueOf(item.numeroItem()));

		x.writeStartElement("prod");
		campo(x, "cProd", item.produtoCodigo());
		campo(x, "cEAN", "SEM GTIN");
		campo(x, "xProd", item.produtoDescricao());
		campo(x, "NCM", item.ncm());
		campo(x, "CFOP", item.cfop());
		campo(x, "uCom", "UN");
		campo(x, "qCom", decimal(item.quantidade(), 4));
		campo(x, "vUnCom", decimal(item.valorUnitario(), 2));
		campo(x, "vProd", decimal(item.valorTotal(), 2));
		campo(x, "cEANTrib", "SEM GTIN");
		campo(x, "uTrib", "UN");
		campo(x, "qTrib", decimal(item.quantidade(), 4));
		campo(x, "vUnTrib", decimal(item.valorUnitario(), 2));
		campo(x, "indTot", "1"); // entra no total da nota
		x.writeEndElement();

		x.writeStartElement("imposto");
		x.writeStartElement("ICMS");
		x.writeStartElement("ICMSSN102"); // Simples Nacional sem destaque de ICMS
		campo(x, "orig", "0"); // 0 = nacional
		campo(x, "CSOSN", "102");
		x.writeEndElement();
		x.writeEndElement();
		x.writeEndElement();

		x.writeEndElement(); // det
	}

	private static void escreverTotal(XMLStreamWriter x, Nota nota) throws XMLStreamException {
		String total = decimal(nota.valorTotal(), 2);
		x.writeStartElement("total");
		x.writeStartElement("ICMSTot");
		campo(x, "vBC", "0.00");
		campo(x, "vICMS", "0.00");
		campo(x, "vProd", total);
		campo(x, "vNF", total);
		x.writeEndElement();
		x.writeEndElement();
	}

	private static void escreverProtocolo(XMLStreamWriter x, DadosAutorizacao a) throws XMLStreamException {
		x.writeStartElement("protNFe");
		x.writeAttribute("versao", "4.00");
		x.writeStartElement("infProt");
		campo(x, "tpAmb", AMBIENTE_HOMOLOGACAO);
		campo(x, "verAplic", "SEFAZ-SIMULADA");
		campo(x, "chNFe", a.chave());
		campo(x, "dhRecbto", DATA_HORA.format(a.autorizadaEm()));
		campo(x, "nProt", a.protocolo());
		campo(x, "cStat", String.valueOf(ResultadoEmissao.AUTORIZADO));
		campo(x, "xMotivo", "Autorizado o uso da NF-e");
		x.writeEndElement();
		x.writeEndElement();
	}

	private static void campo(XMLStreamWriter x, String nome, String valor) throws XMLStreamException {
		x.writeStartElement(nome);
		x.writeCharacters(valor);
		x.writeEndElement();
	}

	private static String decimal(BigDecimal valor, int casas) {
		return valor.setScale(casas, RoundingMode.HALF_UP).toPlainString();
	}

	/** A NF-e de verdade vai numa linha só; aqui a gente indenta pra dar pra ler o arquivo. */
	private static String indentar(String xml) {
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StringWriter saida = new StringWriter();
			t.transform(new StreamSource(new StringReader(xml)), new StreamResult(saida));
			return saida.toString();
		}
		catch (TransformerException e) {
			throw new IllegalStateException("Erro ao formatar o XML", e);
		}
	}

}
