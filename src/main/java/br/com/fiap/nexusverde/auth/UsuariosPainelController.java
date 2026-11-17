package br.com.fiap.nexusverde.auth;

import java.security.Principal;

import br.com.fiap.nexusverde.web.ApiException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/painel/usuarios")
class UsuariosPainelController {
    private static final String LISTA = "redirect:/painel/usuarios";
    private static final String FORMULARIO = "painel/usuario-formulario";
    private final GestaoUsuarios usuarios;
    private final boolean demo;

    UsuariosPainelController(GestaoUsuarios usuarios, @Value("${nexus.demo}") boolean demo) {
        this.usuarios = usuarios;
        this.demo = demo;
    }

    @ModelAttribute("demo")
    boolean demonstracao() {
        return demo;
    }

    @GetMapping
    String listar(Principal principal, Model model, @RequestParam(defaultValue = "") String busca,
                  @RequestParam(required = false) Papel papel, @RequestParam(required = false) Boolean ativo,
                  @RequestParam(defaultValue = "0") int pagina) {
        model.addAttribute("usuarios", usuarios.pesquisar(busca, papel, ativo,
                PageRequest.of(Math.max(0, pagina), 15, Sort.by("nome", "id"))));
        model.addAttribute("busca", busca);
        model.addAttribute("papel", papel);
        model.addAttribute("ativoFiltro", ativo);
        model.addAttribute("idGestor", Long.parseLong(principal.getName()));
        return "painel/usuarios";
    }

    @GetMapping("/novo")
    String novo(Model model) {
        model.addAttribute("dados", new CadastroUsuario("", "", Papel.CIDADAO, "", "", "", ""));
        return FORMULARIO;
    }

    @PostMapping("/novo")
    String cadastrar(@Valid @ModelAttribute("dados") CadastroUsuario dados, BindingResult erros,
                     RedirectAttributes retorno) {
        if (erros.hasErrors()) {
            return FORMULARIO;
        }
        try {
            usuarios.cadastrar(dados);
            retorno.addFlashAttribute("sucesso", "Usuário cadastrado.");
            retorno.addAttribute("busca", AuthService.normalizarEmail(dados.email()));
            return LISTA;
        } catch (ApiException e) {
            erros.reject("usuario.invalido", e.getMessage());
            return FORMULARIO;
        }
    }

    @PostMapping("/{id}/situacao")
    String situacao(@PathVariable Long id, @RequestParam boolean ativo, RedirectAttributes retorno) {
        try {
            usuarios.definirAtivo(id, ativo);
            retorno.addFlashAttribute("sucesso", ativo ? "Usuário reativado." : "Usuário desativado.");
        } catch (ApiException e) {
            retorno.addFlashAttribute("erro", e.getMessage());
        } catch (OptimisticLockingFailureException e) {
            retorno.addFlashAttribute("erro", "A situação da conta mudou. Confira os dados e tente novamente.");
        }
        return LISTA;
    }
}
