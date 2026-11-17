package br.com.fiap.nexusverde.solicitacao;

import java.time.Instant;

import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.Movimentacao;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.Status;
import jakarta.persistence.*;

@Entity
@Table(name = "solicitacao_historico")
class HistoricoStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long solicitacaoId;
    private Long equipeId;
    @Enumerated(EnumType.STRING)
    private Status statusAnterior;
    @Enumerated(EnumType.STRING)
    private Status statusNovo;
    private String motivo;
    private Instant registradaEm;

    protected HistoricoStatus() {}

    HistoricoStatus(Long solicitacaoId, Long equipeId, Status statusAnterior, Status statusNovo, String motivo) {
        this.solicitacaoId = solicitacaoId;
        this.equipeId = equipeId;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.motivo = motivo;
        registradaEm = Instant.now();
    }

    Movimentacao saida() {
        return new Movimentacao(statusAnterior, statusNovo, motivo, registradaEm);
    }
}
