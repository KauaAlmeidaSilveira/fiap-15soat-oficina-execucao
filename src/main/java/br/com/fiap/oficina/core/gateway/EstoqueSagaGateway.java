package br.com.fiap.oficina.core.gateway;

import br.com.fiap.oficina.core.domain.enums.OperacaoEstoqueSaga;
import br.com.fiap.oficina.core.domain.enums.TipoMovimentacao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EstoqueSagaGateway {

    record Movimento(Long produtoId, Integer quantidade) {}

    boolean existeOperacao(Long osId, OperacaoEstoqueSaga operacao);

    void marcarOperacao(Long osId, OperacaoEstoqueSaga operacao);

    /** Trava os saldos até o fim da transação; produto sem saldo volta com zero. */
    Map<Long, Integer> travarSaldos(Collection<Long> produtoIds);

    void registrar(Long osId, OperacaoEstoqueSaga operacao, TipoMovimentacao tipo, Movimento movimento, String motivo);

    List<Movimento> listarMovimentos(Long osId, OperacaoEstoqueSaga operacao);
}
