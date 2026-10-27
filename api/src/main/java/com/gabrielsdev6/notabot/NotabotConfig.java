package com.gabrielsdev6.notabot;

import java.time.Clock;
import java.time.ZoneId;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gabrielsdev6.notabot.emissor.Emissor;
import com.gabrielsdev6.notabot.emissor.EmissorSimulado;
import com.gabrielsdev6.notabot.emissor.Emitente;
import com.gabrielsdev6.notabot.emissor.RegrasRejeicao;
import com.gabrielsdev6.notabot.planilha.ValidadorLinha;

/**
 * Monta as peças do sistema. As classes de regra não sabem nada do Spring:
 * é aqui que elas recebem o relógio, o emitente etc.
 */
@Configuration
public class NotabotConfig implements WebMvcConfigurer {

	@Bean
	Clock clock() {
		return Clock.system(ZoneId.of("America/Sao_Paulo"));
	}

	@Bean
	ValidadorLinha validadorLinha(Clock clock) {
		return new ValidadorLinha(clock);
	}

	@Bean
	RegrasRejeicao regrasRejeicao(Clock clock) {
		return new RegrasRejeicao(clock);
	}

	@Bean
	Emissor emissor(RegrasRejeicao regras, Clock clock) {
		return new EmissorSimulado(regras, clock, Emitente.FICTICIA, RandomGenerator.getDefault());
	}

	/** Deixa o front em Next (localhost:3000) chamar a API e ler o nome dos arquivos baixados. */
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins("http://localhost:3000")
				.allowedMethods("GET", "POST")
				.exposedHeaders("Content-Disposition");
	}

}
