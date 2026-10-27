package com.gabrielsdev6.notabot.planilha;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ModeloPlanilha {

	private static final Set<String> COLUNAS_TEXTO = Set.of(ColunasPlanilha.PEDIDO, ColunasPlanilha.CLIENTE_DOCUMENTO,
			ColunasPlanilha.PRODUTO_CODIGO, ColunasPlanilha.NCM, ColunasPlanilha.CFOP);

	private static final List<List<Object>> EXEMPLOS = List.of(
			List.of("1001", "Padaria Pão Quente", "11.222.333/0001-81", "SP", "P-01", "Pão francês (kg)", "19052090", "5102", 2.5, 18.90),
			List.of("1001", "Padaria Pão Quente", "11.222.333/0001-81", "SP", "C-07", "Café torrado 500 g", "09012100", "5102", 3, 24.50),
			List.of("1002", "Maria Souza", "529.982.247-25", "RJ", "B-02", "Bolo de cenoura", "19059090", "6102", 1, 42.00));

	private ModeloPlanilha() {
	}

	public static byte[] gerar() {
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
			Sheet aba = workbook.createSheet("notas");

			CellStyle cabecalho = workbook.createCellStyle();
			Font negrito = workbook.createFont();
			negrito.setBold(true);
			cabecalho.setFont(negrito);

			CellStyle texto = workbook.createCellStyle();
			texto.setDataFormat(workbook.createDataFormat().getFormat("@"));

			CellStyle data = workbook.createCellStyle();
			data.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));

			Row linhaCabecalho = aba.createRow(0);
			for (int c = 0; c < ColunasPlanilha.TODAS.size(); c++) {
				String coluna = ColunasPlanilha.TODAS.get(c);
				Cell cell = linhaCabecalho.createCell(c);
				cell.setCellValue(coluna);
				cell.setCellStyle(cabecalho);
				if (COLUNAS_TEXTO.contains(coluna)) {
					aba.setDefaultColumnStyle(c, texto);
				}
			}

			for (int i = 0; i < EXEMPLOS.size(); i++) {
				Row row = aba.createRow(i + 1);
				List<Object> exemplo = EXEMPLOS.get(i);
				Cell cellData = row.createCell(1);
				cellData.setCellValue(LocalDate.now());
				cellData.setCellStyle(data);
				for (int e = 0; e < exemplo.size(); e++) {
					int coluna = e == 0 ? 0 : e + 1;
					Cell cell = row.createCell(coluna);
					if (exemplo.get(e) instanceof Number numero) {
						cell.setCellValue(numero.doubleValue());
					}
					else {
						cell.setCellValue((String) exemplo.get(e));
						cell.setCellStyle(texto);
					}
				}
			}

			for (int c = 0; c < ColunasPlanilha.TODAS.size(); c++) {
				aba.autoSizeColumn(c);
			}
			workbook.write(saida);
			return saida.toByteArray();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
