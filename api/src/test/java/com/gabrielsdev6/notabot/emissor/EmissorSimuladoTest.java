package com.gabrielsdev6.notabot.emissor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.gabrielsdev6.notabot.chave.GeradorChaveAcesso;
import com.gabrielsdev6.notabot.nota.Cliente;
import com.gabrielsdev6.notabot.nota.ItemNota;
import com.gabrielsdev6.notabot.nota.Nota;

class EmissorSimuladoTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-27T17:30:45Z"),
			ZoneId.of("America/Sao_Paulo"));

	private static final RegrasRejeicao SO_DUPLICIDADE = new RegrasRejeicao(CLOCK) {
		@Override
		public Optional<Rejeicao> verificar(Nota nota, Set<String> pedidosAutorizados) {
			return pedidosAutorizados.contains(nota.pedido())
					? Optional.of(new Rejeicao(204, "Duplicidade de NF-e"))
					: Optional.empty();
		}
	};

	private final EmissorSimulado emissor = new EmissorSimulado(SO_DUPLICIDADE, CLOCK, Emitente.FICTICIA,
			new Random(42));

	private static Nota nota(String pedido, String documentoCliente, String uf) {
		List<ItemNota> itens = List.of(
				new ItemNota(1, "P-01", "Pão francês", "19052090", "5102", new BigDecimal("2.5"),
						new BigDecimal("18.90"), new BigDecimal("47.25")),
				new ItemNota(2, "C-07", "Café & cia <500 g>", "09012100", "5102", new BigDecimal("3"),
						new BigDecimal("24.50"), new BigDecimal("73.50")));
		return new Nota(pedido, LocalDate.of(2026, 10, 20), new Cliente("Padaria Pão Quente", documentoCliente, uf),
				itens, new BigDecimal("120.75"));
	}

	private static Document parse(String xml) throws Exception {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
		return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
	}

	private static String texto(Document doc, String tag) {
		return doc.getElementsByTagNameNS(GeradorXmlNfe.NAMESPACE, tag).item(0).getTextContent();
	}

	@Test
	void autorizaComChaveValidaEProtocolo() {
		ResultadoEmissao r = emissor.emitir(nota("1001", "11222333000181", "SP"));

		assertThat(r.status()).isEqualTo(StatusEmissao.AUTORIZADA);
		assertThat(r.codigo()).isEqualTo(100);
		assertThat(r.pedido()).isEqualTo("1001");
		assertThat(r.chave()).hasSize(44).startsWith("352610" + Emitente.FICTICIA.cnpj() + "55001000000001");
		assertThat(GeradorChaveAcesso.calcularDigitoVerificador(r.chave().substring(0, 43)))
				.isEqualTo(r.chave().charAt(43) - '0');
		assertThat(r.protocolo()).isEqualTo("135260000000001").hasSize(15);
		assertThat(r.autorizadaEm()).hasToString("2026-10-27T14:30:45-03:00");
	}

	@Test
	void numeraAsNotasEmSequencia() {
		ResultadoEmissao a = emissor.emitir(nota("1", "11222333000181", "SP"));
		ResultadoEmissao b = emissor.emitir(nota("2", "11222333000181", "SP"));

		assertThat(a.chave().substring(25, 34)).isEqualTo("000000001");
		assertThat(b.chave().substring(25, 34)).isEqualTo("000000002");
	}

	@Test
	void mesmoPedidoDuasVezesEhDuplicidadeENaoGastaNumero() {
		emissor.emitir(nota("1001", "11222333000181", "SP"));
		ResultadoEmissao repetida = emissor.emitir(nota("1001", "11222333000181", "SP"));
		ResultadoEmissao outra = emissor.emitir(nota("1002", "11222333000181", "SP"));

		assertThat(repetida.status()).isEqualTo(StatusEmissao.REJEITADA);
		assertThat(repetida.codigo()).isEqualTo(204);
		assertThat(repetida.chave()).isNull();
		assertThat(repetida.xml()).isNull();
		assertThat(outra.chave().substring(25, 34)).as("a rejeitada não consumiu o número 2").isEqualTo("000000002");
	}

	@Test
	void xmlTemAEstruturaDaNfe() throws Exception {
		ResultadoEmissao r = emissor.emitir(nota("1001", "11222333000181", "SP"));
		Document doc = parse(r.xml());

		assertThat(doc.getDocumentElement().getLocalName()).isEqualTo("nfeProc");
		assertThat(doc.getElementsByTagNameNS(GeradorXmlNfe.NAMESPACE, "infNFe").item(0).getAttributes()
				.getNamedItem("Id").getNodeValue()).isEqualTo("NFe" + r.chave());
		assertThat(texto(doc, "tpAmb")).as("homologação").isEqualTo("2");
		assertThat(texto(doc, "nNF")).isEqualTo("1");
		assertThat(texto(doc, "cDV")).isEqualTo(r.chave().substring(43));
		assertThat(texto(doc, "dhEmi")).isEqualTo("2026-10-20T14:30:45-03:00");
		assertThat(texto(doc, "idDest")).as("SP → SP").isEqualTo("1");
		assertThat(texto(doc, "indFinal")).as("cliente com CNPJ").isEqualTo("0");
		assertThat(doc.getElementsByTagNameNS(GeradorXmlNfe.NAMESPACE, "det").getLength()).isEqualTo(2);
		assertThat(texto(doc, "qCom")).isEqualTo("2.5000");
		assertThat(texto(doc, "vNF")).isEqualTo("120.75");
		assertThat(texto(doc, "CSOSN")).isEqualTo("102");
		assertThat(texto(doc, "chNFe")).isEqualTo(r.chave());
		assertThat(texto(doc, "nProt")).isEqualTo(r.protocolo());
		assertThat(texto(doc, "cStat")).isEqualTo("100");
	}

	@Test
	void escapaCaracteresEspeciaisDoXml() throws Exception {
		ResultadoEmissao r = emissor.emitir(nota("1001", "11222333000181", "SP"));

		assertThat(r.xml()).contains("Café &amp; cia &lt;500 g&gt;");
		assertThat(parse(r.xml()).getElementsByTagNameNS(GeradorXmlNfe.NAMESPACE, "xProd").item(1).getTextContent())
				.isEqualTo("Café & cia <500 g>");
	}

	@Test
	void clienteComCpfDeOutroEstado() throws Exception {
		ResultadoEmissao r = emissor.emitir(nota("1001", "52998224725", "RJ"));
		Document doc = parse(r.xml());

		assertThat(texto(doc, "CPF")).isEqualTo("52998224725");
		assertThat(texto(doc, "idDest")).as("SP → RJ").isEqualTo("2");
		assertThat(texto(doc, "indFinal")).as("pessoa física").isEqualTo("1");
	}

}
