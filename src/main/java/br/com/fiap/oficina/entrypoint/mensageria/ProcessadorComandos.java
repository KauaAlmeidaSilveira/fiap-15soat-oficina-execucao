package br.com.fiap.oficina.entrypoint.mensageria;

import br.com.fiap.oficina.core.domain.mensageria.ContextoSaga;
import br.com.fiap.oficina.core.gateway.EstoqueSagaGateway.Movimento;
import br.com.fiap.oficina.core.usecase.EstoqueSagaUseCase;
import br.com.fiap.oficina.core.usecase.FilaExecucaoUseCase;
import br.com.fiap.oficina.dataprovider.mensageria.Envelope;
import br.com.fiap.oficina.dataprovider.mensageria.MensagemProcessada;
import br.com.fiap.oficina.dataprovider.mensageria.MensagemProcessadaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Efeito, inbox e outbox na mesma transação: uma falha devolve a mensagem à fila, sem efeito parcial. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessadorComandos {

    private final FilaExecucaoUseCase filaUseCase;
    private final EstoqueSagaUseCase estoqueUseCase;
    private final MensagemProcessadaRepository inbox;

    @Transactional
    public void processar(Envelope comando) {
        if (inbox.existsById(comando.messageId())) {
            log.info("Comando {} ({}) da OS {} já processado", comando.messageId(), comando.tipo(), comando.osId());
            return;
        }
        ContextoSaga contexto = new ContextoSaga(comando.sagaId(), comando.osId(), comando.correlationId(), comando.messageId());
        JsonNode payload = comando.payload();
        switch (comando.tipo()) {
            case "EnfileirarDiagnostico" -> filaUseCase.enfileirarDiagnostico(contexto,
                    payload.path("numero").asText(), payload.path("descricaoProblema").asText(null));
            case "EnfileirarReparo" -> filaUseCase.enfileirarReparo(contexto, payload.path("numero").asText());
            case "CancelarReparo" -> filaUseCase.cancelarReparo(contexto, payload.path("motivo").asText());
            case "BaixarEstoque" -> estoqueUseCase.baixar(contexto, itens(payload));
            case "DevolverEstoque" -> estoqueUseCase.devolver(contexto, payload.path("motivo").asText());
            default -> log.warn("Comando {} desconhecido na OS {}: ignorado", comando.tipo(), comando.osId());
        }
        inbox.save(new MensagemProcessada(comando.messageId(), Instant.now()));
    }

    private List<Movimento> itens(JsonNode payload) {
        List<Movimento> itens = new ArrayList<>();
        payload.path("itens").forEach(i -> itens.add(new Movimento(i.path("produtoId").asLong(), i.path("quantidade").asInt())));
        return itens;
    }
}
