package br.com.fiap.nexusverde.auth;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

final class PainelAcessoNegadoHandler implements AccessDeniedHandler {
    private final AccessDeniedHandler padrao = new AccessDeniedHandlerImpl();
    private final DefaultRedirectStrategy redirecionamento = new DefaultRedirectStrategy();
    private final AuthenticationTrustResolverImpl confianca = new AuthenticationTrustResolverImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException erro)
            throws IOException, ServletException {
        if (erro instanceof CsrfException) {
            var autenticacao = SecurityContextHolder.getContext().getAuthentication();
            String destino = autenticacao != null && autenticacao.isAuthenticated()
                    && !confianca.isAnonymous(autenticacao)
                    ? "/painel?formularioExpirado" : "/painel/entrar?expirada";
            redirecionamento.sendRedirect(request, response, destino);
            return;
        }
        padrao.handle(request, response, erro);
    }
}
