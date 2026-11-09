package br.com.fiap.oficina.dataprovider.persistence.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import br.com.fiap.oficina.dataprovider.persistence.entity.SaldoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaldoEstoqueRepository extends JpaRepository<SaldoEstoque, Long> {
    Optional<SaldoEstoque> findByProdutoId(Long produtoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SaldoEstoque s JOIN FETCH s.produto WHERE s.produto.id IN :produtoIds ORDER BY s.produto.id")
    List<SaldoEstoque> travarPorProdutos(@Param("produtoIds") Collection<Long> produtoIds);
}
