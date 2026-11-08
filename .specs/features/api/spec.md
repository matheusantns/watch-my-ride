# API de abastecimento

**Approved**: 2026-11-08 - user: "Aprovado. Pode fazer T1"

## Problem Statement

O dono do carro anota abastecimento num caderno. A conta de km/l erra quando o tanque não fecha, e o preço some quando ele só lembra o total da bomba. A API guarda o abastecimento, completa o campo de dinheiro que faltou e só publica média entre dois tanques cheios.

## Goals

- [ ] Um cliente autenticado grava, lista, corrige e apaga abastecimento sem Telegram e sem tela.
- [ ] Preço por litro e valor total ficam os dois gravados. O cliente informa um. A API calcula o outro.
- [ ] A média em km/l sai de um cheio até o próximo cheio, somando os parciais do meio.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Manutenção | Feature posterior. Esta API não tem tipo nem rota de manutenção. |
| Vários carros e carro preferido | AD-002. O contrato não tem veículo. |
| Bot do Telegram e Maritaca | Feature `telegram-bot`. Esta API recebe JSON. |
| Frontend | Feature `frontend`. |
| Login pelo Telegram | Feature do bot. Aqui o acesso é o Bearer token do dono. |
| Paginação, foto, lembrete | Um carro, um dono. Sem arquivo e sem aviso. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Os dois valores monetários vêm na mesma requisição e discordam | 422 `inconsistent_amount` quando a diferença para `round(liters × price_per_liter, 2)` passa de R$ 0,01. A API não escolhe o vencedor. | 2026-11-07, usuário: "Ok. O usuario precisará escolher qual dos dois é o correto." Quem chama pergunta e reenvia só o campo escolhido. | y |
| Data omitida | Data civil de hoje em `America/Sao_Paulo` | O bot é que resolve "ontem". A API recebe data civil. | y |
| Odômetro igual a um já gravado | 422 `duplicate_odometer` | A sequência precisa ser estrita. Km igual não ordena o trecho. | y |
| Arredondamento | `HALF_UP`. Total em 2 casas. Preço e litros em 3. km/l em 3. | Preço de bomba no Brasil usa 3 casas. | y |
| Posto | Opcional. Até 80 caracteres. | Não entra na conta. | y |
| Limites | Litros de 0,001 a 200. Preço de 0,001 a 99,999. Total de 0,01 a 20000,00. Odômetro de 0 a 9999999. | Acima disso o lançamento está errado. | y |
| Apagar | Remove a linha. Não arquiva. | O usuário pediu apagar. | y |
| Concorrência | A sequência de odômetro que permanece gravada é estrita. O segundo commit que a quebraria recebe 422. | Dois POST paralelos não podem furar a ordem. | y |
| Idempotência | POST não é idempotente. O segundo envio com o mesmo odômetro falha. | Não há chave de deduplicação além do odômetro. | y |
| Observabilidade | Sem métrica nem tracing nesta feature. | Nenhum critério depende de telemetria. | y |
| Dependência externa | Nenhuma chamada de saída. | Maritaca fica no bot. | y |
| Vários donos | Um token, um dono. Não há rota de cadastro. | Allowlist e login do Telegram são da feature do bot. | y |
| Corpo de erro | JSON só com `code`. Sem `message`. O 500 usa `internal_error`. O texto da exceção fica no log. | 2026-11-07, usuário: "Beleza." O bot e a tela escrevem a frase a partir do código. | y |

**Open questions:** none.

---

## User Stories

### P1: Gravar abastecimento ⭐ MVP

**User Story**: Como dono, quero registrar um abastecimento informando o total ou o preço por litro para os dois valores ficarem guardados.

**Why P1**: Sem o registro não há consumo nem gasto.

**Acceptance Criteria**:

1. WHEN o dono envia `POST /refuels` com `liters` 40.000, `price_per_liter` 5.899 e sem `total_amount` THEN the system SHALL responder 201 e gravar `total_amount` 235.96 e `price_per_liter` 5.899.
2. WHEN o dono envia `POST /refuels` com `liters` 40.000, `total_amount` 235.96 e sem `price_per_liter` THEN the system SHALL responder 201 e gravar `price_per_liter` 5.899 e `total_amount` 235.96.
3. WHEN o dono envia os dois e `total_amount` está a no máximo R$ 0,01 de `round(liters × price_per_liter, 2)` THEN the system SHALL responder 201 e gravar os dois valores informados.
4. IF faltam litros, odômetro ou tanque cheio, ou faltam os dois campos de dinheiro, ou um número está fora do limite THEN the system SHALL responder 422 `required_field`, `invalid_liters`, `invalid_price`, `invalid_amount` ou `invalid_odometer`, sem gravar.
5. IF `POST` ou `PUT` trazem preço e total e a diferença passa de R$ 0,01 THEN the system SHALL responder 422 `inconsistent_amount`, sem gravar e sem substituir um campo pelo outro.
6. WHEN a data vem omitida THEN the system SHALL gravar a data civil de hoje em `America/Sao_Paulo`.
7. The system SHALL gravar `station` só quando o cliente enviar, com no máximo 80 caracteres.

