package com.gabrielsdev6.notabot.planilha;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Monta um .xlsx na memória pros testes. Cada valor vira uma célula do tipo certo:
 * String → texto, Number → número, LocalDate → data, null → célula vazia.
 */
public final class PlanilhaTeste {

	private final List<String> cabecalho;
	private final List<List<Object>> linhas = new ArrayList<>();

	private PlanilhaTeste(List<String> cabecalho) {
		this.cabecalho = cabecalho;
	}

	public static PlanilhaTeste comColunasPadrao() {
		return new PlanilhaTeste(ColunasPlanilha.TODAS);
	}

	public static PlanilhaTeste comColunas(String... colunas) {
		return new PlanilhaTeste(List.of(colunas));
	}

	/** Valores na ordem das colunas. */
	public PlanilhaTeste linha(Object... valores) {
		linhas.add(java.util.Arrays.asList(valores));
		return this;
	}

	/** Uma linha válida do pedido informado, com valores padrão. */
	public PlanilhaTeste linhaValida(String pedido, String produto) {
		return linha(pedido, LocalDate.of(2026, 10, 20), "Padaria Pão Quente", "11.222.333/0001-81", "SP",
				produto, "Produto " + produto, "19052090", "5102", 2, 10.5);
	}

	public PlanilhaTeste linhaEmBranco() {
		linhas.add(List.of());
		return this;
	}

	public byte[] bytes() {
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
			Sheet aba = workbook.createSheet();
			CellStyle data = workbook.createCellStyle();
			data.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));

			Row header = aba.createRow(0);
			for (int c = 0; c < cabecalho.size(); c++) {
				header.createCell(c).setCellValue(cabecalho.get(c));
			}
			for (int l = 0; l < linhas.size(); l++) {
				Row row = aba.createRow(l + 1);
				List<Object> valores = linhas.get(l);
				for (int c = 0; c < valores.size(); c++) {
					Object valor = valores.get(c);
					if (valor == null) {
						continue;
					}
					Cell cell = row.createCell(c);
					if (valor instanceof Number n) {
						cell.setCellValue(n.doubleValue());
					}
					else if (valor instanceof LocalDate d) {
						cell.setCellValue(d);
						cell.setCellStyle(data);
					}
					else {
						cell.setCellValue(valor.toString());
					}
				}
			}
			workbook.write(saida);
			return saida.toByteArray();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
