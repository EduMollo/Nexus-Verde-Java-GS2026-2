package br.com.fiap.nexusverde.solicitacao;

import br.com.fiap.nexusverde.auth.Ator;
import br.com.fiap.nexusverde.auth.Usuario;
import br.com.fiap.nexusverde.auth.UsuarioRepository;
import br.com.fiap.nexusverde.catalogo.CatalogoService;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.*;
import br.com.fiap.nexusverde.web.ApiException;
import br.com.fiap.nexusverde.web.Pagina;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SolicitacaoService {
    private final SolicitacaoRepository pedidos;
    private final EventoRepository eventos;
    private final HistoricoStatusRepository historico;
    private final UsuarioRepository usuarios;
    private final CatalogoService catalogo;

    SolicitacaoService(SolicitacaoRepository pedidos, EventoRepository eventos,
                       UsuarioRepository usuarios, CatalogoService catalogo, HistoricoStatusRepository historico) {
        this.pedidos = pedidos;
        this.eventos = eventos;
        this.usuarios = usuarios;
        this.catalogo = catalogo;
        this.historico = historico;
    }

    public Pagina<Saida> listar(Ator ator, Pageable pagina) {
        var resultados = ator.equipe() ? pedidos.findByExcluidaEmIsNull(pagina)
                : pedidos.findBySolicitanteIdAndExcluidaEmIsNull(ator.id(), pagina);
        return Pagina.de(resultados.map(Solicitacao::saida));
    }

    public Pagina<Saida> pesquisar(Ator ator, String busca, Status status, Urgencia urgencia, Pageable pagina) {
        ator.exigirEquipe();
        String termo = busca.strip().toLowerCase(Locale.ROOT);
        Specification<Solicitacao> filtros = (pedido, consulta, cb) -> {
            var condicoes = new ArrayList<Predicate>();
            condicoes.add(cb.isNull(pedido.get("excluidaEm")));
            if (status != null) {
                condicoes.add(cb.equal(pedido.get("status"), status));
            }
            if (urgencia != null) {
                condicoes.add(cb.equal(pedido.get("urgencia"), urgencia));
            }
            if (!termo.isEmpty()) {
                condicoes.add(cb.or(cb.gt(cb.locate(cb.lower(pedido.get("cidade")), termo), 0),
                        cb.gt(cb.locate(cb.lower(pedido.get("descricao")), termo), 0)));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        return Pagina.de(pedidos.findAll(filtros, pagina).map(Solicitacao::saida));
    }

    public long contar(Ator ator, Status status) {
        ator.exigirEquipe();
        return pedidos.countByStatusAndExcluidaEmIsNull(status);
    }

    public List<Movimentacao> historico(Ator ator, Long id) {
        ator.exigirEquipe();
        return historico.findBySolicitacaoIdOrderByIdDesc(id).stream().map(HistoricoStatus::saida).toList();
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
        return registrarStatus(ator, id, status, null);
    }

    @Transactional
    public Saida cancelarNoPainel(Ator ator, Long id, String justificativa) {
        String motivo = justificativa == null ? "" : justificativa.strip();
        if (motivo.isEmpty() || motivo.length() > 400) {
            throw ApiException.invalido("Informe um motivo com até 400 caracteres.");
        }
        return registrarStatus(ator, id, Status.CANCELADA, motivo);
    }

    private Saida registrarStatus(Ator ator, Long id, Status status, String motivo) {
        ator.exigirEquipe();
        Solicitacao pedido = encontrar(id);
        Status anterior = pedido.status();
        pedido.alterarStatus(status);
        if (anterior != status) {
            historico.save(new HistoricoStatus(id, ator.id(), anterior, status, motivo));
        }
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
