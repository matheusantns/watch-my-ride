# STATE

## Decisions

### AD-001
- **Decision**: O produto sai em três features, nesta ordem: API, bot do Telegram, frontend. As três vivem num único processo Java. A API é a dona dos registros, da validação e dos cálculos. O bot chama o serviço em processo. O frontend chama a API HTTP.
- **Reason**: O planejamento foi pedido separado, com a API primeiro. A conversa de origem já tinha fechado um Spring Boot só, para o bot e a tela não duplicarem regra.
- **Trade-off**: A primeira entrega não fala com o Telegram. Separar o bot em outro processo depois exige um cliente HTTP e auth de serviço que esta decisão não cria.
- **Scope**: `.specs/features/api`, `.specs/features/telegram-bot`, `.specs/features/frontend`
- **Date**: 2026-11-07
- **Status**: active

### AD-002
- **Decision**: Esta entrega se comporta como um carro só. O contrato HTTP não tem veículo. Uma feature futura acrescenta vários carros e marca um como preferido.
- **Reason**: O usuário pediu um carro por enquanto e, depois, mais de um com um preferido.
- **Trade-off**: Abastecimento não carrega `vehicle_id` nesta API. A feature futura migra os registros para o carro preferido.
- **Scope**: `.specs/features/api`, `.specs/features/telegram-bot`, `.specs/features/frontend`
- **Date**: 2026-11-07
- **Status**: active

### AD-003
- **Decision**: A regra de abastecimento mora em Java puro, sem anotação Spring. O service Spring é a porta que o bot chama. JPA e o filtro HTTP ficam atrás dessa porta.
- **Reason**: 2026-11-07, usuário: "Vamos de 1." A conta precisa de teste sem subir o Spring, e o bot chama o service no mesmo processo.
- **Trade-off**: Mais classes do que colocar a conta dentro do `@Service`. O SQL do abastecimento sai do Hibernate, não de JDBC escrito à mão.
- **Scope**: processo Java, `.specs/features/api`, `.specs/features/telegram-bot`
- **Date**: 2026-11-07
- **Status**: active

### AD-004
- **Decision**: Código, contrato HTTP e banco usam inglês. Isso cobre pacotes, classes, métodos, campos, rotas, chaves JSON, códigos de erro, tabelas e colunas. A prosa das specs continua em português. Texto que a pessoa lê ou digita, como os comandos do Telegram, fica para a spec da feature que o define.
- **Reason**: 2026-11-08, usuário: "Ajuste o projeto e specs para usar termos em ingles na escrita do codigo." O contrato ainda é rascunho e não há código gerado.
- **Trade-off**: O domínio brasileiro (abastecimento, odômetro, posto) deixa de aparecer no identificador. Quem lê a spec precisa do glossário em `.specs/project.md` para achar o nome no código.
- **Scope**: processo Java, `.specs/features/api`, `.specs/features/telegram-bot`, `.specs/features/frontend`
- **Date**: 2026-11-08
- **Status**: active

## Handoff
