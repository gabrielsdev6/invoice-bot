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

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins("http://localhost:3000")
				.allowedMethods("GET", "POST")
				.exposedHeaders("Content-Disposition");
	}

}
