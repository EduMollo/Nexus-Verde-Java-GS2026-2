package br.com.fiap.nexusverde.solicitacao;

import java.time.Instant;

import br.com.fiap.nexusverde.auth.Usuario;
import br.com.fiap.nexusverde.catalogo.Regiao;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.*;
import br.com.fiap.nexusverde.web.ApiException;
import jakarta.persistence.*;

@Entity
@Table(name = "solicitacoes")
class Solicitacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario solicitante;
    @ManyToOne(fetch = FetchType.LAZY)
    private Regiao regiao;
    @Enumerated(EnumType.STRING)
    private Tipo tipo;
    private String descricao;
    private int quantidadePessoas;
    @Enumerated(EnumType.STRING)
    private Urgencia urgencia;
    private boolean grupoVulneravel;
    private String endereco;
    private String cidade;
    private Double latitude;
    private Double longitude;
    @Enumerated(EnumType.STRING)
    private Status status;
    private Integer prioridade;
    private String analiseIa;
    private Instant criadaEm;
    private Instant atualizadaEm;
    private Instant excluidaEm;
    private Instant triadaEm;
    @Version
    private long versao;

    protected Solicitacao() {}

    Solicitacao(Usuario solicitante, Regiao regiao, Entrada dados) {
        this.solicitante = solicitante;
        this.status = Status.ABERTA;
        this.criadaEm = Instant.now();
        editar(dados, regiao);
    }

    void editar(Entrada dados, Regiao regiao) {
        exigirAberta();
        this.regiao = regiao;
        tipo = dados.tipo();
        descricao = dados.descricao().strip();
        quantidadePessoas = dados.quantidadePessoas();
        urgencia = dados.urgencia();
        grupoVulneravel = dados.grupoVulneravel();
        endereco = dados.endereco().strip();
        cidade = dados.cidade().strip();
        latitude = dados.latitude();
        longitude = dados.longitude();
        prioridade = pontuacao();
        analiseIa = null;
        atualizadaEm = Instant.now();
    }

    private int pontuacao() {
        int base = switch (urgencia) {
            case BAIXA -> 10;
            case MEDIA -> 30;
            case ALTA -> 50;
            case CRITICA -> 70;
        };
        int adicional = Math.min(quantidadePessoas, 10)
                + (grupoVulneravel ? 15 : 0) + (tipo == Tipo.AGUA_CONTAMINADA ? 5 : 0);
        return Math.min(base + adicional, 100);
    }

    void excluir() {
        exigirAberta();
        status = Status.CANCELADA;
        excluidaEm = Instant.now();
        atualizadaEm = excluidaEm;
    }

    void alterarStatus(Status novo) {
        if (novo == status) {
            return;
        }
        boolean permitido = switch (status) {
            case ABERTA -> novo == Status.EM_ATENDIMENTO || novo == Status.CANCELADA;
            case EM_ATENDIMENTO -> novo == Status.ATENDIDA || novo == Status.CANCELADA;
            case ATENDIDA, CANCELADA -> false;
        };
        if (!permitido) {
            throw ApiException.conflito("Transição de status indisponível para esta solicitação.");
        }
        status = novo;
        atualizadaEm = Instant.now();
    }

    void registrarTriagem() {
        if (triadaEm == null && status == Status.ABERTA) {
            triadaEm = Instant.now();
        }
    }

    ContextoTriagem contexto() {
        if (status != Status.ABERTA && status != Status.EM_ATENDIMENTO) {
            throw ApiException.conflito("A análise é disponível para pedidos que ainda estão em atendimento ou triagem.");
        }
        return new ContextoTriagem(versao, tipo, urgencia, quantidadePessoas, grupoVulneravel, pontuacao());
    }

    void salvarAnalise(long versaoAnalisada, String texto) {
        if (versao != versaoAnalisada) {
            throw ApiException.conflito("O pedido foi atualizado durante a análise. Solicite uma nova análise.");
        }
        analiseIa = texto;
        atualizadaEm = Instant.now();
    }

    private void exigirAberta() {
        if (status != Status.ABERTA) {
            throw ApiException.conflito("A operação está disponível apenas para solicitações abertas.");
        }
    }

    Status status() { return status; }
    Long id() { return id; }
    Long dono() { return solicitante.getId(); }

    Saida saida() {
        return new Saida(id, tipo, descricao, quantidadePessoas, urgencia, grupoVulneravel, endereco, cidade,
                latitude, longitude, status, prioridade, analiseIa,
                new Solicitante(solicitante.getId(), solicitante.getNome()), criadaEm, atualizadaEm,
                regiao == null ? null : regiao.getId());
    }
}
