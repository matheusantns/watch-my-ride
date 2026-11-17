# Watch My Ride

Watch My Ride is a personal fuel log for one car. You record each fill-up: liters, what you paid, the odometer, and whether the tank was full. The system stores that fact, checks it, and does the math a notebook gets wrong.

Fuel economy is published only between two full tanks. Liters from partial fill-ups in the middle are added to that stretch. A partial on its own has no km/l. The liters of a full tank close the previous stretch; they do not start the next average. Monthly spending is the sum of the totals whose date falls in that month.

If you remember only the pump total, or only the price per liter, the API fills in the missing number. If both are sent and they disagree by more than R$ 0.01, the request is rejected and nothing is stored. An odometer lower than or equal to the last one for this car is rejected, so a stretch never uses negative or zero kilometers.

## What ships, and in which order

One Java process. Three features, each starting only after the previous one is done.

1. **API.** Structured JSON in. A stored fill-up, or a calculated number, out. No chat and no screen. This is the slice under construction.
2. **Telegram bot.** You describe a fill-up in plain text. [Maritaca](https://www.maritaca.ai/) extracts the fields. The bot shows what it understood and writes only after you confirm. Query commands do not call the model. The bot calls the same service as the API, inside the process, not HTTP against itself.
3. **Frontend.** A panel that lists fill-ups, consumption over time, and spending by month. It reads and writes through the HTTP API. Its UI stack is not chosen yet.

The API owns the records, the validation, and the calculations. The bot and the frontend do not write to the database.

## Stack

| Piece | Choice |
| --- | --- |
| Server | Java 21, Spring Boot 4.1.2, Maven |
| HTTP | Spring Web MVC |
| Persistence | PostgreSQL, Spring Data JPA (Hibernate), Flyway (`ddl-auto=validate`) |
| Auth | Spring Security. `Authorization: Bearer` with the owner's token |
| Tests | JUnit. Domain rules run without Spring. Concurrency tests use Testcontainers with Postgres |
| Bot (later) | [TelegramBots](https://github.com/rubenlagus/TelegramBots), long polling, same process |
| Extraction (later) | Maritaca model `S5-free`, text only. Used only to pull fields out of a sentence |
| Secrets | Telegram token, Maritaca key, and the owner token live in environment variables |

Money, odometer order, and consumption are plain Java with no Spring annotations. Spring sits on the edge: the HTTP controller, the transaction, and JPA. That split is the same idea as keeping calculation functions free of Express and calling them from a route handler. The repository stores numbers the service already closed. It does not decide whether a fill-up is valid.

## Out of scope for now

Maintenance, kilometer-based reminders, receipt or dashboard photos, more than one car, and accounts beyond the single owner.

## Status

The API contract is approved. The Maven project is in place. Domain rules, persistence, and HTTP endpoints are still being built. Specs live in `.specs/`.
