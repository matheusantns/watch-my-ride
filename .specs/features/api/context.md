# API Context

**Gathered:** 2026-11-07
**Spec:** `.specs/features/api/spec.md`
**Status:** Ready for design

---

## Feature Boundary

A API grava, lista, corrige e apaga abastecimento, completa o campo de dinheiro que faltou e publica consumo e gasto. Sem Telegram e sem tela.

---

## Implementation Decisions

### Camadas

- O pedido HTTP chega num controller, passa a um service com as regras de negócio e depois a um repositório que persiste o acontecimento.
- Usuário: "Vai chegar num controller (camada HTTP), vai ser servido a um service com as regras de negócio e depois para um repositorio que vai lidar com a persistencia desse acontecimento."

### Dinheiro

- Preço, litros, total e km/l usam `BigDecimal`.
- Usuário: "Vamos de BigDecimal."

### Repositório

- O repositório não calcula. Recebe os valores que o service já fechou.
- Usuário: "Não deveria calcular. Repositorio não deve se preocupar com regras de negoico."

### Corpo de erro

- Erro tratado: JSON só com `code`, sem `message`.
- Falha não tratada: 500 com `code` `internal_error`. O texto da exceção fica no log.
- Usuário, sobre o 500: "Vai ser genérico pq n é um erro tratado."
- Usuário, sobre o corpo recomendado: "Beleza."

### Forma do miolo

- Regra em Java puro. Spring na borda. Persistência com JPA.
- Usuário: "Vamos de 1."

### Agent's Discretion

- A exceção de domínio e o mapeamento HTTP ficam na borda. O service não devolve status.
- O filtro do Bearer token corta o pedido antes do controller. A allowlist do Telegram fica na feature do bot.
- A sequência de odômetro usa transação, `pg_advisory_xact_lock` e índice único. Não há linha de dono. O service traduz a falha desse índice no código `duplicate_odometer`.

### Declined / Undiscussed Gray Areas → Assumptions

Nenhuma. As áreas de produto que estavam abertas fecharam nesta conversa.

---

## Specific References

No specific requirements - open to standard approaches

---

## Deferred Ideas

None - discussion stayed within feature scope
