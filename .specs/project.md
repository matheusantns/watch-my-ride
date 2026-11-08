# Watch My Ride

**Status**: Draft. O contrato da API está em `.specs/features/api/spec.md`, à espera de aprovação.

Registro de abastecimentos de um carro. A pessoa descreve o que aconteceu. O sistema guarda o fato, valida e calcula. Três features, nesta ordem: API, bot do Telegram, frontend. Manutenção fica para uma feature posterior.

Fonte: conversa [Criar bots no Telegram com Java](https://claude.ai/share/c4d800cb-5afc-496e-bfa9-36c8d0e1d136).

## Sequence

| Order | Feature | Spec | Delivers |
| ----- | ------- | ---- | -------- |
| 1 | API | `.specs/features/api/spec.md` | Abastecimentos, validação, consumo e gasto. Sem Telegram e sem tela. |
| 2 | Bot do Telegram | `.specs/features/telegram-bot/spec.md` | Texto livre de abastecimento, extração via Maritaca, confirmação e comandos. Chama a mesma regra da API. |
| 3 | Frontend | `.specs/features/frontend/spec.md` | Painel de abastecimentos que lê e grava pela API. |

A feature 2 não começa antes da spec da API aprovada e implementada. A feature 3 não começa antes do bot.

## Boundaries

### API

Entra JSON já estruturado. Sai o registro gravado ou o número calculado.

Inclui:

- criar, listar, ver, corrigir e apagar um abastecimento
- gravar litros, preço por litro e valor total. O cliente informa o total ou o preço. A API calcula o outro
- rejeitar odômetro menor ou igual ao último abastecimento do mesmo carro
- consumo em km/l de um cheio até o próximo cheio. Litros parciais no meio entram na soma. Um parcial sozinho não tem média.
- gasto por mês
- dono dos dados identificado em toda leitura e escrita

Não inclui: manutenção, Bot API do Telegram, Maritaca, botões, linguagem natural, gráfico, login pelo Telegram.

### Bot do Telegram

Entra texto no chat. A Maritaca devolve JSON. O bot mostra o que entendeu e só grava depois da confirmação. Comandos de consulta não passam pela IA.

Inclui, quando essa feature abrir:

- long polling com [TelegramBots](https://github.com/rubenlagus/TelegramBots)
- extração com Maritaca, modelo `S5-free` (texto apenas, sem ferramentas integradas; oferta gratuita por tempo limitado)
- botões de confirmar e cancelar
- se o preço e o total discordam, o bot pergunta qual dos dois é o correto e só então reenvia o campo escolhido
- `/ultimos`, `/consumo`, `/relatorio`
- só o usuário autorizado dispara a IA

Não inclui: manutenção, tela, lembrete, foto de nota.

### Frontend

Entra depois das duas. Consome a API. Lista registros, mostra consumo ao longo do tempo e gasto por mês.

Não inclui, até a spec dela: manutenção, lembrete, foto, multi-carro, Login Widget do Telegram.

## Decisions

Fechadas pelo pedido desta sessão e pela conversa de origem.

| Decision | Choice |
| -------- | ------ |
| Ordem | API, depois bot, depois frontend. Três specs. |
| Dono dos dados | A API. Bot e frontend não gravam no banco. |
| Linguagem do servidor | Java. O bot usa TelegramBots. |
| IA | Só no bot, só para extrair campos. Conta e validação ficam no código. Modelo `S5-free`. |
| Processo | Uma aplicação. O bot entra no mesmo processo e chama o serviço, não HTTP contra si mesmo. |
| Banco | Postgres. |
| Segredos | Token do Telegram e chave da Maritaca só em variável de ambiente. |
| Escopo inicial | Só abastecimento. Sem tipo `MAINTENANCE` e sem tabela de manutenção. |
| Consumo | Média entre dois tanques cheios. Litros parciais no meio somam no trecho. O parcial não tem km/l próprio. Os litros de um cheio fecham o trecho anterior. |
| Carro | Um carro implícito. Sem campo de veículo. Vários carros, com um preferido, ficam para outra feature. |
| Auth da API | `Authorization: Bearer` com o token do dono. |
| Correção | Criar, alterar e apagar o abastecimento do dono. |
| Dinheiro | Litros, preço por litro e valor total ficam gravados. O cliente manda o total ou o preço. A API calcula o outro. Se os dois vêm e a diferença passa de R$ 0,01, a API responde 422 e não escolhe qual vale. |
| Odômetro | Menor que o último volta erro. Igual também volta erro. |
| Idioma do código | Inglês em identificador, rota, JSON, código de erro, tabela e coluna. A prosa das specs fica em português. AD-004. |

O formato de um processo está em `.specs/STATE.md` como AD-001. Mudar isso depois separa o bot da regra de negócio.

Glossário do código, da rota `refuel`:

| Na spec | No código |
| ------- | --------- |
| abastecimento | `refuel`, rota `/refuels` |
| litros | `liters` |
| preço por litro | `price_per_liter` |
| valor total | `total_amount` |
| odômetro | `odometer_km` |
| tanque cheio | `full_tank` |
| posto | `station` |
| data | `date` |
| código do erro | `code` |
| trecho | `segments[]` com `km_per_liter` |
| trecho aberto | `open` |
| gasto do mês | `/refuels/spending`, campo `month` |
| consumo | `/refuels/consumption` |
| veículo | `vehicle_id`, quando a feature existir |

## Working Defaults

Valem se você não corrigir. Não são critério ainda.

| Topic | Default | Why |
| ----- | ------- | --- |
| Auth da API, fase 2 | `POST /auth/telegram` troca um token de uso único (o bot manda no `/login`, vale 5 minutos) por uma sessão. A sessão carrega o id do Telegram. | O login pelo Telegram só existe na feature 2. |
| Quem pode falar com o bot | Lista `ALLOWED_USER_IDS`. Chat privado. Fora da lista, a mensagem é ignorada. | Cada texto chama a Maritaca. O `S5-free` é grátis só por tempo limitado. |
| Data ausente | A data do registro no servidor, fuso America/Sao_Paulo. | O bot é que resolve "ontem". A API recebe data civil. |
| Stack HTTP | Spring Boot. Build com Maven. | A biblioteca escolhida tem starter Spring. |
| Testes | JUnit no módulo da API. A regra de consumo e a validação de odômetro rodam aí, sem Telegram. | A feature 1 precisa ser demonstrável sozinha. |

## Out of Scope

| Item | Reason |
| ---- | ------ |
| Manutenção | Fora das três features atuais. Vira feature própria quando o abastecimento estiver fechado. |
| Lembrete por km ("troca de óleo") | Depende de manutenção. Fase posterior. |
| Foto de nota ou do painel | Exige modelo com visão. |
| Login Widget do Telegram | Substitui o `/login` só se a spec do frontend pedir. |
| Vários carros e carro preferido | O usuário pediu um carro agora. A escolha do preferido entra numa feature posterior. Ver AD-002. |
| Vários usuários além do dono e da allowlist | O modelo guarda o dono. Convite e cadastro ficam de fora. |
| n8n, Python, grammY | A conversa fechou TelegramBots. |

## Open Questions

Nenhuma. As cinco respostas de 2026-11-07 estão em Decisions e na spec da API.

## Success

- A API grava abastecimento, recusa odômetro regressivo e devolve consumo e gasto sem chamar IA.
- O bot transforma uma frase de abastecimento em registro só depois do "salvar", e os três comandos leem a API.
- O frontend mostra a mesma lista, o mesmo consumo e o mesmo gasto que a API devolve.
