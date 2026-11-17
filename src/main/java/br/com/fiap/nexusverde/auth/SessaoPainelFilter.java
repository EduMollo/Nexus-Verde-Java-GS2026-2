package br.com.fiap.nexusverde.auth;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

final class SessaoPainelFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;

    SessaoPainelFilter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/painel");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof IdentidadePainel identidade
                && !usuarios.existsByIdAndAtivoTrueAndVersaoAcesso(identidade.id(), identidade.versao())) {
            new SecurityContextLogoutHandler().logout(request, response, autenticacao);
            new DefaultRedirectStrategy().sendRedirect(request, response, "/painel/entrar?expirada");
            return;
        }
        chain.doFilter(request, response);
    }
}
