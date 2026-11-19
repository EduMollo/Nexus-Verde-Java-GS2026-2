package br.com.fiap.nexusverde.iot;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class Copiloto {
    record Parecer(String texto, Procedimentos.Fonte fonte, String motor) {}
    record Resposta(String texto, String fonte) {}
    private final Procedimentos base;
    private final ObjectProvider<ChatModel> modelos;
    private final ObjectMapper json;
    private final String webhook;
    private final String token;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

    Copiloto(Procedimentos base, ObjectProvider<ChatModel> modelos, ObjectMapper json,
             @Value("${IOT_N8N_URL:}") String webhook, @Value("${IOT_N8N_TOKEN:}") String token) {
        this.base=base; this.modelos=modelos; this.json=json; this.webhook=webhook; this.token=token;
    }

    Parecer analisar(Situacao situacao) throws Exception {
        List<Procedimentos.Fonte> fontes = base.buscar(situacao.consulta());
        if (fontes.isEmpty()) throw new IllegalStateException("Nenhuma fonte recuperada");
        String contexto = json.writeValueAsString(Map.of("situacao",situacao,"fontes",fontes));
        if (!webhook.isBlank()) {
            try {
                if (token.isBlank()) throw new IllegalStateException("Webhook sem credencial");
                var req = HttpRequest.newBuilder(URI.create(webhook)).timeout(Duration.ofSeconds(50))
                        .header("Content-Type","application/json").header("X-Copiloto-Key",token)
                        .POST(HttpRequest.BodyPublishers.ofString(contexto)).build();
                var r = http.send(req,HttpResponse.BodyHandlers.ofString());
                if (r.statusCode()!=200) {
                    throw new java.io.IOException("HTTP " + r.statusCode());
                }
                return validar(json.readValue(r.body(),Resposta.class), fontes, "Agente n8n");
            } catch (java.io.IOException | RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger(Copiloto.class).warn("Agente n8n indisponível ({}). Usando o modelo do painel.",e.getMessage() != null && e.getMessage().matches("HTTP [0-9]{3}")
                                ? e.getMessage() : e.getClass().getSimpleName());
            }
        }
        var modelo = modelos.getIfAvailable();
        if (modelo==null) throw new IllegalStateException("Modelo indisponível");
        var procedimento = fontes.get(0);
        String contextoOperacional = json.writeValueAsString(Map.of(
                "situacao", situacao.consulta(), "procedimento", procedimento));
        String saida = ChatClient.create(modelo).prompt().system("""
                Redija uma orientação curta em português para a equipe de abastecimento.
                Use a situação já avaliada e a ação do procedimento fornecido. O cenário é fictício. Não afirme potabilidade,
                entrega efetuada ou ajuste de estoque. Escreva apenas a ação a tomar, sem números, percentuais ou volumes.
                Retorne somente JSON com texto e fonte (código de um procedimento recuperado).
                """).user(contextoOperacional).call().content();
        if (saida==null) throw new IllegalStateException("Resposta vazia");
        var resposta=json.readValue(saida.strip().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", ""),Resposta.class);
        return validar(resposta, fontes, webhook.isBlank() ? "Spring AI" : "Spring AI · contingência do agente");
    }

    private Parecer validar(Resposta resposta, List<Procedimentos.Fonte> fontes, String motor) {
        if (resposta.texto()==null || resposta.texto().isBlank() || resposta.texto().length()>900)
            throw new IllegalStateException("Orientação inválida");
        var fonte = fontes.get(0);
        if (!fonte.codigo().equals(resposta.fonte()) || resposta.texto().matches("(?s).*\\d.*"))
            throw new IllegalStateException("Orientação incompatível com o procedimento prioritário");
        return new Parecer(resposta.texto().strip(), fonte, motor);
    }
}
