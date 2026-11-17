package br.com.fiap.nexusverde.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class InicioController {
    @GetMapping("/")
    String inicio() {
        return "redirect:/painel";
    }
}
