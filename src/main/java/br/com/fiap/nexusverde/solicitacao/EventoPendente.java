package br.com.fiap.nexusverde.solicitacao;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "eventos_pendentes")
class EventoPendente {
    @Id
    private UUID id;
    private Long solicitacaoId;
    private Instant criadoEm;
    private Instant publicadoEm;

    protected EventoPendente() {}

    EventoPendente(Long solicitacaoId) {
        this.id = UUID.randomUUID();
        this.solicitacaoId = solicitacaoId;
        this.criadoEm = Instant.now();
    }

    UUID id() { return id; }
    Long solicitacaoId() { return solicitacaoId; }
    Instant criadoEm() { return criadoEm; }
    void publicado() { publicadoEm = Instant.now(); }
}
