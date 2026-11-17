package br.com.fiap.nexusverde.solicitacao;

import br.com.fiap.nexusverde.auth.Ator;
import br.com.fiap.nexusverde.auth.Papel;
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
import java.util.Optional;
import java.util.UUID;
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
        Long protocolo = protocoloBuscado(termo);
        Specification<Solicitacao> filtros = (pedido, consulta, cb) -> {
            var condicoes = new ArrayList<Predicate>();
            condicoes.add(cb.isNull(pedido.get("excluidaEm")));
            if (status != null) {
                condicoes.add(cb.equal(pedido.get("status"), status));
            }
            if (urgencia != null) {
                condicoes.add(cb.equal(pedido.get("urgencia"), urgencia));
            }
            if (protocolo != null) {
                condicoes.add(cb.equal(pedido.get("id"), protocolo));
            } else if (!termo.isEmpty()) {
                condicoes.add(cb.or(cb.gt(cb.locate(cb.lower(pedido.get("cidade")), termo), 0),
                        cb.gt(cb.locate(cb.lower(pedido.get("descricao")), termo), 0),
                        cb.gt(cb.locate(cb.lower(pedido.get("solicitante").get("nome")), termo), 0)));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        return Pagina.de(pedidos.findAll(filtros, pagina).map(Solicitacao::saida));
    }

    private static Long protocoloBuscado(String termo) {
        try {
            return Long.valueOf(termo.startsWith("#") ? termo.substring(1) : termo);
        } catch (NumberFormatException e) {
            return null;
        }
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

    public DetalhePainel detalhePainel(Ator ator, Long id) {
        ator.exigirEquipe();
        return encontrar(id).detalhePainel();
    }

    @Transactional
    public Saida criar(Ator ator, Entrada dados) {
        if (ator.equipe()) {
            throw ApiException.proibido();
        }
        Usuario usuario = usuarios.findById(ator.id()).orElseThrow(ApiException::proibido);
        return salvarNova(nova(usuario, dados)).saida();
    }

    @Transactional
    public long criarNoPainel(Ator ator, long cidadaoId, Entrada dados, UUID registro) {
        ator.exigirEquipe();
        var existente = localizarRegistroPainel(ator, cidadaoId, registro);
        if (existente.isPresent()) {
            return existente.get();
        }
        Usuario equipe = usuarios.findByIdAndPapelAndAtivoTrue(ator.id(), Papel.EQUIPE)
                .orElseThrow(ApiException::proibido);
        Usuario cidadao = usuarios.findByIdAndPapelAndAtivoTrue(cidadaoId, Papel.CIDADAO)
                .orElseThrow(() -> ApiException.naoEncontrado("Cidadão ativo"));
        Solicitacao pedido = nova(cidadao, dados);
        pedido.atribuirCadastro(equipe, registro);
        return salvarNova(pedido).id();
    }

    public Optional<Long> localizarRegistroPainel(Ator ator, long cidadaoId, UUID registro) {
        ator.exigirEquipe();
        return pedidos.findByRegistroPainelAndCadastradaPorIdAndSolicitanteId(registro, ator.id(), cidadaoId)
                .map(Solicitacao::id);
    }

    private Solicitacao nova(Usuario solicitante, Entrada dados) {
        return new Solicitacao(solicitante, catalogo.resolverRegiao(dados.regiaoId(), dados.cidade()), dados);
    }

    private Solicitacao salvarNova(Solicitacao pedido) {
        Solicitacao salvo = pedidos.save(pedido);
        eventos.save(new EventoPendente(salvo.id()));
        return salvo;
    }

    @Transactional
    public void editarNoPainel(Ator ator, long id, long versao, Entrada dados) {
        DetalhePainel atual = detalheParaEdicao(ator, id);
        if (atual.versao() != versao) {
            throw ApiException.conflito("O pedido mudou. Abra a edição novamente para conferir os dados.");
        }
        Long regiao = atual.pedido().cidade().equalsIgnoreCase(dados.cidade().strip())
                ? atual.pedido().regiaoId() : null;
        encontrar(id).editar(dados, catalogo.resolverRegiao(regiao, dados.cidade()));
    }

    public DetalhePainel detalheParaEdicao(Ator ator, long id) {
        var detalhe = detalhePainel(ator, id);
        if (detalhe.pedido().status() != Status.ABERTA) {
            throw ApiException.conflito("A edição está disponível apenas para solicitações abertas.");
        }
        return detalhe;
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
