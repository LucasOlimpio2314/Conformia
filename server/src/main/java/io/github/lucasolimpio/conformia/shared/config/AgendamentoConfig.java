package io.github.lucasolimpio.conformia.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga as tarefas com @Scheduled (ex.: VencimentoAceiteJob). Já vem no Spring, sem dependência nova. */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}
