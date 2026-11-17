package br.com.fiap.nexusverde.auth;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String email;
    private String senhaHash;
    @Enumerated(EnumType.STRING)
    private Papel papel;
    private String telefone;
    private String cidade;
    private boolean ativo = true;
    @Version
    private long versaoAcesso;

    protected Usuario() {}

    Usuario(String nome, String email, String senhaHash, Papel papel, String telefone, String cidade) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.telefone = telefone;
        this.cidade = cidade;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public Papel getPapel() { return papel; }
    boolean ativo() { return ativo; }
    void definirAtivo(boolean ativo) { this.ativo = ativo; }
    long versaoAcesso() { return versaoAcesso; }
    String senhaHash() { return senhaHash; }
    void atualizarSenha(String hash) { senhaHash = hash; }
    String telefone() { return telefone; }
    String cidade() { return cidade; }
}
