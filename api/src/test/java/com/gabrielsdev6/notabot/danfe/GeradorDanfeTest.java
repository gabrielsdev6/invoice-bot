package com.gabrielsdev6.notabot.danfe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import com.gabrielsdev6.notabot.emissor.Emitente;
import com.gabrielsdev6.notabot.emissor.Rejeicao;
import com.gabrielsdev6.notabot.emissor.ResultadoEmissao;
import com.gabrielsdev6.notabot.emissor.StatusEmissao;
import com.gabrielsdev6.notabot.nota.Cliente;
import com.gabrielsdev6.notabot.nota.ItemNota;
import com.gabrielsdev6.notabot.nota.Nota;

class GeradorDanfeTest {

	private static final String CHAVE = "35261011444777000161550010000000071123456785";

	private static final Nota NOTA = new Nota("1001", LocalDate.of(2026, 10, 20),
			new Cliente("Padaria Pão Quente", "11222333000181", "SP"),
			List.of(new ItemNota(1, "P-01", "Pão francês (kg)", "19052090", "5102", new BigDecimal("2.5"),
					new BigDecimal("18.90"), new BigDecimal("47.25"))),
			new BigDecimal("1047.25"));

	private static final ResultadoEmissao AUTORIZADA = new ResultadoEmissao("1001", StatusEmissao.AUTORIZADA, 100,
			"Autorizado o uso da NF-e", CHAVE, "135260000000007", OffsetDateTime.parse("2026-10-27T14:30:45-03:00"),
			"<xml/>");

	private static String textoDoPdf(byte[] pdf) throws Exception {
		try (PdfReader reader = new PdfReader(pdf)) {
			return new PdfTextExtractor(reader).getTextFromPage(1);
		}
	}

	@Test
	void geraUmPdf() {
		byte[] pdf = GeradorDanfe.gerar(NOTA, AUTORIZADA, Emitente.FICTICIA);

		assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
	}

	@Test
	void temOsDadosDaNota() throws Exception {
		String texto = textoDoPdf(GeradorDanfe.gerar(NOTA, AUTORIZADA, Emitente.FICTICIA));

		assertThat(texto)
				.contains("DANFE")
				.contains("3526 1011 4447 7700 0161 5500 1000 0000 0711 2345 6785")
				.contains("000.000.007")
				.contains("135260000000007")
				.contains("Padaria Pão Quente")
				.contains("11.222.333/0001-81")
				.contains("Pão francês (kg)")
				.contains("18,90")
				.contains("1.047,25")
				.contains("SEM VALOR FISCAL");
	}

	@Test
	void naoGeraDanfeDeNotaRejeitada() {
		ResultadoEmissao rejeitada = ResultadoEmissao.rejeitada("1001", new Rejeicao(204, "Duplicidade de NF-e"));

		assertThatThrownBy(() -> GeradorDanfe.gerar(NOTA, rejeitada, Emitente.FICTICIA))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void formataChaveEDocumentos() {
		assertThat(GeradorDanfe.formatarChave(CHAVE)).hasSize(54).startsWith("3526 1011 ").endsWith(" 6785");
		assertThat(GeradorDanfe.formatarDocumento("52998224725")).isEqualTo("529.982.247-25");
		assertThat(GeradorDanfe.formatarDocumento("11222333000181")).isEqualTo("11.222.333/0001-81");
	}

}