**Independent Test**: POST com só o preço e POST com só o total devolvem os dois campos, com 235,96 e 5,899 no exemplo de 40 L.

---

### P1: Manter o odômetro em ordem ⭐ MVP

**User Story**: Como dono, quero que um odômetro menor que o anterior seja recusado para a média não usar km negativo.

**Why P1**: O usuário fechou esta regra.

**Acceptance Criteria**:

1. IF o `odometer_km` do POST é menor que o maior odômetro já gravado THEN the system SHALL responder 422 `regressive_odometer`, sem gravar.
2. IF o `odometer_km` do POST é igual a um odômetro já gravado THEN the system SHALL responder 422 `duplicate_odometer`, sem gravar.
3. WHEN um `PUT /refuels/{id}` altera o odômetro THEN the system SHALL aceitar o valor só se ele for estritamente maior que o vizinho anterior, quando houver, e estritamente menor que o vizinho seguinte, quando houver.
4. IF o novo odômetro do PUT sai desse intervalo THEN the system SHALL responder 422 `regressive_odometer`, sem gravar.
5. WHEN o registro é o único e o PUT muda o odômetro para 9000 THEN the system SHALL responder 200.
6. The system SHALL manter a sequência gravada estritamente crescente mesmo com dois POST concorrentes.

**Independent Test**: Com um registro em 10.000 km, POST em 9.999 e POST em 10.000 respondem 422. PUT do único registro para 9.000 responde 200.

---

### P1: Listar, corrigir e apagar ⭐ MVP

**User Story**: Como dono, quero ver, corrigir e apagar um abastecimento para consertar um lançamento errado.

**Why P1**: O usuário pediu correção e exclusão nesta API.

**Acceptance Criteria**:

1. WHEN o dono envia `GET /refuels` THEN the system SHALL responder 200 com os abastecimentos do dono em ordem decrescente de `odometer_km`.
2. WHEN o dono envia `GET /refuels/{id}` de um registro seu THEN the system SHALL responder 200 com litros, preço por litro, valor total, data, odômetro, tanque cheio e posto.
3. IF o id não existe THEN the system SHALL responder 404 `not_found`.
4. WHEN o dono envia `PUT /refuels/{id}` THEN the system SHALL aplicar as mesmas regras de dinheiro e de limite do POST e responder 200 com os dois campos de dinheiro gravados.
5. WHEN o dono envia `DELETE /refuels/{id}` de um registro seu THEN the system SHALL remover a linha e responder 204.
6. The system SHALL não expor campo de veículo em nenhuma resposta.

**Independent Test**: Criar dois abastecimentos, listar do maior km para o menor, corrigir o preço e apagar um. O id apagado responde 404.

---

### P1: Consumo e gasto ⭐ MVP

**User Story**: Como dono, quero a média entre tanques cheios e o gasto do mês para não tratar um meio tanque como consumo.

**Why P1**: É a conta que a API existe para fazer.

**Acceptance Criteria**:

1. WHEN existem um cheio em 10.000 km, um parcial de 10 litros em 10.050 km e um cheio de 12 litros em 10.200 km THEN the system SHALL devolver um trecho com `km` 200, `liters` 22.000 e `km_per_liter` 9.091, sem incluir os litros do cheio de 10.000 km.
2. WHEN o último cheio ainda não tem outro cheio depois THEN the system SHALL omitir média desse trecho e, se houver litros depois dele, devolver `open` com esses litros e `km_per_liter` nulo.
3. WHILE só existem abastecimentos parciais, ou um único cheio sem litros depois, the system SHALL devolver `segments` vazio e `open` nulo em `GET /refuels/consumption`.
4. WHEN o dono envia `GET /refuels/spending?month=2026-11` THEN the system SHALL responder 200 com a soma de `total_amount` dos abastecimentos cuja `date` cai nesse mês.
5. IF `month` não tem o formato `YYYY-MM` THEN the system SHALL responder 422 `invalid_month`.
6. WHEN `month` vem omitido THEN the system SHALL usar o mês civil de hoje em `America/Sao_Paulo`.

