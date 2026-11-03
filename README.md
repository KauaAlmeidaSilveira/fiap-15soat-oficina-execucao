# Oficina — Execução e Produção

Microsserviço da Fase 4 do Tech Challenge: fila de execução, diagnóstico, reparo, produto e estoque.

> Em construção. Nesta etapa contém produto e estoque, migrados do monólito da Fase 3. A documentação geral está em [`fiap-15soat-oficina-os`](https://github.com/KauaAlmeidaSilveira/fiap-15soat-oficina-os).

## Rodar os testes

Requer Java 21 e Maven 3.9+. A chave pública JWT não é versionada. Este serviço só valida tokens, então não usa chave privada:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 | openssl rsa -pubout -out src/main/resources/jwt.public.key
mvn verify
```
