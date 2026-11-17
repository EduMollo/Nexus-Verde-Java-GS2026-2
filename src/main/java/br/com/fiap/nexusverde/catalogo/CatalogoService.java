package br.com.fiap.nexusverde.catalogo;

import br.com.fiap.nexusverde.auth.Ator;
import br.com.fiap.nexusverde.catalogo.CatalogoDtos.*;
import br.com.fiap.nexusverde.web.ApiException;
import br.com.fiap.nexusverde.web.Pagina;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogoService {
    private final RegiaoRepository regioes;
    private final PontoRepository pontos;

    CatalogoService(RegiaoRepository regioes, PontoRepository pontos) {
        this.regioes = regioes;
        this.pontos = pontos;
    }

    public Pagina<RegiaoSaida> regioes(Pageable pagina) {
        return Pagina.de(regioes.findAll(pagina).map(Regiao::saida));
    }

    public RegiaoSaida regiao(Long id) {
        return encontrarRegiao(id).saida();
    }

    @Transactional
    public RegiaoSaida salvarRegiao(Ator ator, Long id, RegiaoEntrada dados) {
        ator.exigirEquipe();
        Regiao regiao = id == null ? new Regiao() : encontrarRegiao(id);
        regiao.atualizar(dados);
        return regioes.save(regiao).saida();
    }

    @Transactional
    public void excluirRegiao(Ator ator, Long id) {
        ator.exigirEquipe();
        regioes.delete(encontrarRegiao(id));
    }

    public Pagina<PontoSaida> pontos(Pageable pagina) {
        return Pagina.de(pontos.findAll(pagina).map(PontoAgua::saida));
    }

    public Pagina<PontoSaida> pesquisarPontos(String busca, StatusPonto status, Pageable pagina) {
        String termo = busca.strip().toLowerCase(Locale.ROOT);
        Specification<PontoAgua> filtros = (ponto, consulta, cb) -> {
            var condicoes = new ArrayList<Predicate>();
            if (status != null) {
                condicoes.add(cb.equal(ponto.get("status"), status));
            }
            if (!termo.isEmpty()) {
                condicoes.add(cb.or(cb.gt(cb.locate(cb.lower(ponto.get("nome")), termo), 0),
                        cb.gt(cb.locate(cb.lower(ponto.get("cidade")), termo), 0)));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        return Pagina.de(pontos.findAll(filtros, pagina).map(PontoAgua::saida));
    }

    public long totalPontos() {
        return pontos.count();
    }

    public PontoSaida ponto(Long id) {
        return encontrarPonto(id).saida();
    }

    @Transactional
    public PontoSaida salvarPonto(Ator ator, Long id, PontoEntrada dados) {
        return gravarPonto(ator, id, dados, null);
    }

    @Transactional
    public PontoSaida salvarPontoNoPainel(Ator ator, Long id, PontoEntrada dados, String versao) {
        return gravarPonto(ator, id, dados, versao == null ? "" : versao);
    }

    private PontoSaida gravarPonto(Ator ator, Long id, PontoEntrada dados, String versao) {
        ator.exigirEquipe();
        PontoAgua ponto = id == null ? new PontoAgua() : encontrarPonto(id);
        if (id != null && versao != null && !ponto.saida().atualizadoEm().toString().equals(versao)) {
            throw ApiException.conflito("O ponto foi atualizado. Abra a edição novamente para conferir os dados.");
        }
        Regiao regiao = resolverRegiao(dados.regiaoId(), dados.cidade());
        ponto.atualizar(dados, regiao);
        return pontos.save(ponto).saida();
    }

    @Transactional
    public void atualizarDisponibilidade(Ator ator, Long id, StatusPonto status, int estoque) {
        ator.exigirEquipe();
        encontrarPonto(id).disponibilidade(status, estoque);
    }

    @Transactional
    public void excluirPonto(Ator ator, Long id) {
        ator.exigirEquipe();
        pontos.delete(encontrarPonto(id));
    }

    public Regiao resolverRegiao(Long id, String cidade) {
        if (id != null) {
            Regiao regiao = encontrarRegiao(id);
            if (!regiao.getCidade().equalsIgnoreCase(cidade.strip())) {
                throw ApiException.invalido("A região informada não pertence à cidade selecionada.");
            }
            return regiao;
        }
        var candidatas = regioes.findTop2ByCidadeIgnoreCaseOrderById(cidade.strip());
        return candidatas.size() == 1 ? candidatas.getFirst() : null;
    }

    private Regiao encontrarRegiao(Long id) {
        return regioes.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Região"));
    }

    private PontoAgua encontrarPonto(Long id) {
        return pontos.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Ponto de água"));
    }
}
