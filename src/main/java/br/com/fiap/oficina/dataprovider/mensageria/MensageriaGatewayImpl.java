package br.com.fiap.oficina.dataprovider.mensageria;

import br.com.fiap.oficina.core.domain.mensageria.ContextoSaga;
import br.com.fiap.oficina.core.gateway.MensageriaGateway;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class MensageriaGatewayImpl implements MensageriaGateway {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final String filaEventos;

    public MensageriaGatewayImpl(OutboxRepository outboxRepository, ObjectMapper objectMapper,
                                 @Value("${app.mensageria.fila-eventos}") String filaEventos) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.filaEventos = filaEventos;
    }

    @Override
    @Transactional
    public void publicarEvento(String tipo, ContextoSaga contexto, Map<String, Object> payload) {
        Instant agora = Instant.now();
        UUID id = UUID.randomUUID();
        Envelope envelope = new Envelope(id, tipo, 1, contexto.sagaId(), contexto.osId(), contexto.correlationId(),
                contexto.causationId(), agora, objectMapper.valueToTree(payload));
        try {
            outboxRepository.save(OutboxMensagem.builder()
                    .id(id)
                    .fila(filaEventos)
                    .tipo(tipo)
                    .corpo(objectMapper.writeValueAsString(envelope))
                    .criadoEm(agora)
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Evento " + tipo + " da OS " + contexto.osId() + " inválido", e);
        }
    }
}
