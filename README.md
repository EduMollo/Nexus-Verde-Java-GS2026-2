# Nexus Verde · Java Advanced · GS 2026/2

CitizenTech para ajudar moradores afetados por interrupções de abastecimento a localizar pontos de distribuição e solicitar água. A equipe acompanha a disponibilidade, organiza a triagem e registra o andamento. Os dados da carga acadêmica são fictícios.

[**Video**](https://youtu.be/4kTnQgR9ZAs)

[**Painel da equipe**](https://nexus-verde-java-gs2026-2-tprk.onrender.com/painel)

## Demonstração online

- [API do Mobile](https://nexus-verde-java-gs2026-2-tprk.onrender.com/api)
- [Swagger](https://nexus-verde-java-gs2026-2-tprk.onrender.com/swagger-ui/index.html)

O painel tem páginas próprias para [solicitações](https://nexus-verde-java-gs2026-2-tprk.onrender.com/painel/solicitacoes) e [pontos de água](https://nexus-verde-java-gs2026-2-tprk.onrender.com/painel/pontos-agua). A ficha de cada solicitação registra as novas mudanças de status e exige motivo no cancelamento pelo painel. A equipe pode buscar pedidos e cadastrar, editar ou atualizar o estoque dos pontos.

A área **Usuários** fica disponível para a conta principal definida por `EQUIPE_EMAIL`. Ela permite cadastrar contas de equipe ou cidadão, localizar usuários e gerenciar a situação de suas contas. Desativar uma conta invalida a sessão do painel e os tokens da API emitidos anteriormente. Após a reativação, o usuário deve entrar novamente.

A equipe também pode abrir solicitações pelo painel. Em **Nova solicitação**, selecione um cidadão ativo e preencha a situação e o endereço. A conta principal pode cadastrar um cidadão e seguir direto para o pedido. A solicitação aparece para o cidadão no app, recebe um protocolo e pode ser corrigida pela equipe enquanto estiver aberta.

Nos testes do Mobile, configure a URL base com o sufixo `/api`. A conta de avaliação do ambiente online é distribuída pela equipe.

## Executar

Requisitos: JDK 21 e Maven Wrapper. A primeira execução baixa as dependências.

~~~powershell
$env:SPRING_PROFILES_ACTIVE = "demo"
$env:EQUIPE_EMAIL = "equipe@nexusverde.example"
$env:EQUIPE_SENHA = "NexusLocal-2026"
.\mvnw.cmd spring-boot:run
~~~

No Linux/macOS, exporte as mesmas variáveis e use `./mvnw spring-boot:run`. Esse modo usa H2 em memória. O usuário `equipe@nexusverde.example` e a senha acima são exclusivos da demonstração local. Cadastre o cidadão em `POST /api/auth/register`.

Para reproduzir com PostgreSQL e RabbitMQ, execute `docker compose up --build -d`. O painel é acessível em `/painel`, a documentação em `/swagger-ui/index.html`, o OpenAPI em `/v3/api-docs` e a saúde em `/actuator/health`. A API usa a base `http://localhost:8080/api`.

A análise generativa é habilitada com `NEXUS_CHAT_MODEL=openai` e `OPENAI_API_KEY`. `OPENAI_MODEL` permite escolher o modelo e `OPENAI_BASE_URL` permite um provedor compatível. O painel usa login de equipe com sessão e CSRF. O Mobile usa JWT.

## API do Mobile

Envie `Authorization: Bearer <token>` nas operações protegidas. Os JWTs expiram em duas horas. A resposta de login e cadastro é `{ "token": "...", "usuario": { "id": 1, "nome": "...", "email": "...", "papel": "CIDADAO", "telefone": null, "cidade": "Canoas" } }`.

| Operação | Rota relativa a /api | Acesso |
|---|---|---|
| POST | /auth/register | Público. Cria CIDADAO. |
| POST | /auth/login | Público. Retorna token e perfil. |
| GET | /solicitacoes | Cidadão vê seus pedidos. Equipe vê todos. |
| GET | /solicitacoes/{id} | Titular ou equipe. |
| POST | /solicitacoes | Cidadão. Cria em ABERTA. |
| PUT, DELETE | /solicitacoes/{id} | Titular, enquanto ABERTA. |
| PATCH | /solicitacoes/{id}/status | Equipe. Recebe um objeto com `status`. |
| GET | /pontos-agua e /pontos-agua/{id} | Usuários autenticados. |
| POST | /pontos-agua | Equipe. |
| PUT, DELETE | /pontos-agua/{id} | Equipe. |
| GET | /regioes | Usuários autenticados. |


Exemplo de cadastro

~~~json
{
  "nome": "Maria Souza",
  "email": "maria@example.com",
  "senha": "MinhaSenhaLocal",
  "cidade": "Canoas"
}
~~~

Exemplo de solicitação para POST e PUT

~~~json
{
  "tipo": "FALTA_DE_AGUA",
  "descricao": "Família sem abastecimento desde ontem",
  "quantidadePessoas": 5,
  "urgencia": "ALTA",
  "grupoVulneravel": true,
  "endereco": "Endereço fictício",
  "cidade": "Canoas",
  "latitude": -29.91,
  "longitude": -51.18
}
~~~

A resposta acrescenta `id`, `status`, `prioridade`, `analiseIa`, `solicitante`, `criadaEm` e `atualizadaEm`. A equipe usa `{ "status": "EM_ATENDIMENTO" }` para iniciar o atendimento. Um pedido aberto pode entrar em atendimento ou ser cancelado. Um pedido em atendimento pode ser atendido ou cancelado. `DELETE` devolve 204 e retira o pedido das consultas, preservando o registro como cancelado para auditoria.

## Persistência, RabbitMQ e Spring AI

`usuarios`, `regioes`, `pontos_agua` e `solicitacoes` são definidos pelo Flyway. Solicitações pertencem a um usuário. Pontos e solicitações podem indicar `regiaoId`, sem exigir mudança no app. Quando a cidade informada tem uma única região cadastrada, ela é vinculada automaticamente. Os scripts em `src/main/resources/db/migration` e `db/demo` dão a referência relacional para a entrega posterior de Oracle e MongoDB.

**Arquitetura distribuída, opção A.** A criação persiste o pedido e o evento na mesma transação. O publicador lê eventos pendentes e confirma o roteamento com RabbitMQ antes de marcar a publicação. O consumidor registra a entrada do pedido na fila de triagem com tolerância a redelivery. Após as tentativas configuradas, uma mensagem com falha vai para `nexus-verde.triagem.falhas.v1`.

| Contrato do evento | Valor |
|---|---|
| Exchange | `nexus-verde.solicitacoes` |
| Routing key | `solicitacao.criada.v1` |
| Corpo JSON | `{"id":"<UUID>","solicitacaoId":123,"criadoEm":"2026-11-14T18:00:00Z"}` |

No painel, a equipe pode gerar uma análise com Spring AI. O modelo recebe tipo, urgência, quantidade, indicação de grupo vulnerável e a prioridade calculada pelo sistema. A resposta fundamenta a triagem e aparece em `analiseIa`. O atendimento permanece sob responsabilidade humana. A análise é opcional e a operação manual funciona com o provedor desligado.


## Uso de IA no desenvolvimento

ChatGPT/Codex foi usado para planejamento, implementação, revisão, testes e documentação. A funcionalidade da aplicação é implementada com Spring AI no fluxo de triagem.
