package br.com.fiap.nexusverde.solicitacao;

import java.util.Optional;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long>, JpaSpecificationExecutor<Solicitacao> {
    @Override
    @EntityGraph(attributePaths = "solicitante")
    Page<Solicitacao> findAll(Specification<Solicitacao> filtros, Pageable pagina);

    long countByStatusAndExcluidaEmIsNull(Status status);
    @EntityGraph(attributePaths = "solicitante")
    Optional<Solicitacao> findByIdAndExcluidaEmIsNull(Long id);

    @EntityGraph(attributePaths = "solicitante")
    Page<Solicitacao> findByExcluidaEmIsNull(Pageable pagina);

    @EntityGraph(attributePaths = "solicitante")
    Page<Solicitacao> findBySolicitanteIdAndExcluidaEmIsNull(Long usuario, Pageable pagina);

    @EntityGraph(attributePaths = "solicitante")
    Page<Solicitacao> findByStatusAndExcluidaEmIsNullAndTriadaEmIsNotNull(Status status, Pageable pagina);
}
