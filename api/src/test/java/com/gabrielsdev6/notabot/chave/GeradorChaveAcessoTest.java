package com.gabrielsdev6.notabot.chave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GeradorChaveAcessoTest {

	private static final String CNPJ = "11222333000181";

	private static DadosChave dados(int uf, int serie, int numero, int cnf) {
		return new DadosChave(uf, YearMonth.of(2026, 10), CNPJ, serie, numero, cnf);
	}

	@Nested
	class DigitoVerificador {

		@Test
		void exemploDoManualDaNfe() {
			assertThat(GeradorChaveAcesso.calcularDigitoVerificador("5206043300991100250655012000000780026730161"))
					.isEqualTo(5);
		}

		@Test
		void restoZeroOuUmViraZero() {
			assertThat(GeradorChaveAcesso.calcularDigitoVerificador("3526101122233300018155001000000001112345678"))
					.isZero();
		}

		@Test
		void outroCaso() {
			assertThat(GeradorChaveAcesso.calcularDigitoVerificador("3326011144477700016155999000004250100000042"))
					.isEqualTo(9);
		}

		@ParameterizedTest
		@ValueSource(strings = { "", "123", "35261011222333000181550010000000011123456789", "352610112223330001815500100000000111234567a" })
		void recusaQuemNaoTem43Digitos(String entrada) {
			assertThatThrownBy(() -> GeradorChaveAcesso.calcularDigitoVerificador(entrada))
					.isInstanceOf(IllegalArgumentException.class);
		}

	}

	@Nested
	class Gerar {

		@Test
		void montaOsCamposNaOrdemCerta() {
			String chave = GeradorChaveAcesso.gerar(dados(35, 1, 1, 12345678));

			assertThat(chave).isEqualTo("35261011222333000181550010000000011123456780");
		}

		@Test
		void temSempre44Digitos() {
			String chave = GeradorChaveAcesso.gerar(dados(35, 1, 1, 12345678));

			assertThat(chave).hasSize(44).containsOnlyDigits();
		}

		@Test
		void completaComZerosAEsquerda() {
			DadosChave d = new DadosChave(33, YearMonth.of(2026, 1), "11444777000161", 999, 4250, 42);

			assertThat(GeradorChaveAcesso.gerar(d)).isEqualTo("33260111444777000161559990000042501000000429");
		}

		@Test
		void maiorNumeroPossivel() {
			String chave = GeradorChaveAcesso.gerar(dados(35, 999, 999_999_999, 99_999_999));

			assertThat(chave).startsWith("352610" + CNPJ + "55" + "999" + "999999999" + "1" + "99999999");
		}

		@Test
		void modeloETipoDeEmissaoSaoFixos() {
			String chave = GeradorChaveAcesso.gerar(dados(35, 1, 1, 12345678));

			assertThat(chave.substring(20, 22)).as("modelo").isEqualTo("55");
			assertThat(chave.substring(34, 35)).as("tpEmis").isEqualTo("1");
		}

	}

	@Nested
	class EntradaInvalida {

		@Test
		void dadosNull() {
			assertThatThrownBy(() -> GeradorChaveAcesso.gerar(null)).isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void anoMesNull() {
			DadosChave d = new DadosChave(35, null, CNPJ, 1, 1, 1);
			assertThatThrownBy(() -> GeradorChaveAcesso.gerar(d)).isInstanceOf(IllegalArgumentException.class);
		}

		@ParameterizedTest
		@ValueSource(ints = { 0, 10, 54, 100 })
		void ufForaDaFaixa(int uf) {
			assertThatThrownBy(() -> GeradorChaveAcesso.gerar(dados(uf, 1, 1, 1)))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@ParameterizedTest
		@ValueSource(strings = { "1122233300018", "112223330001811", "11.222.333/0001-81" })
		void cnpjQueNaoTem14Digitos(String cnpj) {
			DadosChave d = new DadosChave(35, YearMonth.of(2026, 10), cnpj, 1, 1, 1);
			assertThatThrownBy(() -> GeradorChaveAcesso.gerar(d)).isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void errosQueSeCompensamNoTamanhoNaoPassam() {
			DadosChave d = new DadosChave(35, YearMonth.of(2026, 10), "1122233300018", 1, 1, 123_456_789);
			assertThatThrownBy(() -> GeradorChaveAcesso.gerar(d)).isInstanceOf(IllegalArgumentException.class);
		}

		@ParameterizedTest
		@ValueSource(ints = { 0, 1000 })
		void serieForaDaFaixa(int serie) {
			assertThatThrownBy(() -> GeradorChaveAcesso.gerar(dados(35, serie, 1, 1)))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@ParameterizedTest
		@ValueSource(ints = { 0, -1, 1_000_000_000 })
		void numeroForaDaFaixa(int numero) {
			assertThatThrownBy(() -> GeradorChaveAcesso.gerar(dados(35, 1, numero, 1)))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@ParameterizedTest
		@ValueSource(ints = { -1, 100_000_000 })
		void codigoNumericoForaDaFaixa(int cnf) {
			assertThatThrownBy(() -> GeradorChaveAcesso.gerar(dados(35, 1, 1, cnf)))
					.isInstanceOf(IllegalArgumentException.class);
		}

	}

}
