package br.com.fiap.nexusverde.auth;

import java.util.ArrayList;
import java.util.Locale;

import br.com.fiap.nexusverde.web.ApiException;
import br.com.fiap.nexusverde.web.Pagina;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GestaoUsuarios {
    private final UsuarioRepository usuarios;
    private final AuthService auth;
    private final AcessoUsuarios acesso;

    GestaoUsuarios(UsuarioRepository usuarios, AuthService auth, AcessoUsuarios acesso) {
        this.usuarios = usuarios;
        this.auth = auth;
        this.acesso = acesso;
    }

    Pagina<Resumo> pesquisar(String busca, Papel papel, Boolean ativo, Pageable pagina) {
        exigirGestor();
        String termo = busca.strip().toLowerCase(Locale.ROOT);
        Specification<Usuario> filtros = (usuario, consulta, cb) -> {
            var condicoes = new ArrayList<Predicate>();
            if (papel != null) {
                condicoes.add(cb.equal(usuario.get("papel"), papel));
            }
            if (ativo != null) {
                condicoes.add(cb.equal(usuario.get("ativo"), ativo));
            }
            if (!termo.isEmpty()) {
                condicoes.add(cb.or(cb.gt(cb.locate(cb.lower(usuario.get("nome")), termo), 0),
                        cb.gt(cb.locate(cb.lower(usuario.get("email")), termo), 0)));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        return Pagina.de(usuarios.findAll(filtros, pagina).map(Resumo::de));
    }

    long cadastrar(CadastroUsuario dados) {
        exigirGestor();
        if (!dados.senha().equals(dados.confirmacao())) {
            throw ApiException.invalido("As senhas não conferem.");
        }
        if (dados.papel() == Papel.EQUIPE && dados.senha().length() < 12) {
            throw ApiException.invalido("A senha para a equipe precisa ter pelo menos 12 caracteres.");
        }
        return auth.criar(dados.dados(), dados.papel()).getId();
    }

    @Transactional
    void definirAtivo(Long id, boolean ativo) {
        exigirGestor();
        Usuario usuario = usuarios.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Usuário"));
        if (acesso.contaPrincipal(usuario)) {
            throw ApiException.invalido("A conta principal precisa continuar ativa.");
        }
        usuario.definirAtivo(ativo);
    }

    private void exigirGestor() {
        if (!acesso.podeGerenciar()) {
            throw ApiException.proibido();
        }
    }

    record Resumo(Long id, String nome, String email, Papel papel, String cidade, boolean ativo) {
        static Resumo de(Usuario usuario) {
            return new Resumo(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPapel(),
                    usuario.cidade(), usuario.ativo());
        }
    }
}
