package io.github.lucasolimpio.conformia.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Relógio injetável: nos testes dá para fixar "agora" e testar prazos
 * (ex.: senha temporária expirada) sem esperar 24 horas.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
