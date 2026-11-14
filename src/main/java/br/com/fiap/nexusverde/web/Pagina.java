package br.com.fiap.nexusverde.web;

import java.util.List;
import org.springframework.data.domain.Page;

public record Pagina<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    public static <T> Pagina<T> de(Page<T> page) {
        return new Pagina<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
