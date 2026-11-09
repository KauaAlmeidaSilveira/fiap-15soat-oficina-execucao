# Oficina — Execução e Produção

Microsserviço da Fase 4 do Tech Challenge: fila de execução, diagnóstico, reparo, produto e estoque. Recebe comandos da Saga pelo SQS e responde com eventos. A estratégia geral está no [OS Service](https://github.com/KauaAlmeidaSilveira/fiap-15soat-oficina-os).

- **Fila:** `GET /api/execucoes` e `POST /api/execucoes/{osId}/diagnostico|reparo/iniciar|concluir|finalizar`.
- **Estoque na Saga:** baixa tudo ou nada, com os saldos travados na transação. Baixa e devolução são idempotentes, e uma baixa atrasada depois da devolução é recusada.
- **Banco:** PostgreSQL próprio, com o saldo protegido por transação.
- **Falha simulada:** o profile `simulacao` expõe `PUT /api/simulacao/falha?ponto=ENFILEIRAR_REPARO`, nunca ativo em produção.

## Rodar os testes

Requer Java 21, Maven 3.9+ e Docker para o Testcontainers. Este serviço só valida tokens, então não usa chave privada de sessão:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 | openssl rsa -pubout -out src/main/resources/jwt.public.key
mvn verify
```
