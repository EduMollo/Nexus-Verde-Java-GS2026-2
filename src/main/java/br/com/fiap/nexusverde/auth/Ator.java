package br.com.fiap.nexusverde.auth;

import br.com.fiap.nexusverde.web.ApiException;
import org.springframework.security.oauth2.jwt.Jwt;

public record Ator(long id, Papel papel) {
    public static Ator de(Jwt jwt) {
        return new Ator(Long.parseLong(jwt.getSubject()), Papel.valueOf(jwt.getClaimAsString("papel")));
    }

    public boolean equipe() {
        return papel == Papel.EQUIPE;
    }

    public void exigirEquipe() {
        if (!equipe()) {
            throw ApiException.proibido();
        }
    }
}
