package io.github.lucasolimpio.conformia.aceite.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Verificação diária dos aceites vencidos (6h da manhã). Marca como VENCIDO
 * e avisa os Gerentes. A tela de aceites também verifica ao abrir, então
 * nada fica errado se o servidor estava desligado na hora agendada.
 */
@Component
public class VencimentoAceiteJob {

    private static final Logger log = LoggerFactory.getLogger(VencimentoAceiteJob.class);

    private final AceiteService aceiteService;

    public VencimentoAceiteJob(AceiteService aceiteService) {
        this.aceiteService = aceiteService;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void verificar() {
        int vencidos = aceiteService.atualizarVencidos();
        if (vencidos > 0) {
            log.info("{} aceite(s) formal(is) vencido(s) e Gerentes avisados.", vencidos);
        }
    }
}
