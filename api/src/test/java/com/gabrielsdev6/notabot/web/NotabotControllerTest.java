package com.gabrielsdev6.notabot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gabrielsdev6.notabot.emissor.Rejeicao;
import com.gabrielsdev6.notabot.emissor.RegrasRejeicao;
import com.gabrielsdev6.notabot.nota.Nota;
import com.gabrielsdev6.notabot.planilha.PlanilhaTeste;

@SpringBootTest
@AutoConfigureMockMvc
class NotabotControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RegrasRejeicao regras;

	@BeforeEach
	void soDuplicidade() {
		when(regras.verificar(any(Nota.class), anySet())).thenAnswer(chamada -> {
			Nota nota = chamada.getArgument(0);
			Set<String> autorizados = chamada.getArgument(1);
			return autorizados.contains(nota.pedido())
					? Optional.of(new Rejeicao(204, "Duplicidade de NF-e"))
					: Optional.empty();
		});
	}

	private static MockMultipartFile planilha(PlanilhaTeste p) {
		return new MockMultipartFile("arquivo", "vendas.xlsx", "application/octet-stream", p.bytes());
	}

	private static String pedido(String sufixo) {
		return Long.toString(System.nanoTime()) + "-" + sufixo;
	}

	private String enviar(PlanilhaTeste p) throws Exception {
		String json = mvc.perform(multipart("/api/lotes/preview").file(planilha(p)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return com.jayway.jsonpath.JsonPath.read(json, "$.loteId");
	}

	@Test
	void previewAgrupaAsNotasEMostraOsErrosPorLinha() throws Exception {
		String p1 = pedido("a");
		String p2 = pedido("b");
		PlanilhaTeste p = PlanilhaTeste.comColunasPadrao()
				.linhaValida(p1, "A")
				.linhaValida(p1, "B")
				.linha(p2, LocalDate.of(2026, 10, 20), "Cliente", "123", "SP", "X", "Produto X", "19052090", "5102",
						1, 10)
				.linhaValida(p2, "C");

		mvc.perform(multipart("/api/lotes/preview").file(planilha(p)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.loteId").isNotEmpty())
				.andExpect(jsonPath("$.notas", hasSize(2)))
				.andExpect(jsonPath("$.notas[0].pedido").value(p1))
				.andExpect(jsonPath("$.notas[0].itens", hasSize(2)))
				.andExpect(jsonPath("$.notas[0].valorTotal").value(42.0))
				.andExpect(jsonPath("$.notas[0].dataEmissao").value("2026-10-20"))
				.andExpect(jsonPath("$.notas[1].pedido").value(p2))
				.andExpect(jsonPath("$.erros", hasSize(1)))
				.andExpect(jsonPath("$.erros[0].linha").value(4))
				.andExpect(jsonPath("$.erros[0].campo").value("cliente_documento"));
	}

	@Test
	void emitirAutorizaEDepoisDaDuplicidade() throws Exception {
		String lote = enviar(PlanilhaTeste.comColunasPadrao().linhaValida(pedido("x"), "A").linhaValida(pedido("y"), "B"));

		mvc.perform(post("/api/lotes/{id}/emitir", lote))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.autorizadas").value(2))
				.andExpect(jsonPath("$.rejeitadas").value(0))
				.andExpect(jsonPath("$.notas[0].status").value("AUTORIZADA"))
				.andExpect(jsonPath("$.notas[0].codigo").value(100))
				.andExpect(jsonPath("$.notas[0].chave").value(org.hamcrest.Matchers.matchesPattern("\\d{44}")))
				.andExpect(jsonPath("$.notas[0].xml").doesNotExist());

		mvc.perform(post("/api/lotes/{id}/emitir", lote))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.autorizadas").value(0))
				.andExpect(jsonPath("$.notas[0].status").value("REJEITADA"))
				.andExpect(jsonPath("$.notas[0].codigo").value(204))
				.andExpect(jsonPath("$.notas[0].motivo").value("Duplicidade de NF-e"));
	}

	@Test
	void zipContinuaComAsNotasDepoisDeUmaEmissaoRejeitada() throws Exception {
		String lote = enviar(PlanilhaTeste.comColunasPadrao().linhaValida(pedido("r"), "A"));
		mvc.perform(post("/api/lotes/{id}/emitir", lote));
		mvc.perform(post("/api/lotes/{id}/emitir", lote)).andExpect(jsonPath("$.rejeitadas").value(1));

		byte[] zip = mvc.perform(get("/api/lotes/{id}/zip", lote))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsByteArray();
		assertThat(nomesNoZip(zip)).hasSize(2);
	}

	@Test
	void baixaXmlPdfEZipDaNotaAutorizada() throws Exception {
		String pedido = pedido("z");
		String lote = enviar(PlanilhaTeste.comColunasPadrao().linhaValida(pedido, "A"));
		String json = mvc.perform(post("/api/lotes/{id}/emitir", lote)).andReturn().getResponse().getContentAsString();
		String chave = com.jayway.jsonpath.JsonPath.read(json, "$.notas[0].chave");

		mvc.perform(get("/api/notas/{chave}/xml", chave))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/xml")))
				.andExpect(header().string("Content-Disposition", containsString(chave + "-nfe.xml")))
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("<chNFe>" + chave + "</chNFe>"));

		mvc.perform(get("/api/notas/{chave}/pdf", chave))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"))
				.andExpect(result -> assertThat(new String(result.getResponse().getContentAsByteArray(), 0, 5))
						.isEqualTo("%PDF-"));

		byte[] zip = mvc.perform(get("/api/lotes/{id}/zip", lote))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsByteArray();
		assertThat(nomesNoZip(zip)).containsExactlyInAnyOrder(
				"pedido-" + pedido + "-" + chave + ".xml",
				"pedido-" + pedido + "-" + chave + ".pdf");
	}

	@Test
	void modeloEhUmaPlanilhaQueOProprioSistemaAceita() throws Exception {
		byte[] modelo = mvc.perform(get("/api/modelo"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", containsString("modelo-notabot.xlsx")))
				.andReturn().getResponse().getContentAsByteArray();

		mvc.perform(multipart("/api/lotes/preview")
				.file(new MockMultipartFile("arquivo", "modelo.xlsx", "application/octet-stream", modelo)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.notas", hasSize(2)))
				.andExpect(jsonPath("$.erros", hasSize(0)));
	}

	@Test
	void arquivoQueNaoEhPlanilhaDa400() throws Exception {
		mvc.perform(multipart("/api/lotes/preview")
				.file(new MockMultipartFile("arquivo", "vendas.csv", "text/csv", "a;b\n1;2".getBytes())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value(containsString(".xlsx")));
	}

	@Test
	void planilhaSoComCabecalhoDa400() throws Exception {
		mvc.perform(multipart("/api/lotes/preview").file(planilha(PlanilhaTeste.comColunasPadrao())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value(containsString("nenhuma linha")));
	}

	@Test
	void semOArquivoDa400() throws Exception {
		mvc.perform(multipart("/api/lotes/preview"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value(containsString("arquivo")));
	}

	@Test
	void loteOuChaveQueNaoExisteDa404() throws Exception {
		mvc.perform(post("/api/lotes/nao-existe/emitir"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensagem").value(containsString("Lote não encontrado")));
		mvc.perform(get("/api/notas/{chave}/pdf", "3".repeat(44)))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/lotes/nao-existe/zip"))
				.andExpect(status().isNotFound());
	}

	@Test
	void zipDeLoteAindaNaoEmitidoDa404() throws Exception {
		String lote = enviar(PlanilhaTeste.comColunasPadrao().linhaValida(pedido("w"), "A"));

		mvc.perform(get("/api/lotes/{id}/zip", lote))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensagem").value(containsString("nenhuma nota autorizada")));
	}

	@Test
	void corsLiberaOFrontDoNext() throws Exception {
		mvc.perform(options("/api/lotes/preview")
				.header("Origin", "http://localhost:3000")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

		mvc.perform(options("/api/lotes/preview")
				.header("Origin", "http://site-estranho.com")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden());
	}

	private static List<String> nomesNoZip(byte[] zip) throws Exception {
		List<String> nomes = new ArrayList<>();
		try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
			ZipEntry entrada;
			while ((entrada = in.getNextEntry()) != null) {
				nomes.add(entrada.getName());
			}
		}
		return nomes;
	}

}
