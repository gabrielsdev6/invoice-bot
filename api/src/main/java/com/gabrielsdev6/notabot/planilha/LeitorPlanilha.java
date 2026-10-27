package com.gabrielsdev6.notabot.planilha;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public final class LeitorPlanilha {

	private LeitorPlanilha() {
	}

	public static List<LinhaBruta> ler(InputStream arquivo) {
		try (Workbook workbook = abrir(arquivo)) {
			if (workbook.getNumberOfSheets() == 0) {
				throw new PlanilhaInvalidaException("A planilha não tem nenhuma aba.");
			}
			Sheet aba = workbook.getSheetAt(0);
			Map<Integer, String> colunas = lerCabecalho(aba.getRow(0));

			List<LinhaBruta> linhas = new ArrayList<>();
			for (int i = 1; i <= aba.getLastRowNum(); i++) {
				Row row = aba.getRow(i);
				if (row == null) {
					continue;
				}
				Map<String, String> valores = new HashMap<>();
				for (Map.Entry<Integer, String> coluna : colunas.entrySet()) {
					String valor = texto(row.getCell(coluna.getKey()));
					if (!valor.isEmpty()) {
						valores.put(coluna.getValue(), valor);
					}
				}
				if (!valores.isEmpty()) {
					linhas.add(new LinhaBruta(i + 1, valores));
				}
			}
			return linhas;
		}
		catch (PlanilhaInvalidaException e) {
			throw e;
		}
		catch (Exception e) {
			throw new PlanilhaInvalidaException("Não consegui ler a planilha: " + e.getMessage(), e);
		}
	}

	private static Workbook abrir(InputStream arquivo) {
		try {
			return WorkbookFactory.create(arquivo);
		}
		catch (Exception e) {
			throw new PlanilhaInvalidaException("O arquivo não é uma planilha do Excel (.xlsx).", e);
		}
	}

	private static Map<Integer, String> lerCabecalho(Row cabecalho) {
		if (cabecalho == null) {
			throw new PlanilhaInvalidaException("A primeira linha da planilha precisa ter os nomes das colunas.");
		}
		Map<Integer, String> colunas = new LinkedHashMap<>();
		for (Cell cell : cabecalho) {
			String nome = texto(cell).toLowerCase(Locale.ROOT);
			if (ColunasPlanilha.TODAS.contains(nome)) {
				colunas.put(cell.getColumnIndex(), nome);
			}
		}
		List<String> faltando = ColunasPlanilha.TODAS.stream().filter(c -> !colunas.containsValue(c)).toList();
		if (!faltando.isEmpty()) {
			throw new PlanilhaInvalidaException("Faltam colunas na planilha: " + String.join(", ", faltando)
					+ ". Baixe a planilha modelo pra ver o formato.");
		}
		return colunas;
	}

	private static String texto(Cell cell) {
		if (cell == null) {
			return "";
		}
		CellType tipo = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
		return switch (tipo) {
			case STRING -> cell.getStringCellValue().trim();
			case NUMERIC -> DateUtil.isCellDateFormatted(cell)
					? cell.getLocalDateTimeCellValue().toLocalDate().toString()
					: BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
			case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
			default -> "";
		};
	}

}
