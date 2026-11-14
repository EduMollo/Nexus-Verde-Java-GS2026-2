package br.com.fiap.nexusverde.catalogo;

import java.time.Instant;
import br.com.fiap.nexusverde.catalogo.CatalogoDtos.*;
import jakarta.persistence.*;

@Entity
@Table(name = "pontos_agua")
class PontoAgua {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Regiao regiao;
    private String nome;
    @Enumerated(EnumType.STRING)
    private TipoPonto tipo;
    private String endereco;
    private String cidade;
    private Double latitude;
    private Double longitude;
    @Enumerated(EnumType.STRING)
    private StatusPonto status;
    private int capacidadeLitros;
    private int estoqueLitros;
    private String horarioFuncionamento;
    private String telefone;
    private Instant atualizadoEm;
    @Version
    private long versao;

    protected PontoAgua() {}

    void atualizar(PontoEntrada dados, Regiao regiao) {
        this.regiao = regiao;
        nome = dados.nome().strip();
        tipo = dados.tipo();
        endereco = dados.endereco().strip();
        cidade = dados.cidade().strip();
        latitude = dados.latitude();
        longitude = dados.longitude();
        capacidadeLitros = dados.capacidadeLitros();
        horarioFuncionamento = dados.horarioFuncionamento();
        telefone = dados.telefone();
        disponibilidade(dados.status(), dados.estoqueLitros());
    }

    void disponibilidade(StatusPonto status, int estoque) {
        if (estoque < 0 || estoque > capacidadeLitros) {
            throw br.com.fiap.nexusverde.web.ApiException.invalido("O estoque deve estar entre zero e a capacidade.");
        }
        this.status = status;
        this.estoqueLitros = estoque;
        atualizadoEm = Instant.now();
    }

    PontoSaida saida() {
        return new PontoSaida(id, nome, tipo, endereco, cidade, latitude, longitude, status,
                capacidadeLitros, estoqueLitros, horarioFuncionamento, telefone, atualizadoEm,
                regiao == null ? null : regiao.getId());
    }
}
