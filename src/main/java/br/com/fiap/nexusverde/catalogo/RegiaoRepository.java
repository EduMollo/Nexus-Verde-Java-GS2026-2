package br.com.fiap.nexusverde.catalogo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface RegiaoRepository extends JpaRepository<Regiao, Long> {
    List<Regiao> findTop2ByCidadeIgnoreCaseOrderById(String cidade);
}
