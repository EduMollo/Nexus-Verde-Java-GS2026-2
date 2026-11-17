package br.com.fiap.nexusverde.auth;

import java.util.Locale;

import br.com.fiap.nexusverde.web.ApiException;
import br.com.fiap.nexusverde.web.Pagina;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CidadaosPainel {
    private final UsuarioRepository usuarios;

    CidadaosPainel(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Pagina<Resumo> pesquisar(Ator ator, String busca, Pageable pagina) {
        ator.exigirEquipe();
        return Pagina.de(usuarios.pesquisarAtivos(Papel.CIDADAO, busca.strip().toLowerCase(Locale.ROOT), pagina)
                .map(CidadaosPainel::resumo));
    }

    public Resumo consultar(Ator ator, long id) {
        ator.exigirEquipe();
        var usuario = usuarios.findByIdAndPapelAndAtivoTrue(id, Papel.CIDADAO)
                .orElseThrow(() -> ApiException.naoEncontrado("Cidadão ativo"));
        return resumo(usuario);
    }

    private static Resumo resumo(Usuario usuario) {
        return new Resumo(usuario.getId(), usuario.getNome(), usuario.cidade());
    }

    public record Resumo(Long id, String nome, String cidade) {}
}
