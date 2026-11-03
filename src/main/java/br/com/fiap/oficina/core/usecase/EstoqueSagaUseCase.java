package br.com.fiap.oficina.core.usecase;

import br.com.fiap.oficina.core.domain.enums.OperacaoEstoqueSaga;
import br.com.fiap.oficina.core.domain.enums.TipoMovimentacao;
import br.com.fiap.oficina.core.domain.mensageria.ContextoSaga;
import br.com.fiap.oficina.core.gateway.EstoqueSagaGateway;
import br.com.fiap.oficina.core.gateway.EstoqueSagaGateway.Movimento;
import br.com.fiap.oficina.core.gateway.MensageriaGateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EstoqueSagaUseCase {

    private static final System.Logger LOG = System.getLogger(EstoqueSagaUseCase.class.getName());

    private final EstoqueSagaGateway estoqueGateway;
    private final MensageriaGateway mensageria;

    public EstoqueSagaUseCase(EstoqueSagaGateway estoqueGateway, MensageriaGateway mensageria) {
        this.estoqueGateway = estoqueGateway;
        this.mensageria = mensageria;
    }

    public void baixar(ContextoSaga contexto, List<Movimento> itens) {
        Long osId = contexto.osId();
        // A devolução deixa um marcador: uma baixa atrasada não pode executar depois dela.
        if (estoqueGateway.existeOperacao(osId, OperacaoEstoqueSaga.DEVOLUCAO)) {
            LOG.log(System.Logger.Level.WARNING, "Baixa ignorada: estoque da OS " + osId + " já foi devolvido");
            return;
        }
        if (estoqueGateway.existeOperacao(osId, OperacaoEstoqueSaga.BAIXA)) {
            publicarMovimentos("EstoqueBaixado", contexto, estoqueGateway.listarMovimentos(osId, OperacaoEstoqueSaga.BAIXA));
            return;
        }

        Map<Long, Integer> solicitado = new TreeMap<>();
        itens.forEach(i -> solicitado.merge(i.produtoId(), i.quantidade(), Integer::sum));
        Map<Long, Integer> saldos = estoqueGateway.travarSaldos(solicitado.keySet());

        List<Map<String, Object>> faltantes = new ArrayList<>();
        solicitado.forEach((produtoId, quantidade) -> {
            int saldo = saldos.getOrDefault(produtoId, 0);
            if (saldo < quantidade) {
                faltantes.add(Map.of("produtoId", produtoId, "saldo", saldo, "solicitado", quantidade));
            }
        });
        if (!faltantes.isEmpty()) {
            LOG.log(System.Logger.Level.WARNING, "Estoque insuficiente para a OS " + osId + ": " + faltantes);
            mensageria.publicarEvento("EstoqueInsuficiente", contexto, Map.of("faltantes", faltantes));
            return;
        }

        estoqueGateway.marcarOperacao(osId, OperacaoEstoqueSaga.BAIXA);
        List<Movimento> movimentos = new ArrayList<>();
        solicitado.forEach((produtoId, quantidade) -> {
            Movimento movimento = new Movimento(produtoId, quantidade);
            estoqueGateway.registrar(osId, OperacaoEstoqueSaga.BAIXA, TipoMovimentacao.SAIDA, movimento,
                    "Saída automática - aprovação da OS " + osId);
            movimentos.add(movimento);
        });
        LOG.log(System.Logger.Level.INFO, "Baixa de estoque processada na OS " + osId + ": " + movimentos);
        publicarMovimentos("EstoqueBaixado", contexto, movimentos);
    }

    public void devolver(ContextoSaga contexto, String motivo) {
        Long osId = contexto.osId();
        if (estoqueGateway.existeOperacao(osId, OperacaoEstoqueSaga.DEVOLUCAO)) {
            publicarMovimentos("EstoqueDevolvido", contexto, estoqueGateway.listarMovimentos(osId, OperacaoEstoqueSaga.DEVOLUCAO));
            return;
        }
        estoqueGateway.marcarOperacao(osId, OperacaoEstoqueSaga.DEVOLUCAO);
        List<Movimento> baixados = estoqueGateway.listarMovimentos(osId, OperacaoEstoqueSaga.BAIXA);
        for (Movimento movimento : baixados) {
            estoqueGateway.registrar(osId, OperacaoEstoqueSaga.DEVOLUCAO, TipoMovimentacao.ENTRADA, movimento,
                    "Devolução - OS " + osId + ": " + motivo);
        }
        LOG.log(System.Logger.Level.INFO, "Estoque da OS " + osId + " devolvido: " + baixados);
        publicarMovimentos("EstoqueDevolvido", contexto, baixados);
    }

    private void publicarMovimentos(String tipo, ContextoSaga contexto, List<Movimento> movimentos) {
        mensageria.publicarEvento(tipo, contexto, Map.of("movimentacoes", movimentos.stream()
                .map(m -> Map.<String, Object>of("produtoId", m.produtoId(), "quantidade", m.quantidade()))
                .toList()));
    }
}
