package io.github.lucasolimpio.conformia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste de integração: sobe a aplicação inteira contra o banco conformia_test.
 * Se falhar, o problema é de configuração (banco, Flyway, beans) e não de regra de negócio.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServerApplicationIT {

    @Test
    void contextoSobeComBancoDeTeste() {
    }

}