**Independent Test**: Os três lançamentos do critério 1 produzem 9,091 km/l. Um parcial depois do último cheio entra em `open` e não muda o trecho fechado. A soma de novembro ignora outubro.

---

### P1: Acesso do dono ⭐ MVP

**User Story**: Como dono, quero que só o meu token leia e grave abastecimento.

**Why P1**: A API sobe antes do login do Telegram.

**Acceptance Criteria**:

1. IF a requisição não traz `Authorization: Bearer` igual ao token do dono THEN the system SHALL responder 401 `unauthorized` e não alterar dados.
2. The system SHALL gravar todo abastecimento para o único dono configurado.
3. The system SHALL não criar rota de cadastro nem aceitar outro dono no corpo.

**Independent Test**: GET sem cabeçalho e GET com token errado respondem 401. GET com o token do dono responde 200.

---

### P1: Corpo de erro ⭐ MVP

**User Story**: Como cliente da API, quero um JSON de erro estável para reagir ao código sem depender da frase.

**Why P1**: O bot e o frontend decidem o que dizer a partir do código.

**Acceptance Criteria**:

1. WHEN a resposta é 401, 404 ou 422 THEN the system SHALL enviar um JSON com somente o campo `code`, sem campo `message`.
2. IF uma falha não tratada sai do controller THEN the system SHALL responder 500 com `code` `internal_error` e manter o texto da exceção no log do servidor.

**Independent Test**: Preço e total divergentes respondem 422 `{"code":"inconsistent_amount"}`. Uma falha não tratada responde 500 `{"code":"internal_error"}`.

---

## Edge Cases

- IF `full_tank` não é booleano THEN the system SHALL responder 422 `required_field`, sem gravar.
- IF `date` não é uma data civil `YYYY-MM-DD` THEN the system SHALL responder 422 `invalid_date`, sem gravar.
- IF `station` passa de 80 caracteres THEN the system SHALL responder 422 `invalid_station`, sem gravar.
- WHEN um DELETE remove o cheio que fechava um trecho THEN the system SHALL recalcular `GET /refuels/consumption` só com os abastecimentos restantes.
- IF litros, preço ou total são zero ou negativos THEN the system SHALL responder 422 com `invalid_liters`, `invalid_price` ou `invalid_amount`.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| FUEL-01 | P1: Gravar abastecimento | Tasks | T2, T7, T14 |
| FUEL-02 | P1: Gravar abastecimento | Tasks | T2, T14 |
| FUEL-03 | P1: Gravar abastecimento | Tasks | T2, T14 |
| FUEL-04 | P1: Gravar abastecimento | Tasks | T2, T14 |
| FUEL-05 | P1: Gravar abastecimento | Tasks | T2, T14 |
| FUEL-06 | P1: Manter o odômetro em ordem | Tasks | T3, T7, T14 |
| FUEL-07 | P1: Manter o odômetro em ordem | Tasks | T3, T7, T14 |
| FUEL-08 | P1: Manter o odômetro em ordem | Tasks | T3, T9, T16 |
| FUEL-09 | P1: Listar, corrigir e apagar | Tasks | T8, T15 |
| FUEL-10 | P1: Listar, corrigir e apagar | Tasks | T9, T10, T15, T16, T17 |
| FUEL-11 | P1: Consumo e gasto | Tasks | T4, T11, T18 |
| FUEL-12 | P1: Consumo e gasto | Tasks | T11, T19 |
| FUEL-13 | P1: Acesso do dono | Tasks | T13, T14 |
| FUEL-14 | P1: Corpo de erro | Tasks | T12, T15 |

**Coverage:** 14 requisitos, mapeados em `.specs/features/api/tasks.md`.

---

## Sources

- `.specs/project.md`, respostas de 2026-11-07: um carro, Bearer token, corrigir e apagar, litros com total ou preço por litro, odômetro menor recusado.
- Conversa [Criar bots no Telegram com Java](https://claude.ai/share/c4d800cb-5afc-496e-bfa9-36c8d0e1d136): média entre cheios, parciais somados, conta fora da IA.

---

## Success Criteria

- [ ] POST com só o preço ou só o total devolve os dois campos gravados.
- [ ] Odômetro menor ou igual ao último responde 422 e não grava.
- [ ] O trecho 10.000 cheio, 10.050 parcial 10 L, 10.200 cheio 12 L devolve 9,091 km/l.
- [ ] Token ausente ou errado responde 401.
- [ ] Erro tratado responde só `{"code":"..."}`. Falha não tratada responde 500 `internal_error`.
