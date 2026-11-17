package br.com.fiap.nexusverde.auth;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByIdAndPapelAndAtivoTrue(Long id, Papel papel);

    @Query("""
            select u from Usuario u where u.papel = :papel and u.ativo = true
            and (:busca = '' or lower(u.email) = :busca or locate(:busca, lower(u.nome)) > 0)
            """)
    Page<Usuario> pesquisarAtivos(Papel papel, String busca, Pageable pagina);
    boolean existsByEmail(String email);
    boolean existsByIdAndAtivoTrueAndVersaoAcesso(Long id, long versaoAcesso);
}
