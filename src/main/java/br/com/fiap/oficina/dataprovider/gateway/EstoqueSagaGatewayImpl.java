package br.com.fiap.oficina.dataprovider.gateway;

import br.com.fiap.oficina.core.domain.enums.OperacaoEstoqueSaga;
import br.com.fiap.oficina.core.domain.enums.TipoMovimentacao;
import br.com.fiap.oficina.core.gateway.EstoqueGateway;
import br.com.fiap.oficina.core.gateway.EstoqueSagaGateway;
import br.com.fiap.oficina.dataprovider.persistence.entity.MovimentacaoEstoque;
import br.com.fiap.oficina.dataprovider.persistence.entity.OperacaoEstoqueRegistro;
import br.com.fiap.oficina.dataprovider.persistence.entity.SaldoEstoque;
import br.com.fiap.oficina.dataprovider.persistence.repository.MovimentacaoEstoqueRepository;
import br.com.fiap.oficina.dataprovider.persistence.repository.OperacaoEstoqueRegistroRepository;
import br.com.fiap.oficina.dataprovider.persistence.repository.ProdutoRepository;
import br.com.fiap.oficina.dataprovider.persistence.repository.SaldoEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EstoqueSagaGatewayImpl implements EstoqueSagaGateway {

    private final OperacaoEstoqueRegistroRepository operacaoRepository;
    private final SaldoEstoqueRepository saldoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueGateway estoqueGateway;

    @Override
    @Transactional(readOnly = true)
    public boolean existeOperacao(Long osId, OperacaoEstoqueSaga operacao) {
        return operacaoRepository.existsByOsIdAndOperacao(osId, operacao);
    }

    @Override
    @Transactional
    public void marcarOperacao(Long osId, OperacaoEstoqueSaga operacao) {
        operacaoRepository.save(OperacaoEstoqueRegistro.builder().osId(osId).operacao(operacao).build());
    }

    @Override
    @Transactional
    public Map<Long, Integer> travarSaldos(Collection<Long> produtoIds) {
        if (produtoIds.isEmpty()) {
            return Map.of();
        }
        return saldoRepository.travarPorProdutos(produtoIds).stream()
                .collect(Collectors.toMap(s -> s.getProduto().getId(), SaldoEstoque::getQuantidade));
    }

    @Override
    @Transactional
    public void registrar(Long osId, OperacaoEstoqueSaga operacao, TipoMovimentacao tipo,
                          Movimento movimento, String motivo) {
        movimentacaoRepository.saveAndFlush(MovimentacaoEstoque.builder()
                .produto(produtoRepository.getReferenceById(movimento.produtoId()))
                .tipo(tipo)
                .quantidade(movimento.quantidade())
                .motivo(motivo)
                .ordemServicoId(osId)
                .operacaoSaga(operacao)
                .build());
        estoqueGateway.sincronizarSaldo(movimento.produtoId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Movimento> listarMovimentos(Long osId, OperacaoEstoqueSaga operacao) {
        return movimentacaoRepository.findByOrdemServicoIdAndOperacaoSaga(osId, operacao).stream()
                .map(m -> new Movimento(m.getProduto().getId(), m.getQuantidade()))
                .toList();
    }
}
