package br.com.fiap.nexusverde.catalogo;

import java.time.Instant;
import br.com.fiap.nexusverde.catalogo.CatalogoDtos.*;
import jakarta.persistence.*;

@Entity
@Table(name = "regioes")
public class Regiao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String cidade;
    @Enumerated(EnumType.STRING)
    private NivelRisco nivelRisco;
    private String situacao;
    private String descricao;
    private int populacaoAfetada;
    private Instant atualizadoEm;
    @Version
    private long versao;

    protected Regiao() {}

    void atualizar(RegiaoEntrada dados) {
        nome = dados.nome().strip();
        cidade = dados.cidade().strip();
        nivelRisco = dados.nivelRisco();
        situacao = dados.situacao().strip();
        descricao = dados.descricao();
        populacaoAfetada = dados.populacaoAfetada();
        atualizadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public String getCidade() { return cidade; }

    RegiaoSaida saida() {
        return new RegiaoSaida(id, nome, cidade, nivelRisco, situacao, descricao, populacaoAfetada, atualizadoEm);
    }
}
