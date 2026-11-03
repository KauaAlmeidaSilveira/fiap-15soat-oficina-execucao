package br.com.fiap.oficina.core.usecase;

import br.com.fiap.oficina.core.domain.entity.ItemFila;
import br.com.fiap.oficina.core.domain.enums.EtapaExecucao;
import br.com.fiap.oficina.core.domain.enums.StatusItemFila;
import br.com.fiap.oficina.core.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.core.domain.mensageria.ContextoSaga;
import br.com.fiap.oficina.core.gateway.FilaExecucaoGateway;
import br.com.fiap.oficina.core.gateway.MensageriaGateway;
import br.com.fiap.oficina.core.gateway.SimuladorFalhasGateway;

import java.time.Clock;
import java.util.List;
import java.util.Map;

public class FilaExecucaoUseCase {

    static final String FALHA_ENFILEIRAR_REPARO = "ENFILEIRAR_REPARO";

    private static final System.Logger LOG = System.getLogger(FilaExecucaoUseCase.class.getName());

    private final FilaExecucaoGateway filaGateway;
    private final MensageriaGateway mensageria;
    private final SimuladorFalhasGateway simulador;
    private final Clock clock;

    public FilaExecucaoUseCase(FilaExecucaoGateway filaGateway, MensageriaGateway mensageria,
                               SimuladorFalhasGateway simulador, Clock clock) {
        this.filaGateway = filaGateway;
        this.mensageria = mensageria;
        this.simulador = simulador;
        this.clock = clock;
    }

    public void enfileirarDiagnostico(ContextoSaga contexto, String numeroOs, String descricao) {
        enfileirar(contexto, numeroOs, descricao, EtapaExecucao.DIAGNOSTICO);
        mensageria.publicarEvento("DiagnosticoEnfileirado", contexto, Map.of());
    }

    public void enfileirarReparo(ContextoSaga contexto, String numeroOs) {
        if (simulador.deveFalhar(FALHA_ENFILEIRAR_REPARO)) {
            LOG.log(System.Logger.Level.WARNING, "Falha simulada ao enfileirar o reparo da OS " + contexto.osId());
            mensageria.publicarEvento("ReparoRecusado", contexto, Map.of("motivo", "Falha simulada"));
            return;
        }
        enfileirar(contexto, numeroOs, null, EtapaExecucao.REPARO);
        mensageria.publicarEvento("ReparoEnfileirado", contexto, Map.of());
    }

    public List<ItemFila> listarAbertos() {
        return filaGateway.listarAbertos();
    }

    public ItemFila iniciarDiagnostico(Long osId) {
        ItemFila item = buscar(osId, EtapaExecucao.DIAGNOSTICO);
        item.iniciar(clock.instant());
        ItemFila salvo = filaGateway.salvar(item);
        publicarDoOperador("DiagnosticoIniciado", salvo, Map.of("iniciadoEm", salvo.getIniciadoEm().toString()));
        return salvo;
    }

    public ItemFila concluirDiagnostico(Long osId) {
        ItemFila item = buscar(osId, EtapaExecucao.DIAGNOSTICO);
        // Depois de uma falha ao gerar o orçamento, a oficina conclui de novo o mesmo diagnóstico (D17).
        if (item.getStatus() != StatusItemFila.CONCLUIDO) {
            item.concluir(clock.instant());
            item = filaGateway.salvar(item);
        }
        publicarDoOperador("DiagnosticoConcluido", item, Map.of("concluidoEm", item.getConcluidoEm().toString()));
        return item;
    }

    public ItemFila iniciarReparo(Long osId) {
        ItemFila item = buscar(osId, EtapaExecucao.REPARO);
        item.iniciar(clock.instant());
        ItemFila salvo = filaGateway.salvar(item);
        publicarDoOperador("ReparoIniciado", salvo, Map.of("iniciadoEm", salvo.getIniciadoEm().toString()));
        return salvo;
    }

    public ItemFila finalizarReparo(Long osId) {
        ItemFila item = buscar(osId, EtapaExecucao.REPARO);
        item.concluir(clock.instant());
        ItemFila salvo = filaGateway.salvar(item);
        publicarDoOperador("ReparoFinalizado", salvo, Map.of("finalizadoEm", salvo.getConcluidoEm().toString()));
        return salvo;
    }

    public void cancelarReparo(ContextoSaga contexto, String numeroOs, String motivo) {
        // Sem item: o EnfileirarReparo atrasou e pode chegar depois. O marcador evita um item fantasma na fila.
        enfileirar(contexto, numeroOs, null, EtapaExecucao.REPARO);
        ItemFila item = buscar(contexto.osId(), EtapaExecucao.REPARO);
        if (item.getStatus() == StatusItemFila.CONCLUIDO) {
            LOG.log(System.Logger.Level.INFO,
                    "Cancelamento recusado: reparo da OS " + contexto.osId() + " já finalizado");
            mensageria.publicarEvento("ReparoJaFinalizado", contexto,
                    Map.of("finalizadoEm", item.getConcluidoEm().toString()));
            return;
        }
        if (item.getStatus() != StatusItemFila.CANCELADO) {
            item.cancelar(clock.instant());
            item = filaGateway.salvar(item);
            LOG.log(System.Logger.Level.INFO, "Reparo da OS " + contexto.osId() + " cancelado: " + motivo);
        }
        mensageria.publicarEvento("ReparoCancelado", contexto, Map.of("canceladoEm", item.getConcluidoEm().toString()));
    }

    private void enfileirar(ContextoSaga contexto, String numeroOs, String descricao, EtapaExecucao etapa) {
        if (filaGateway.buscar(contexto.osId(), etapa).isPresent()) {
            return;
        }
        filaGateway.salvar(ItemFila.builder()
                .osId(contexto.osId())
                .numeroOs(numeroOs)
                .sagaId(contexto.sagaId())
                .correlationId(contexto.correlationId())
                .etapa(etapa)
                .descricao(descricao)
                .build());
        LOG.log(System.Logger.Level.INFO, "OS " + contexto.osId() + " entrou na fila de " + etapa);
    }

    private ItemFila buscar(Long osId, EtapaExecucao etapa) {
        return filaGateway.buscar(osId, etapa)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "A OS " + osId + " não está na fila de " + etapa));
    }

    private void publicarDoOperador(String tipo, ItemFila item, Map<String, Object> payload) {
        mensageria.publicarEvento(tipo,
                new ContextoSaga(item.getSagaId(), item.getOsId(), item.getCorrelationId(), null), payload);
    }
}
