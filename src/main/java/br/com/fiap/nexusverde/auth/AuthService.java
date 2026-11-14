package br.com.fiap.nexusverde.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import br.com.fiap.nexusverde.auth.AuthDtos.*;
import br.com.fiap.nexusverde.web.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    static final String EMISSOR = "nexus-verde";
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final JwtEncoder jwt;
    private final String hashDeComparacao;

    AuthService(UsuarioRepository usuarios, PasswordEncoder senhas, JwtEncoder jwt) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.jwt = jwt;
        this.hashDeComparacao = senhas.encode(java.util.UUID.randomUUID().toString());
    }

    Sessao cadastrar(Cadastro dados) {
        validarTamanhoSenha(dados.senha());
        String email = normalizarEmail(dados.email());
        if (usuarios.existsByEmail(email)) {
            throw emailEmUso();
        }

        try {
            Usuario usuario = usuarios.saveAndFlush(new Usuario(dados.nome().strip(), email,
                    senhas.encode(dados.senha()), Papel.CIDADAO, dados.telefone(), dados.cidade()));
            return sessao(usuario);
        } catch (DataIntegrityViolationException e) {
            if (usuarios.existsByEmail(email)) {
                throw emailEmUso();
            }
            throw e;
        }
    }

    Sessao entrar(Login dados) {
        return sessao(autenticar(dados.email(), dados.senha()));
    }

    public Usuario autenticar(String email, String senha) {
        Usuario usuario = usuarios.findByEmail(normalizarEmail(email)).orElse(null);
        String hash = usuario == null ? hashDeComparacao : usuario.senhaHash();
        if (senha.getBytes(UTF_8).length > 72 || !senhas.matches(senha, hash) || usuario == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }
        return usuario;
    }

    private Sessao sessao(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(2, ChronoUnit.HOURS))
                .claim("papel", usuario.getPapel().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new Sessao(jwt.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(),
                Perfil.de(usuario));
    }

    static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    static void validarTamanhoSenha(String senha) {
        if (senha.getBytes(UTF_8).length > 72) {
            throw ApiException.invalido("A senha deve ter no máximo 72 bytes.");
        }
    }

    private static ApiException emailEmUso() {
        return ApiException.conflito("Este e-mail já está cadastrado.");
    }
}
