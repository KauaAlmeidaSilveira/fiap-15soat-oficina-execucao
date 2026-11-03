package br.com.fiap.oficina.entrypoint.mensageria;

import br.com.fiap.oficina.core.domain.entity.Produto;
import br.com.fiap.oficina.core.domain.enums.TipoProduto;
import br.com.fiap.oficina.core.usecase.ProdutoUseCase;
import br.com.fiap.oficina.dataprovider.mensageria.Envelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class MensageriaFluxoIT {

    private static final String COMANDOS = "it-execucao-comandos";
    private static final String EVENTOS = "it-saga-eventos";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(LocalStackContainer.Service.SQS);

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) throws Exception {
        try (SqsClient sqs = sqsCliente()) {
            sqs.createQueue(b -> b.queueName(COMANDOS));
            sqs.createQueue(b -> b.queueName(EVENTOS));
        }
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("app.mensageria.habilitada", () -> "true");
        registry.add("app.mensageria.fila-comandos", () -> COMANDOS);
        registry.add("app.mensageria.fila-eventos", () -> EVENTOS);
        registry.add("spring.cloud.aws.sqs.enabled", () -> "true");
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localstack.getEndpoint().toString());
        registry.add("spring.cloud.aws.region.static", localstack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
    }

    private static SqsClient sqsCliente() {
        return SqsClient.builder()
                .endpointOverride(localstack.getEndpoint())
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .build();
    }

    @Autowired ProdutoUseCase produtoUseCase;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("Comando real na fila baixa o estoque no PostgreSQL e o evento sai pela outbox")
    void comandoViraEventoPelaFila() throws Exception {
        Long peca = produtoUseCase.criar(Produto.builder().nome("Velas").tipo(TipoProduto.PECA)
                .precoUnitario(BigDecimal.TEN).ativo(true).build(), 10).getId();
        UUID sagaId = UUID.randomUUID();
        Envelope comando = new Envelope(UUID.randomUUID(), "BaixarEstoque", 1, sagaId, 7L, "corr-it", null, Instant.now(),
                objectMapper.valueToTree(Map.of("itens", List.of(Map.of("produtoId", peca, "quantidade", 4)))));

        try (SqsClient sqs = sqsCliente()) {
            String comandos = sqs.getQueueUrl(b -> b.queueName(COMANDOS)).queueUrl();
            String eventos = sqs.getQueueUrl(b -> b.queueName(EVENTOS)).queueUrl();
            String corpo = objectMapper.writeValueAsString(comando);
            sqs.sendMessage(b -> b.queueUrl(comandos).messageBody(corpo));

            Envelope evento = null;
            long limite = System.currentTimeMillis() + 30_000;
            while (evento == null && System.currentTimeMillis() < limite) {
                List<Message> recebidas = sqs.receiveMessage(b -> b.queueUrl(eventos).waitTimeSeconds(2)).messages();
                if (!recebidas.isEmpty()) {
                    evento = objectMapper.readValue(recebidas.get(0).body(), Envelope.class);
                }
            }

            assertThat(evento).as("evento na fila da Saga").isNotNull();
            assertThat(evento.tipo()).isEqualTo("EstoqueBaixado");
            assertThat(evento.sagaId()).isEqualTo(sagaId);
            assertThat(evento.correlationId()).isEqualTo("corr-it");
            assertThat(evento.causationId()).isEqualTo(comando.messageId());
            assertThat(produtoUseCase.buscarPorId(peca).getSaldoEstoque()).isEqualTo(6);
        }
    }
}
