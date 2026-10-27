package com.gabrielsdev6.notabot.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import com.gabrielsdev6.notabot.danfe.GeradorDanfe;
import com.gabrielsdev6.notabot.emissor.Emissor;
import com.gabrielsdev6.notabot.emissor.Emitente;
import com.gabrielsdev6.notabot.emissor.ResultadoEmissao;
import com.gabrielsdev6.notabot.nota.AgrupadorPedidos;
import com.gabrielsdev6.notabot.nota.Nota;
import com.gabrielsdev6.notabot.nota.ResultadoAgrupamento;
import com.gabrielsdev6.notabot.planilha.ErroLinha;
import com.gabrielsdev6.notabot.planilha.LeitorPlanilha;
import com.gabrielsdev6.notabot.planilha.LinhaBruta;
import com.gabrielsdev6.notabot.planilha.PlanilhaInvalidaException;
import com.gabrielsdev6.notabot.planilha.ResultadoValidacao;
import com.gabrielsdev6.notabot.planilha.ValidadorLinha;

@Service
public class LoteService {

	private final ValidadorLinha validador;
	private final Emissor emissor;

	private final Map<String, Lote> lotes = new ConcurrentHashMap<>();
	private final Map<String, NotaEmitida> notasPorChave = new ConcurrentHashMap<>();

	public LoteService(ValidadorLinha validador, Emissor emissor) {
		this.validador = validador;
		this.emissor = emissor;
	}

	Lote preview(InputStream planilha) {
		List<LinhaBruta> brutas = LeitorPlanilha.ler(planilha);
		if (brutas.isEmpty()) {
			throw new PlanilhaInvalidaException("A planilha não tem nenhuma linha preenchida além do cabeçalho.");
		}
		ResultadoValidacao validacao = validador.validar(brutas);
		ResultadoAgrupamento agrupamento = AgrupadorPedidos.agrupar(validacao.linhas());

		List<ErroLinha> erros = new ArrayList<>(validacao.erros());
		erros.addAll(agrupamento.erros());
		erros.sort(Comparator.comparingInt(ErroLinha::linha));

		Lote lote = new Lote(UUID.randomUUID().toString(), agrupamento.notas(), erros);
		lotes.put(lote.id(), lote);
		return lote;
	}

	List<ResultadoEmissao> emitir(String loteId) {
		Lote lote = buscarLote(loteId);
		List<ResultadoEmissao> resultados = new ArrayList<>();
		for (Nota nota : lote.notas()) {
			ResultadoEmissao resultado = emissor.emitir(nota);
			if (resultado.isAutorizada()) {
				notasPorChave.put(resultado.chave(), new NotaEmitida(nota, resultado));
			}
			resultados.add(resultado);
		}
		lote.registrar(resultados);
		return resultados;
	}

	String xml(String chave) {
		return buscarNota(chave).resultado().xml();
	}

	byte[] danfe(String chave) {
		NotaEmitida emitida = buscarNota(chave);
		return GeradorDanfe.gerar(emitida.nota(), emitida.resultado(), Emitente.FICTICIA);
	}

	byte[] zip(String loteId) {
		Lote lote = buscarLote(loteId);
		List<NotaEmitida> autorizadas = lote.chavesAutorizadas().stream().map(notasPorChave::get).toList();
		if (autorizadas.isEmpty()) {
			throw new NaoEncontradoException("O lote " + loteId + " ainda não tem nenhuma nota autorizada.");
		}

		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(saida)) {
			for (NotaEmitida n : autorizadas) {
				String nome = "pedido-" + n.nota().pedido() + "-" + n.resultado().chave();
				adicionar(zip, nome + ".xml", n.resultado().xml().getBytes(StandardCharsets.UTF_8));
				adicionar(zip, nome + ".pdf", GeradorDanfe.gerar(n.nota(), n.resultado(), Emitente.FICTICIA));
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return saida.toByteArray();
	}

	private static void adicionar(ZipOutputStream zip, String nome, byte[] conteudo) throws IOException {
		zip.putNextEntry(new ZipEntry(nome));
		zip.write(conteudo);
		zip.closeEntry();
	}

	private Lote buscarLote(String id) {
		Lote lote = lotes.get(id);
		if (lote == null) {
			throw new NaoEncontradoException("Lote não encontrado: " + id
					+ ". Se a API reiniciou, envie a planilha de novo.");
		}
		return lote;
	}

	private NotaEmitida buscarNota(String chave) {
		NotaEmitida nota = notasPorChave.get(chave);
		if (nota == null) {
			throw new NaoEncontradoException("Nenhuma nota autorizada com a chave " + chave + ".");
		}
		return nota;
	}

	private record NotaEmitida(Nota nota, ResultadoEmissao resultado) {
	}

}
