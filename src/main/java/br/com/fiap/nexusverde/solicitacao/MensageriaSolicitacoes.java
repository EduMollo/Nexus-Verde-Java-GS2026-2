package br.com.fiap.nexusverde.solicitacao;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "nexus.rabbit-enabled", havingValue = "true")
class MensageriaSolicitacoes {
    static final String EXCHANGE = "nexus-verde.solicitacoes";
    static final String ROTA = "solicitacao.criada.v1";
    static final String FILA = "nexus-verde.triagem.v1";
    static final String FALHAS = "nexus-verde.triagem.falhas.v1";
    private static final Logger log = LoggerFactory.getLogger(MensageriaSolicitacoes.class);

    private final EventoRepository eventos;
    private final SolicitacaoService solicitacoes;
    private final RabbitTemplate rabbit;
    private final ObjectMapper json;
    private final TransactionTemplate transacao;

    MensageriaSolicitacoes(EventoRepository eventos, SolicitacaoService solicitacoes, RabbitTemplate rabbit,
                           ObjectMapper json, TransactionTemplate transacao) {
        this.eventos = eventos;
        this.solicitacoes = solicitacoes;
        this.rabbit = rabbit;
        this.json = json;
        this.transacao = transacao;
    }

    @Bean
    Declarables topologiaSolicitacoes() {
        var exchange = new DirectExchange(EXCHANGE);
        var fila = QueueBuilder.durable(FILA).deadLetterExchange("").deadLetterRoutingKey(FALHAS).build();
        var falhas = QueueBuilder.durable(FALHAS).build();
        return new Declarables(exchange, fila, falhas, BindingBuilder.bind(fila).to(exchange).with(ROTA));
    }

    @Scheduled(fixedDelayString = "${nexus.eventos-intervalo-ms:3000}")
    void publicarPendentes() {
        for (EventoPendente evento : eventos.findTop20ByPublicadoEmIsNullOrderByCriadoEmAsc()) {
            try {
                publicar(evento);
                transacao.executeWithoutResult(status ->
                        eventos.findById(evento.id()).ifPresent(EventoPendente::publicado));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Evento {} continua pendente ({})", evento.id(), e.getClass().getSimpleName());
                return;
            }
        }
    }

    private void publicar(EventoPendente evento) throws Exception {
        var dados = new SolicitacaoCriada(evento.id(), evento.solicitacaoId(), evento.criadoEm());
        Message mensagem = MessageBuilder.withBody(json.writeValueAsBytes(dados))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(evento.id().toString())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        CorrelationData confirmacao = new CorrelationData(evento.id().toString());
        rabbit.send(EXCHANGE, ROTA, mensagem, confirmacao);
        if (!confirmacao.getFuture().get(5, TimeUnit.SECONDS).isAck() || confirmacao.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ não confirmou o roteamento.");
        }
        log.info("Evento {} publicado para solicitação {}", evento.id(), evento.solicitacaoId());
    }

    @RabbitListener(queues = FILA)
    void consumir(Message mensagem) throws java.io.IOException {
        SolicitacaoCriada evento = json.readValue(mensagem.getBody(), SolicitacaoCriada.class);
        solicitacoes.registrarTriagem(evento.solicitacaoId());
        log.info("Evento {} consumido para solicitação {}", evento.id(), evento.solicitacaoId());
    }

    record SolicitacaoCriada(UUID id, Long solicitacaoId, Instant criadoEm) {}
}
