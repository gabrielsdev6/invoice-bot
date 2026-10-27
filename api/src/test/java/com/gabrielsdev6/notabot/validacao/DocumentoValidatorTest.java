package com.gabrielsdev6.notabot.validacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentoValidatorTest {

	@Nested
	class SomenteDigitos {

		@Test
		void tiraPontosTracosEBarras() {
			assertThat(DocumentoValidator.somenteDigitos("11.222.333/0001-81")).isEqualTo("11222333000181");
		}

		@Test
		void tiraEspacosELetras() {
			assertThat(DocumentoValidator.somenteDigitos(" cpf: 529 982 247-25 ")).isEqualTo("52998224725");
		}

		@Test
		void nullViraVazio() {
			assertThat(DocumentoValidator.somenteDigitos(null)).isEmpty();
		}

	}

	@Nested
	class Cpf {

		@ParameterizedTest
		@ValueSource(strings = { "529.982.247-25", "52998224725", "111.444.777-35" })
		void validos(String cpf) {
			assertThat(DocumentoValidator.isCpfValido(cpf)).isTrue();
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"529.982.247-24",
				"529.982.247-15",
				"111.111.111-11",
				"000.000.000-00",
				"5299822472",
				"529982247250",
				"abc" })
		void invalidos(String cpf) {
			assertThat(DocumentoValidator.isCpfValido(cpf)).isFalse();
		}

		@ParameterizedTest
		@NullAndEmptySource
		void nuloOuVazio(String cpf) {
			assertThat(DocumentoValidator.isCpfValido(cpf)).isFalse();
		}

	}

	@Nested
	class Cnpj {

		@ParameterizedTest
		@ValueSource(strings = { "11.222.333/0001-81", "11222333000181", "11.444.777/0001-61" })
		void validos(String cnpj) {
			assertThat(DocumentoValidator.isCnpjValido(cnpj)).isTrue();
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"11.222.333/0001-80",
				"11.222.333/0001-91",
				"00.000.000/0000-00",
				"22.222.222/2222-22",
				"1122233300018",
				"52998224725" })
		void invalidos(String cnpj) {
			assertThat(DocumentoValidator.isCnpjValido(cnpj)).isFalse();
		}

		@ParameterizedTest
		@NullAndEmptySource
		void nuloOuVazio(String cnpj) {
			assertThat(DocumentoValidator.isCnpjValido(cnpj)).isFalse();
		}

	}

}
