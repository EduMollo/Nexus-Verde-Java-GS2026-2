package br.com.fiap.nexusverde.solicitacao;

import br.com.fiap.nexusverde.auth.Ator;
import br.com.fiap.nexusverde.auth.Usuario;
import br.com.fiap.nexusverde.auth.UsuarioRepository;
import br.com.fiap.nexusverde.catalogo.CatalogoService;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.*;
import br.com.fiap.nexusverde.web.ApiException;
import br.com.fiap.nexusverde.web.Pagina;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SolicitacaoService {
    private final SolicitacaoRepository pedidos;
    private final EventoRepository eventos;
    private final UsuarioRepository usuarios;
    private final CatalogoService catalogo;

    SolicitacaoService(SolicitacaoRepository pedidos, EventoRepository eventos,
                       UsuarioRepository usuarios, CatalogoService catalogo) {
        this.pedidos = pedidos;
        this.eventos = eventos;
        this.usuarios = usuarios;
        this.catalogo = catalogo;
    }

    public Pagina<Saida> listar(Ator ator, Pageable pagina) {
        var resultados = ator.equipe() ? pedidos.findByExcluidaEmIsNull(pagina)
                : pedidos.findBySolicitanteIdAndExcluidaEmIsNull(ator.id(), pagina);
        return Pagina.de(resultados.map(Solicitacao::saida));
    }

    public Pagina<Saida> triagem(Ator ator, Pageable pagina) {
        ator.exigirEquipe();
        return Pagina.de(pedidos.findByStatusAndExcluidaEmIsNullAndTriadaEmIsNotNull(Status.ABERTA, pagina)
                .map(Solicitacao::saida));
    }

    public Saida detalhe(Ator ator, Long id) {
        Solicitacao pedido = encontrar(id);
        if (!ator.equipe()) {
            exigirDono(ator, pedido);
        }
        return pedido.saida();
    }

    @Transactional
    public Saida criar(Ator ator, Entrada dados) {
        if (ator.equipe()) {
            throw ApiException.proibido();
        }
        Usuario usuario = usuarios.findById(ator.id()).orElseThrow(ApiException::proibido);
        var regiao = catalogo.resolverRegiao(dados.regiaoId(), dados.cidade());
        Solicitacao pedido = pedidos.save(new Solicitacao(usuario, regiao, dados));
        eventos.save(new EventoPendente(pedido.id()));
        return pedido.saida();
    }

    @Transactional
    public Saida editar(Ator ator, Long id, Entrada dados) {
        Solicitacao pedido = encontrar(id);
        exigirDono(ator, pedido);
        pedido.editar(dados, catalogo.resolverRegiao(dados.regiaoId(), dados.cidade()));
        return pedido.saida();
    }

    @Transactional
    public void excluir(Ator ator, Long id) {
        Solicitacao pedido = encontrar(id);
        exigirDono(ator, pedido);
        pedido.excluir();
    }

    @Transactional
    public Saida status(Ator ator, Long id, Status status) {
        ator.exigirEquipe();
        Solicitacao pedido = encontrar(id);
        pedido.alterarStatus(status);
        return pedido.saida();
    }

    public ContextoTriagem contexto(Ator ator, Long id) {
        ator.exigirEquipe();
        return encontrar(id).contexto();
    }

    @Transactional
    public void salvarAnalise(Ator ator, Long id, long versao, String texto) {
        ator.exigirEquipe();
        encontrar(id).salvarAnalise(versao, texto);
    }

    @Transactional
    public void registrarTriagem(Long id) {
        pedidos.findByIdAndExcluidaEmIsNull(id).ifPresent(Solicitacao::registrarTriagem);
    }

    private Solicitacao encontrar(Long id) {
        return pedidos.findByIdAndExcluidaEmIsNull(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Solicitação"));
    }

    private void exigirDono(Ator ator, Solicitacao pedido) {
        if (ator.equipe() || ator.id() != pedido.dono()) {
            throw ApiException.proibido();
        }
    }
}
