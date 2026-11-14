package br.com.fiap.nexusverde;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "nexus.equipe-email=equipe-test@example.com",
    "nexus.equipe-senha=Equipe-Teste-2026"
})
@AutoConfigureMockMvc
class ApiMobileTest {
    private static final String PEDIDO = """
            {
              "tipo":"FALTA_DE_AGUA","descricao":"Sem abastecimento","quantidadePessoas":5,
              "urgencia":"ALTA","grupoVulneravel":true,"endereco":"Endereço fictício",
              "cidade":"Canoas","latitude":-29.91,"longitude":-51.18
            }
            """;
    private static final String REGIAO = """
            {
              "nome":"Região de teste","cidade":"Canoas","nivelRisco":"CRITICO",
              "situacao":"Abastecimento interrompido","populacaoAfetada":100
            }
            """;
    private static final String PONTO = """
            {
              "nome":"Posto de teste","tipo":"POSTO_DISTRIBUICAO","endereco":"Endereço fictício",
              "cidade":"Canoas","latitude":-29.91,"longitude":-51.18,"status":"DISPONIVEL",
              "capacidadeLitros":3000,"estoqueLitros":1000,"horarioFuncionamento":"08h às 20h"
            }
            """;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void loginCadastroETratamentoDeErros() throws Exception {
        mvc.perform(get("/api/regioes"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").isString());
        mvc.perform(get("/api/pontos-agua").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").isString());

        cadastrar("maria-auth@example.com");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro("maria-auth@example.com")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").isString());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"maria-auth@example.com","senha":"incorreta"}
                                """))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").isString());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").isString());
        equipe();
    }

    @Test
    void cidadaoGerenciaSeusPedidosEAEquipeAvancaOStatus() throws Exception {
        String cidadao = cadastrar("dono@example.com");
        String outro = cadastrar("outro@example.com");
        String equipe = equipe();
        long primeiro = criarPedido(cidadao);
        long segundo = criarPedido(cidadao);

        mvc.perform(get("/api/solicitacoes").header("Authorization", cidadao).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/solicitacoes").header("Authorization", outro))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/solicitacoes/{id}", primeiro).header("Authorization", outro))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/solicitacoes/{id}", primeiro).header("Authorization", equipe))
                .andExpect(status().isOk()).andExpect(jsonPath("$.solicitante.nome").value("Maria Souza"));
        mvc.perform(put("/api/solicitacoes/{id}", primeiro).header("Authorization", cidadao)
                        .contentType(MediaType.APPLICATION_JSON).content(PEDIDO.replace("Sem abastecimento", "Atualizado")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.descricao").value("Atualizado"));
        mvc.perform(delete("/api/solicitacoes/{id}", segundo).header("Authorization", cidadao))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/solicitacoes/{id}", segundo).header("Authorization", equipe))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/solicitacoes/{id}/status", primeiro).header("Authorization", cidadao)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EM_ATENDIMENTO\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/solicitacoes/{id}/status", primeiro).header("Authorization", equipe)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EM_ATENDIMENTO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"));
        mvc.perform(delete("/api/solicitacoes/{id}", primeiro).header("Authorization", cidadao))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/solicitacoes/{id}/status", primeiro).header("Authorization", equipe)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ATENDIDA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATENDIDA"));
    }

    @Test
    void equipeMantemCatalogoECidadaoConsulta() throws Exception {
        String equipe = equipe();
        String cidadao = cadastrar("catalogo@example.com");
        long regiao = corpo(mvc.perform(post("/api/regioes").header("Authorization", equipe)
                        .contentType(MediaType.APPLICATION_JSON).content(REGIAO))
                .andExpect(status().isCreated()).andReturn()).path("id").asLong();
        mvc.perform(get("/api/regioes").header("Authorization", cidadao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].nivelRisco").value("CRITICO"));
        mvc.perform(post("/api/pontos-agua").header("Authorization", cidadao)
                        .contentType(MediaType.APPLICATION_JSON).content(PONTO))
                .andExpect(status().isForbidden());
        long ponto = corpo(mvc.perform(post("/api/pontos-agua").header("Authorization", equipe)
                        .contentType(MediaType.APPLICATION_JSON).content(PONTO))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.regiaoId").value(regiao))
                .andReturn()).path("id").asLong();
        mvc.perform(get("/api/pontos-agua").header("Authorization", cidadao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].cidade").value("Canoas"));
        mvc.perform(get("/api/pontos-agua/{id}", ponto).header("Authorization", cidadao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISPONIVEL"));
        mvc.perform(put("/api/pontos-agua/{id}", ponto).header("Authorization", equipe)
                        .contentType(MediaType.APPLICATION_JSON).content(PONTO.replace("1000", "4000")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/pontos-agua/{id}", ponto).header("Authorization", equipe)
                        .contentType(MediaType.APPLICATION_JSON).content(PONTO.replace("1000", "700")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estoqueLitros").value(700));
        mvc.perform(delete("/api/pontos-agua/{id}", ponto).header("Authorization", equipe))
                .andExpect(status().isNoContent());
    }

    private long criarPedido(String cidadao) throws Exception {
        return corpo(mvc.perform(post("/api/solicitacoes").header("Authorization", cidadao)
                        .contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.prioridade").value(70)).andReturn()).path("id").asLong();
    }

    private String cadastrar(String email) throws Exception {
        return token(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro(email)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usuario.papel").value("CIDADAO"))
                .andReturn());
    }

    private String equipe() throws Exception {
        return token(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"equipe-test@example.com","senha":"Equipe-Teste-2026"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.papel").value("EQUIPE")).andReturn());
    }

    private static String cadastro(String email) {
        return """
                {"nome":"Maria Souza","email":"%s","senha":"TesteSenha-2026","cidade":"Canoas","papel":"EQUIPE"}
                """.formatted(email);
    }

    private JsonNode corpo(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsByteArray());
    }

    private String token(MvcResult resultado) throws Exception {
        return "Bearer " + corpo(resultado).path("token").asText();
    }
}
