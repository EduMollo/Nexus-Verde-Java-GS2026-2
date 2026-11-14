package br.com.fiap.nexusverde.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@EnabledIfEnvironmentVariable(named = "NEXUS_TESTE_RABBIT", matches = "true")
@SpringBootTest(properties = "nexus.eventos-intervalo-ms=300")
@AutoConfigureMockMvc
class MensageriaIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void publicaEventoConfirmadoEConsomeParaTriagem() throws Exception {
        String cadastro = """
                {"nome":"Cidadã Teste","email":"fila-%s@example.com","senha":"TesteFila-2026"}
                """.formatted(java.util.UUID.randomUUID());
        var registrado = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(cadastro))
                .andExpect(status().isCreated()).andReturn();
        String jwt = json.readTree(registrado.getResponse().getContentAsByteArray()).path("token").asText();

        var pedido = mvc.perform(post("/api/solicitacoes").header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                            {"tipo":"FALTA_DE_AGUA","descricao":"Cenário de fila","quantidadePessoas":4,
                            "urgencia":"ALTA","grupoVulneravel":false,"endereco":"Endereço fictício","cidade":"Canoas"}
                            """))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(pedido.getResponse().getContentAsByteArray()).path("id").asLong();

        long limite = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < limite && !triado(id)) {
            Thread.sleep(200L);
        }
        assertThat(triado(id)).as("Evento publicado e pedido recebido na triagem").isTrue();
    }

    private boolean triado(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM solicitacoes s JOIN eventos_pendentes e ON e.solicitacao_id = s.id
                    WHERE s.id = ? AND s.triada_em IS NOT NULL AND e.publicado_em IS NOT NULL
                )
                """, Boolean.class, id));
    }
}
