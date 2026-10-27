package com.gabrielsdev6.notabot.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gabrielsdev6.notabot.planilha.ModeloPlanilha;

@RestController
@RequestMapping("/api")
class NotabotController {

	private static final MediaType XLSX = MediaType
			.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final LoteService service;

	NotabotController(LoteService service) {
		this.service = service;
	}

	@GetMapping("/modelo")
	ResponseEntity<byte[]> modelo() {
		return arquivo(ModeloPlanilha.gerar(), XLSX, "modelo-notabot.xlsx");
	}

	@PostMapping("/lotes/preview")
	Respostas.Preview preview(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
		try (InputStream conteudo = arquivo.getInputStream()) {
			return Respostas.Preview.de(service.preview(conteudo));
		}
	}

	@PostMapping("/lotes/{loteId}/emitir")
	Respostas.Emissao emitir(@PathVariable String loteId) {
		return Respostas.Emissao.de(service.emitir(loteId));
	}

	@GetMapping("/lotes/{loteId}/zip")
	ResponseEntity<byte[]> zip(@PathVariable String loteId) {
		return arquivo(service.zip(loteId), MediaType.parseMediaType("application/zip"), "notas-" + loteId + ".zip");
	}

	@GetMapping("/notas/{chave}/xml")
	ResponseEntity<byte[]> xml(@PathVariable String chave) {
		byte[] xml = service.xml(chave).getBytes(StandardCharsets.UTF_8);
		return arquivo(xml, MediaType.APPLICATION_XML, chave + "-nfe.xml");
	}

	@GetMapping("/notas/{chave}/pdf")
	ResponseEntity<byte[]> pdf(@PathVariable String chave) {
		return arquivo(service.danfe(chave), MediaType.APPLICATION_PDF, chave + "-danfe.pdf");
	}

	private static ResponseEntity<byte[]> arquivo(byte[] conteudo, MediaType tipo, String nome) {
		return ResponseEntity.ok()
				.contentType(tipo)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nome).build().toString())
				.body(conteudo);
	}

}
