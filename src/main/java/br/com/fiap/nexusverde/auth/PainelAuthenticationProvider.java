package br.com.fiap.nexusverde.auth;

import br.com.fiap.nexusverde.web.ApiException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

@Component
class PainelAuthenticationProvider implements AuthenticationProvider {
    private final AuthService auth;

    PainelAuthenticationProvider(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        try {
            Usuario usuario = auth.autenticar(authentication.getName(), authentication.getCredentials().toString());
            if (usuario.getPapel() != Papel.EQUIPE) {
                throw new BadCredentialsException("Acesso reservado à equipe.");
            }
            return UsernamePasswordAuthenticationToken.authenticated(usuario.getId().toString(), null,
                    AuthorityUtils.createAuthorityList("ROLE_EQUIPE"));
        } catch (ApiException e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> tipo) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
    }
}
