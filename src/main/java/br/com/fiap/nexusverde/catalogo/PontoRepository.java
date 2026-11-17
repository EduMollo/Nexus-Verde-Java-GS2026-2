package br.com.fiap.nexusverde.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface PontoRepository extends JpaRepository<PontoAgua, Long>, JpaSpecificationExecutor<PontoAgua> {}
