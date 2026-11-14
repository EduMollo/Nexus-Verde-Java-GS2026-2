package br.com.fiap.nexusverde.painel;

import br.com.fiap.nexusverde.auth.Ator;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoService;
import br.com.fiap.nexusverde.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
class TriagemIa {
    private static final Logger log = LoggerFactory.getLogger(TriagemIa.class);
    private final ObjectProvider<ChatModel> modelos;
    private final SolicitacaoService pedidos;

    TriagemIa(ObjectProvider<ChatModel> modelos, SolicitacaoService pedidos) {
        this.modelos = modelos;
        this.pedidos = pedidos;
    }

    boolean disponivel() {
        return modelos.getIfAvailable() != null;
    }

    void analisar(Ator ator, Long id) {
        var dados = pedidos.contexto(ator, id);
        var modelo = modelos.getIfAvailable();
        if (modelo == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "A análise por IA não está configurada.");
        }
        String texto;
        try {
            texto = ChatClient.create(modelo).prompt()
                    .system("""
                            Auxilie uma equipe humana a triar pedidos de água em um protótipo acadêmico.
                            Em até duas frases em português, justifique a prioridade informada e sugira
                            uma verificação operacional para a equipe. Use somente os dados recebidos.
                            Não invente estoque, potabilidade, prazos, pessoas ou ações já executadas.
                            Não altere o status nem a pontuação. Escreva texto simples.
                            """)
                    .user("""
                            Tipo: %s. Urgência informada: %s. Pessoas: %d.
                            Grupo vulnerável: %s. Prioridade pela regra de triagem: %d/100.
                            """.formatted(dados.tipo(), dados.urgencia(), dados.quantidadePessoas(),
                            dados.grupoVulneravel() ? "sim" : "não", dados.prioridade()))
                    .call().content();
        } catch (RuntimeException e) {
            log.warn("Não foi possível consultar o modelo ({})", e.getClass().getSimpleName());
            throw indisponivel();
        }
        if (texto == null || texto.isBlank() || texto.length() > 1000) {
            throw indisponivel();
        }
        pedidos.salvarAnalise(ator, id, dados.versao(), texto.strip());
    }

    private static ApiException indisponivel() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "A análise por IA está indisponível. A equipe pode continuar a triagem manual.");
    }
}
