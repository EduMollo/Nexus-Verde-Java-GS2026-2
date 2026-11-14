package br.com.fiap.nexusverde.solicitacao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventoRepository extends JpaRepository<EventoPendente, UUID> {
    List<EventoPendente> findTop20ByPublicadoEmIsNullOrderByCriadoEmAsc();
}
