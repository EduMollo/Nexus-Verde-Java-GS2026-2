package br.com.fiap.nexusverde.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AcessoUsuarios {
    static final String AUTORIDADE = "GERENCIAR_USUARIOS";
    private final String emailPrincipal;

    AcessoUsuarios(@Value("${nexus.equipe-email}") String emailPrincipal) {
        this.emailPrincipal = AuthService.normalizarEmail(emailPrincipal);
    }

    boolean contaPrincipal(Usuario usuario) {
        return usuario.getPapel() == Papel.EQUIPE && emailPrincipal.equals(usuario.getEmail());
    }

    public boolean podeGerenciar() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.getAuthorities().contains(new SimpleGrantedAuthority(AUTORIDADE));
    }
}
