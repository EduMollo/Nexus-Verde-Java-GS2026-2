package br.com.fiap.nexusverde.iot;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:reservatorio;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","nexus.equipe-email=reservatorio@example.test","nexus.equipe-senha=SenhaLocalTeste123"})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class TelemetriaIntegracaoTest {
    @Autowired JdbcTemplate db;
    @Autowired Telemetria leituras;
    @Autowired MockMvc mvc;

    @Test void entregaDuplicatasAtrasosELimites() throws Exception {
        long ponto=db.queryForObject("SELECT min(id) FROM pontos_agua",Long.class);
        String chave=leituras.parear(ponto);
        var id=UUID.randomUUID();
        var hora=Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String url="/api/iot/pontos/"+ponto+"/leituras";
        String body=corpo(id,hora,5);
        mvc.perform(post(url).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        for(int tentativa=0; tentativa<2; tentativa++)
            mvc.perform(post(url).header("X-Sensor-Key",chave).contentType("application/json").content(body))
                    .andExpect(status().isNoContent());
        mvc.perform(post(url).header("X-Sensor-Key",chave).contentType("application/json")
                .content(corpo(UUID.randomUUID(),hora.minusSeconds(5),6))).andExpect(status().isConflict());
        mvc.perform(post(url).header("X-Sensor-Key",chave).contentType("application/json")
                .content(corpo(UUID.randomUUID(),hora,101))).andExpect(status().isBadRequest());
        assertThat(leituras.ultima(ponto)).get().extracting(Telemetria.Leitura::evento).isEqualTo(id);
        leituras.parear(ponto);
        mvc.perform(post(url).header("X-Sensor-Key",chave).contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    private String corpo(UUID id,Instant h,int nivel) {
        return "{\"evento\":\""+id+"\",\"medidaEm\":\""+h+"\",\"percentual\":"+nivel+"}";
    }
}
