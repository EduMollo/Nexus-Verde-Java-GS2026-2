package br.com.fiap.nexusverde.solicitacao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {
    List<HistoricoStatus> findBySolicitacaoIdOrderByIdDesc(Long solicitacaoId);
}
