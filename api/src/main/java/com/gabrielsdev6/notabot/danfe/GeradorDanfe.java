package com.gabrielsdev6.notabot.danfe;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.Barcode;
import org.openpdf.text.pdf.Barcode128;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.GrayColor;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import com.gabrielsdev6.notabot.emissor.Emitente;
import com.gabrielsdev6.notabot.emissor.ResultadoEmissao;
import com.gabrielsdev6.notabot.nota.ItemNota;
import com.gabrielsdev6.notabot.nota.Nota;

public final class GeradorDanfe {

	private static final Locale PT_BR = Locale.of("pt", "BR");
	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private static final Font TITULO = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
	private static final Font ROTULO = new Font(Font.HELVETICA, 6, Font.NORMAL, Color.DARK_GRAY);
	private static final Font VALOR = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
	private static final Font VALOR_NEGRITO = new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK);
	private static final Font MARCA_DAGUA = new Font(Font.HELVETICA, 42, Font.BOLD, new GrayColor(0.85f));

	private GeradorDanfe() {
	}

	public static byte[] gerar(Nota nota, ResultadoEmissao resultado, Emitente emitente) {
		if (!resultado.isAutorizada()) {
			throw new IllegalArgumentException("Só existe DANFE de nota autorizada (pedido " + nota.pedido() + ")");
		}
		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		Document documento = new Document(PageSize.A4, 28, 28, 28, 28);
		try {
			PdfWriter writer = PdfWriter.getInstance(documento, saida);
			documento.open();
			marcaDagua(writer.getDirectContentUnder());

			documento.add(cabecalho(nota, resultado, emitente, writer.getDirectContent()));
			documento.add(espaco());
			documento.add(destinatario(nota));
			documento.add(espaco());
			documento.add(itens(nota));
			documento.add(espaco());
			documento.add(totais(nota));
			documento.add(espaco());
			documento.add(new Paragraph("Emitida em ambiente de HOMOLOGAÇÃO pelo notabot (projeto de estudo). "
					+ "Esta nota não tem valor fiscal.", ROTULO));
		}
		catch (DocumentException e) {
			throw new IllegalStateException("Erro ao gerar a DANFE do pedido " + nota.pedido(), e);
		}
		finally {
			if (documento.isOpen()) {
				documento.close();
			}
		}
		return saida.toByteArray();
	}

	private static PdfPTable cabecalho(Nota nota, ResultadoEmissao r, Emitente emitente, PdfContentByte cb) {
		PdfPTable t = tabela(new float[] { 3.4f, 1.8f, 4.8f });

		PdfPCell emit = celula();
		emit.addElement(new Paragraph(emitente.razaoSocial(), VALOR_NEGRITO));
		emit.addElement(new Paragraph(emitente.municipio() + " - " + emitente.uf(), VALOR));
		emit.addElement(new Paragraph("CNPJ " + formatarDocumento(emitente.cnpj()), VALOR));
		emit.addElement(new Paragraph("IE " + emitente.inscricaoEstadual(), VALOR));
		t.addCell(emit);

		PdfPCell danfe = celula();
		danfe.setHorizontalAlignment(Element.ALIGN_CENTER);
		Paragraph titulo = new Paragraph("DANFE", TITULO);
		titulo.setAlignment(Element.ALIGN_CENTER);
		danfe.addElement(titulo);
		Paragraph sub = new Paragraph("Documento Auxiliar da Nota Fiscal Eletrônica\n1 - SAÍDA", ROTULO);
		sub.setAlignment(Element.ALIGN_CENTER);
		danfe.addElement(sub);
		Paragraph numero = new Paragraph("Nº " + numeroFormatado(r.chave()) + "\nSÉRIE 001", VALOR_NEGRITO);
		numero.setAlignment(Element.ALIGN_CENTER);
		danfe.addElement(numero);
		t.addCell(danfe);

		PdfPCell chave = celula();
		chave.addElement(codigoDeBarras(r.chave(), cb));
		chave.addElement(new Paragraph("CHAVE DE ACESSO", ROTULO));
		chave.addElement(new Paragraph(formatarChave(r.chave()), VALOR_NEGRITO));
		chave.addElement(new Paragraph("PROTOCOLO DE AUTORIZAÇÃO", ROTULO));
		chave.addElement(new Paragraph(r.protocolo() + " - " + DATA_HORA.format(r.autorizadaEm()), VALOR));
		t.addCell(chave);

		PdfPCell natOp = campo("NATUREZA DA OPERAÇÃO", "Venda de mercadoria");
		natOp.setColspan(2);
		t.addCell(natOp);
		t.addCell(campo("PEDIDO", nota.pedido()));
		return t;
	}

	private static PdfPTable destinatario(Nota nota) {
		PdfPTable t = tabela(new float[] { 5, 3, 1, 2 });
		PdfPCell titulo = new PdfPCell(new Phrase("DESTINATÁRIO", VALOR_NEGRITO));
		titulo.setColspan(4);
		titulo.setBorder(Rectangle.NO_BORDER);
		t.addCell(titulo);
		t.addCell(campo("NOME / RAZÃO SOCIAL", nota.cliente().nome()));
		t.addCell(campo(nota.cliente().documento().length() == 11 ? "CPF" : "CNPJ",
				formatarDocumento(nota.cliente().documento())));
		t.addCell(campo("UF", nota.cliente().uf()));
		t.addCell(campo("DATA DE EMISSÃO", DATA.format(nota.dataEmissao())));
		return t;
	}

	private static PdfPTable itens(Nota nota) {
		String[] colunas = { "#", "CÓDIGO", "DESCRIÇÃO", "NCM", "CSOSN", "CFOP", "QTD", "VL. UNIT.", "VL. TOTAL" };
		PdfPTable t = tabela(new float[] { 0.5f, 1.3f, 4.5f, 1.2f, 0.9f, 0.8f, 1, 1.3f, 1.4f });
		t.setHeaderRows(2);

		PdfPCell titulo = new PdfPCell(new Phrase("DADOS DOS PRODUTOS", VALOR_NEGRITO));
		titulo.setColspan(colunas.length);
		titulo.setBorder(Rectangle.NO_BORDER);
		t.addCell(titulo);
		for (String coluna : colunas) {
			PdfPCell c = new PdfPCell(new Phrase(coluna, ROTULO));
			c.setBackgroundColor(new GrayColor(0.92f));
			c.setPadding(3);
			t.addCell(c);
		}
		for (ItemNota item : nota.itens()) {
			t.addCell(texto(String.valueOf(item.numeroItem()), Element.ALIGN_CENTER));
			t.addCell(texto(item.produtoCodigo(), Element.ALIGN_LEFT));
			t.addCell(texto(item.produtoDescricao(), Element.ALIGN_LEFT));
			t.addCell(texto(item.ncm(), Element.ALIGN_CENTER));
			t.addCell(texto("102", Element.ALIGN_CENTER));
			t.addCell(texto(item.cfop(), Element.ALIGN_CENTER));
			t.addCell(texto(numero(item.quantidade(), 0, 4), Element.ALIGN_RIGHT));
			t.addCell(texto(numero(item.valorUnitario(), 2, 2), Element.ALIGN_RIGHT));
			t.addCell(texto(numero(item.valorTotal(), 2, 2), Element.ALIGN_RIGHT));
		}
		return t;
	}

	private static PdfPTable totais(Nota nota) {
		PdfPTable t = tabela(new float[] { 2, 2, 2, 2 });
		t.addCell(campo("BASE DE CÁLCULO DO ICMS", "0,00"));
		t.addCell(campo("VALOR DO ICMS", "0,00"));
		t.addCell(campo("VALOR TOTAL DOS PRODUTOS", numero(nota.valorTotal(), 2, 2)));
		PdfPCell total = campo("VALOR TOTAL DA NOTA", "R$ " + numero(nota.valorTotal(), 2, 2));
		total.setBackgroundColor(new GrayColor(0.92f));
		t.addCell(total);
		return t;
	}

	private static Image codigoDeBarras(String chave, PdfContentByte cb) {
		Barcode128 barras = new Barcode128();
		barras.setCodeType(Barcode.CODE128);
		barras.setCode(chave);
		barras.setBarHeight(28);
		barras.setX(0.62f);
		barras.setFont(null);
		Image imagem = barras.createImageWithBarcode(cb, null, null);
		imagem.setAlignment(Element.ALIGN_CENTER);
		return imagem;
	}

	private static void marcaDagua(PdfContentByte cb) {
		Rectangle a4 = PageSize.A4;
		ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase("SEM VALOR FISCAL", MARCA_DAGUA),
				a4.getWidth() / 2, a4.getHeight() / 2, 45);
	}

	private static PdfPTable tabela(float[] larguras) {
		PdfPTable t = new PdfPTable(larguras);
		t.setWidthPercentage(100);
		return t;
	}

	private static PdfPCell celula() {
		PdfPCell c = new PdfPCell();
		c.setPadding(4);
		return c;
	}

	private static PdfPCell campo(String rotulo, String valor) {
		PdfPCell c = celula();
		c.addElement(new Paragraph(rotulo, ROTULO));
		c.addElement(new Paragraph(valor, VALOR));
		return c;
	}

	private static PdfPCell texto(String valor, int alinhamento) {
		PdfPCell c = new PdfPCell(new Phrase(valor, VALOR));
		c.setHorizontalAlignment(alinhamento);
		c.setPadding(3);
		return c;
	}

	private static Paragraph espaco() {
		return new Paragraph(" ", ROTULO);
	}

	static String formatarChave(String chave) {
		return chave.replaceAll("(\\d{4})(?=\\d)", "$1 ");
	}

	private static String numeroFormatado(String chave) {
		String n = chave.substring(25, 34);
		return n.substring(0, 3) + "." + n.substring(3, 6) + "." + n.substring(6);
	}

	static String formatarDocumento(String digitos) {
		if (digitos.length() == 11) {
			return digitos.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
		}
		return digitos.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
	}

	private static String numero(BigDecimal valor, int minimoCasas, int maximoCasas) {
		NumberFormat f = NumberFormat.getNumberInstance(PT_BR);
		f.setMinimumFractionDigits(minimoCasas);
		f.setMaximumFractionDigits(maximoCasas);
		return f.format(valor);
	}

}
