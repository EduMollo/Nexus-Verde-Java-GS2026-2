package br.com.fiap.nexusverde.painel;

import java.security.Principal;
import java.util.UUID;

import br.com.fiap.nexusverde.auth.Ator;
import br.com.fiap.nexusverde.auth.CidadaosPainel;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.Entrada;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.Tipo;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.Urgencia;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoService;
import br.com.fiap.nexusverde.web.ApiException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/painel/solicitacoes")
class RegistroSolicitacaoPainelController {
    private static final String LISTA = "redirect:/painel/solicitacoes";
    private static final String FORMA = "painel/solicitacao-formulario";
    private final SolicitacaoService pedidos;
    private final CidadaosPainel cidadaos;
    private final boolean demo;

    RegistroSolicitacaoPainelController(SolicitacaoService pedidos, CidadaosPainel cidadaos,
                                       @Value("${nexus.demo}") boolean demo) {
        this.pedidos = pedidos;
        this.cidadaos = cidadaos;
        this.demo = demo;
    }

    @ModelAttribute("demo")
    boolean demonstracao() {
        return demo;
    }

    @GetMapping("/nova")
    String escolherCidadao(Principal principal, Model model, @RequestParam(defaultValue = "") String busca,
                           @RequestParam(defaultValue = "0") int pagina) {
        model.addAttribute("cidadaos", cidadaos.pesquisar(PainelController.ator(principal), busca,
                PageRequest.of(Math.max(0, pagina), 10, Sort.by("nome", "id"))));
        model.addAttribute("busca", busca);
        return "painel/solicitacao-cidadao";
    }

    @GetMapping("/nova/{cidadaoId}")
    String nova(Principal principal, @PathVariable long cidadaoId, Model model, RedirectAttributes retorno) {
        Ator ator = PainelController.ator(principal);
        try {
            var cidadao = cidadaos.consultar(ator, cidadaoId);
            model.addAttribute("dados", new Entrada(null, "", null, null, null, "",
                    cidadao.cidade(), null, null, null));
            return formularioNovo(model, cidadao, UUID.randomUUID());
        } catch (ApiException e) {
            retorno.addFlashAttribute("erro", e.getMessage());
            return LISTA + "/nova";
        }
    }

    @PostMapping("/nova/{cidadaoId}")
    String criar(Principal principal, @PathVariable long cidadaoId, @RequestParam UUID registro,
                 @Valid @ModelAttribute("dados") Entrada dados, BindingResult erros, Model model,
                 RedirectAttributes retorno) {
        Ator ator = PainelController.ator(principal);
        if (!erros.hasErrors()) {
            try {
                return criado(retorno, pedidos.criarNoPainel(ator, cidadaoId, dados, registro));
            } catch (ApiException e) {
                erros.reject("solicitacao.invalida", e.getMessage());
            } catch (DataIntegrityViolationException e) {
                var existente = pedidos.localizarRegistroPainel(ator, cidadaoId, registro);
                if (existente.isPresent()) {
                    return criado(retorno, existente.get());
                }
                throw e;
            }
        }
        try {
            return formularioNovo(model, cidadaos.consultar(ator, cidadaoId), registro);
        } catch (ApiException e) {
            retorno.addFlashAttribute("erro", e.getMessage());
            return LISTA + "/nova";
        }
    }

    @GetMapping("/{id}/editar")
    String editar(Principal principal, @PathVariable long id, Model model, RedirectAttributes retorno) {
        try {
            var detalhe = pedidos.detalheParaEdicao(PainelController.ator(principal), id);
            var pedido = detalhe.pedido();
            model.addAttribute("dados", new Entrada(pedido.tipo(), pedido.descricao(), pedido.quantidadePessoas(),
                    pedido.urgencia(), pedido.grupoVulneravel(), pedido.endereco(), pedido.cidade(), pedido.latitude(),
                    pedido.longitude(), null));
            return formularioEdicao(model, id, detalhe.versao(), pedido.solicitante().nome());
        } catch (ApiException e) {
            retorno.addFlashAttribute("erro", e.getMessage());
            return e.status().value() == 404 ? LISTA : LISTA + "/" + id;
        }
    }

    @PostMapping("/{id}/editar")
    String gravar(Principal principal, @PathVariable long id, @RequestParam long versao,
                  @Valid @ModelAttribute("dados") Entrada dados, BindingResult erros, Model model,
                  RedirectAttributes retorno) {
        Ator ator = PainelController.ator(principal);
        try {
            if (erros.hasErrors()) {
                var pedido = pedidos.detalheParaEdicao(ator, id).pedido();
                return formularioEdicao(model, id, versao, pedido.solicitante().nome());
            }
            pedidos.editarNoPainel(ator, id, versao, dados);
            retorno.addFlashAttribute("sucesso", "Solicitação atualizada.");
        } catch (ApiException e) {
            retorno.addFlashAttribute("erro", e.getMessage());
            if (e.status().value() == 404) {
                return LISTA;
            }
        } catch (OptimisticLockingFailureException e) {
            retorno.addFlashAttribute("erro", "O pedido mudou. Abra a edição novamente para conferir os dados.");
        }
        return LISTA + "/" + id;
    }

    private static String criado(RedirectAttributes retorno, long id) {
        retorno.addFlashAttribute("sucesso", "Solicitação cadastrada. Protocolo #" + id + ".");
        return LISTA + "/" + id;
    }

    private static String formularioNovo(Model model, CidadaosPainel.Resumo cidadao, UUID registro) {
        model.addAttribute("cidadaoId", cidadao.id());
        model.addAttribute("solicitante", cidadao.nome());
        model.addAttribute("registro", registro);
        return formulario(model);
    }

    private static String formularioEdicao(Model model, long pedidoId, long versao, String solicitante) {
        model.addAttribute("pedidoId", pedidoId);
        model.addAttribute("versao", versao);
        model.addAttribute("solicitante", solicitante);
        return formulario(model);
    }

    private static String formulario(Model model) {
        model.addAttribute("tipos", Tipo.values());
        model.addAttribute("urgencias", Urgencia.values());
        return FORMA;
    }
}
