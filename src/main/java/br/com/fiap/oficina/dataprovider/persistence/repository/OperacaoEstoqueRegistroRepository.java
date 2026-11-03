package br.com.fiap.oficina.dataprovider.persistence.repository;

import br.com.fiap.oficina.core.domain.enums.OperacaoEstoqueSaga;
import br.com.fiap.oficina.dataprovider.persistence.entity.OperacaoEstoqueRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoEstoqueRegistroRepository extends JpaRepository<OperacaoEstoqueRegistro, Long> {

    boolean existsByOsIdAndOperacao(Long osId, OperacaoEstoqueSaga operacao);
}
