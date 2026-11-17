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
    private final AcessoUsuarios acesso;

    PainelAuthenticationProvider(AuthService auth, AcessoUsuarios acesso) {
        this.auth = auth;
        this.acesso = acesso;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        try {
            Usuario usuario = auth.autenticar(authentication.getName(), authentication.getCredentials().toString());
            if (usuario.getPapel() != Papel.EQUIPE) {
                throw new BadCredentialsException("Acesso reservado à equipe.");
            }
            var autoridades = acesso.contaPrincipal(usuario)
                    ? AuthorityUtils.createAuthorityList("ROLE_EQUIPE", AcessoUsuarios.AUTORIDADE)
                    : AuthorityUtils.createAuthorityList("ROLE_EQUIPE");
            return UsernamePasswordAuthenticationToken.authenticated(
                    new IdentidadePainel(usuario.getId(), usuario.versaoAcesso()), null, autoridades);
        } catch (ApiException e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> tipo) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
    }
}
