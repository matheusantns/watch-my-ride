# API Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, the walk, check_proofs, the Verifier and the screens sub-agent).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/api/design.md`
**Status**: Approved
**Approved**: 2026-11-08 - user: "Aprovado. Pode fazer T1"
**Build**: `./mvnw -q -DskipTests package`
**Run test**: `./mvnw -q test`

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `.specs/project.md` (JUnit, Maven), `.specs/features/api/design.md` (pure Java for the rules, HTTP at the edge, Testcontainers for Postgres). No test sources and no build file exist yet. The commands below are the Maven commands T1 adds.

| Code Layer                    | Required Test Type | Coverage Expectation                                                                | Location Pattern                                                                                              | Run Command                                                    |
| ----------------------------- | ------------------ | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| Money, Odometer, Consumption  | unit               | All branches of those classes, including the money and odometer limits they own     | `src/test/java/watchmyride/refuel/MoneyTest.java`, `OdometerTest.java`, `ConsumptionTest.java`                | `./mvnw -q -Dtest=MoneyTest,OdometerTest,ConsumptionTest test` |
| RefuelService                 | integration        | Create, list, get, update, delete, consumption, spending, clock, and the write lock | `src/test/java/watchmyride/refuel/RefuelServiceTest.java`                                                     | `./mvnw -q -Dtest=RefuelServiceTest test`                      |
| RefuelRepository              | integration        | Highest odometer, descending order, unique `odometer_km`                            | `src/test/java/watchmyride/persistence/RefuelRepositoryTest.java`                                             | `./mvnw -q -Dtest=RefuelRepositoryTest test`                   |
| HTTP API                      | integration        | Every route in the design: happy path, listed edge, and error path                  | `src/test/java/watchmyride/api/RefuelApiTest.java`, `ApiExceptionHandlerTest.java`, `SecurityConfigTest.java` | `./mvnw -q test`                                               |
| Maven project, entity, Flyway | none               | Build gate only                                                                     | `pom.xml`, `RefuelEntity.java`, `V1__refuel.sql`                                                              | `./mvnw -q -DskipTests package`                                |

A passing test prints one stdout line: `PASSED` plus its JUnit display name. T1's listener does that. Each check below owns a display name that starts with its id (`C1`, `C2`, ...), so one line cannot satisfy another check.

Postgres tests need Docker. Testcontainers starts the database. No H2.

## Gate Check Commands

> Generated from the Maven build T1 creates - confirm before Execute. No linter is configured. The compiler runs inside the package command.

| Gate Level | When to Use                                                                         | Command                                                        |
| ---------- | ----------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| Quick      | After a task whose tests are only `MoneyTest`, `OdometerTest`, or `ConsumptionTest` | `./mvnw -q -Dtest=MoneyTest,OdometerTest,ConsumptionTest test` |
| Full       | After a task that starts Spring or Postgres                                         | `./mvnw -q test`                                               |
| Build      | After the scaffold, the entity, or a phase                                          | `./mvnw -q -DskipTests package`                                |

---

## Execution Plan

Tasks run in id order. An arrow is a dependency, not a second schedule.

### Phase 1: Rules

Pure Java. No Spring.

```
T1 → T2 → T3
T2 → T4
```

### Phase 2: Persistence and service

```
T5 → T6 → T7 → T8 → T9 → T10 → T11
```

### Phase 3: HTTP

```
T12 → T14
T13 → T14
T14 → T15 → T16 → T17 → T18 → T19
```

---

## Task Breakdown

### Phase 1: Rules

### T1: Scaffold the Maven build

**What**: Create the Spring Boot 4.1.2 project that later tasks compile against.
**Where**: `pom.xml`
**Depends on**: None
**Reuses**: none
**Requirement**: FUEL-01

**Done when**:

- [x] Java 21. Parent `spring-boot-starter-parent` 4.1.2. Starters: `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, Flyway, and the test starter. Testcontainers Postgres is a test dependency.
- [x] `WatchMyRideApplication` starts. `spring.jpa.hibernate.ddl-auto=validate`. Flyway owns the schema. Jackson uses snake_case. `Clock` is a bean in `America/Sao_Paulo`. The owner token is read from the environment variable `OWNER_TOKEN`.
- [x] `PassedTestLogger` prints `PASSED <display name>` to stdout for each passing test. Surefire leaves that line on the console under `./mvnw -q test`.
- [x] The Maven wrapper is `./mvnw`. Gate passes: `./mvnw -q -DskipTests package`.

**Tests**: none
**Gate**: build
**Commit**: `chore(api): scaffold the Spring Boot build`

---

### T2: Complete the money fields

**What**: `Money.complete` fills the missing money field and rejects a pair outside the spec.
**Where**: `src/main/java/watchmyride/refuel/Money.java`
**Depends on**: T1
**Reuses**: none
**Requirement**: FUEL-01, FUEL-02, FUEL-03, FUEL-04, FUEL-05

**Done when**:

- [x] `RuleViolationException` carries `code()`. `complete(liters, pricePerLiter, totalAmount)` returns `Amounts` at scales 3, 3, and 2, `HALF_UP`.
- [x] 40 liters at 5.899 with no total returns total 235.96. Total 235.96 with no price returns price 5.899.
- [x] Total 235.97 with price 5.899 stays 235.97. Total 235.98 raises `inconsistent_amount`.
- [x] A missing pair raises `required_field`. Out of range raises `invalid_liters`, `invalid_price`, or `invalid_amount`. Zero and negative values use those same three codes.
- [x] 3 liters and total 10.00 return price 3.333.
- [x] `MoneyTest` has these cases. Gate passes: `./mvnw -q -Dtest=MoneyTest test`. The 8 tests pass.

**Tests**: unit
**Gate**: quick
**Commit**: `feat(api): complete refuel money fields`

---

### T3: Keep the odometer strict

**What**: `Odometer` accepts a reading only when it stays strictly ordered.
**Where**: `src/main/java/watchmyride/refuel/Odometer.java`
**Depends on**: T2
**Reuses**: `RuleViolationException`
**Requirement**: FUEL-06, FUEL-07, FUEL-08

**Done when**:

- [x] `acceptsInsert(highestStored, next)` rejects a next reading lower than the stored max with `regressive_odometer`, and an equal reading with `duplicate_odometer`. An empty history accepts an in-range reading.
- [x] `acceptsUpdate(previous, next, following)` requires next to be greater than the previous neighbor when it exists, and less than the following neighbor when it exists. A sole reading of 9000 is accepted.
- [x] A reading below 0 or above 9999999 raises `invalid_odometer`.
- [x] `OdometerTest` covers insert, duplicate, update inside the neighbors, update outside, the sole reading, and the range. Gate passes: `./mvnw -q -Dtest=MoneyTest,OdometerTest test`. No prior test was deleted.

**Tests**: unit
**Gate**: quick
**Commit**: `feat(api): reject an odometer that breaks the sequence`

---

### T4: Calculate consumption segments

**What**: `Consumption.calculate` builds closed segments and the open tail.
**Where**: `src/main/java/watchmyride/refuel/Consumption.java`
**Depends on**: T2
**Reuses**: none
**Requirement**: FUEL-11

**Done when**:

- [ ] A full tank at 10000 km, a partial of 10 liters at 10050 km, and a full tank of 12 liters at 10200 km return one segment: `km` 200, `liters` 22.000, `km_per_liter` 9.091. The liters of the 10000 km fill are not in the segment.
- [ ] Liters after the last full tank return `open` with those liters and a null `km_per_liter`, and they add no closed segment.
- [ ] Only partials, or one full tank with nothing after it, return empty `segments` and null `open`.
- [ ] Dropping the 10200 km fill from that input leaves empty `segments` and `open` of 10 liters.
- [ ] `ConsumptionTest` covers the four inputs. Gate passes: `./mvnw -q -Dtest=MoneyTest,OdometerTest,ConsumptionTest test`.

**Tests**: unit
**Gate**: quick
**Commit**: `feat(api): calculate consumption between full tanks`

---

### Phase 2: Persistence and service

### T5: Add the refuel table

**What**: Flyway creates `refuel` and JPA maps it.
**Where**: `src/main/resources/db/migration/V1__refuel.sql`
**Depends on**: T1
**Reuses**: none
**Requirement**: FUEL-01

**Done when**:

- [ ] Columns match the design: `id`, `liters numeric(6,3)`, `price_per_liter numeric(5,3)`, `total_amount numeric(10,2)`, `date`, `odometer_km` unique, `full_tank`, `station varchar(80)` nullable.
- [ ] `RefuelEntity` maps those columns and does not calculate money or consumption. There is no vehicle column.
- [ ] Gate passes: `./mvnw -q -DskipTests package`.

**Tests**: none
**Gate**: build
**Commit**: `feat(api): add the refuel table`

---

### T6: Query stored refuels

**What**: The repository returns the highest odometer and the rows in descending order.
**Where**: `src/main/java/watchmyride/persistence/RefuelRepository.java`
**Depends on**: T5
**Reuses**: `RefuelEntity`
**Requirement**: FUEL-09, FUEL-07

**Done when**:

- [ ] `highestOdometer()` is empty on an empty table and returns the greatest `odometer_km` after inserts.
- [ ] `findAllByOrderByOdometerKmDesc` returns 200 km before 100 km.
- [ ] Saving a second row with the same `odometer_km` fails on the unique index.
- [ ] `RefuelRepositoryTest` runs on Testcontainers Postgres. Gate passes: `./mvnw -q -Dtest=RefuelRepositoryTest test`.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): query refuels by odometer`

---

### T7: Create a refuel

**What**: `RefuelService.create` locks, checks the rules, and stores one row.
**Where**: `src/main/java/watchmyride/refuel/RefuelService.java`
**Depends on**: T2, T3, T6
**Reuses**: `Money`, `Odometer`, `RefuelRepository`
**Requirement**: FUEL-01, FUEL-06, FUEL-07

**Done when**:

- [ ] `create` is `@Transactional`, takes `pg_advisory_xact_lock` before reading the max odometer, and stores the `Amounts` from `Money`.
- [ ] A unique-index failure becomes `RuleViolationException("duplicate_odometer")`. A lower reading becomes `regressive_odometer`. Nothing is stored on either failure.
- [ ] A null date stores the civil date of the injected `Clock` in `America/Sao_Paulo`. Clock `2026-11-08T02:00:00Z` stores `2026-11-07`.
- [ ] A missing station is stored as null. A station of 80 characters is stored. 81 characters raise `invalid_station`.
- [ ] Two concurrent creates with odometers 5000 and 4000 leave a strictly increasing sequence. The rejected call carries `regressive_odometer` or `duplicate_odometer`.
- [ ] `RefuelServiceTest` covers these cases on Testcontainers. Gate passes: `./mvnw -q test`.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): create a refuel under the odometer lock`

---

### T8: List and get a refuel

**What**: `list` and `get` return the stored rows without calculating.
**Where**: `src/main/java/watchmyride/refuel/RefuelService.java`
**Depends on**: T7
**Reuses**: `RefuelRepository`
**Requirement**: FUEL-09, FUEL-10

**Done when**:

- [ ] `list` returns rows in descending `odometer_km`.
- [ ] `get` returns liters, price, total, date, odometer, full tank, and station for an existing id.
- [ ] An unknown id raises `RuleViolationException("not_found")`.
- [ ] The new `RefuelServiceTest` cases pass. Gate passes: `./mvnw -q test`. No prior test was deleted.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): list and get a refuel`

---

### T9: Update a refuel

**What**: `update` applies the money rules and the neighbor check.
**Where**: `src/main/java/watchmyride/refuel/RefuelService.java`
**Depends on**: T8
**Reuses**: `Money`, `Odometer`
**Requirement**: FUEL-08, FUEL-10

**Done when**:

- [ ] `update` locks, then accepts a new odometer only inside the neighbors. Rows at 10000, 11000, and 12000 accept 11500 for the middle id.
- [ ] 10000 and 12000 for that same middle id raise `regressive_odometer` and leave the row unchanged.
- [ ] The only row, moved from 10000 to 9000, is stored.
- [ ] Price 5.899 and 40 liters with no total store total 235.96. Total 235.98 with that price raises `inconsistent_amount`.
- [ ] Gate passes: `./mvnw -q test`. No prior test was deleted.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): update a refuel between its neighbors`

---

### T10: Delete a refuel

**What**: `delete` removes the row.
**Where**: `src/main/java/watchmyride/refuel/RefuelService.java`
**Depends on**: T9
**Reuses**: `RefuelRepository`
**Requirement**: FUEL-10

**Done when**:

- [ ] `delete` on an existing id removes the row. A later `get` raises `not_found`.
- [ ] `delete` on an unknown id raises `not_found` and removes nothing.
- [ ] Gate passes: `./mvnw -q test`. No prior test was deleted.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): delete a refuel`

---

### T11: Read consumption and spending

**What**: `consumption` and `spending` read stored rows and call the pure rules.
**Where**: `src/main/java/watchmyride/refuel/RefuelService.java`
**Depends on**: T4, T10
**Reuses**: `Consumption`
**Requirement**: FUEL-11, FUEL-12

**Done when**:

- [ ] The 10000 / 10 L at 10050 / 12 L at 10200 rows return `km` 200, `liters` 22.000, `km_per_liter` 9.091.
- [ ] Liters after the last full tank come back as `open` with a null `km_per_liter`. Only partials, or one full tank alone, return empty segments and null `open`.
- [ ] Deleting the 10200 km row and calling `consumption` again returns empty segments and `open` of 10 liters.
- [ ] `spending("2026-11")` sums `total_amount` for dates in that month and ignores `2026-10-31`. A month that is not `YYYY-MM` raises `invalid_month`.
- [ ] A null month uses the civil month of the clock in `America/Sao_Paulo`. Clock `2026-11-08T15:00:00Z` selects `2026-11`.
- [ ] Gate passes: `./mvnw -q test`. No prior test was deleted.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): read consumption and monthly spending`

---

### Phase 3: HTTP

### T12: Map failures to the error body

**What**: The advice turns a rule failure into `{"code":"..."}` and an unknown failure into 500.
**Where**: `src/main/java/watchmyride/api/ApiExceptionHandler.java`
**Depends on**: T2
**Reuses**: `RuleViolationException`
**Requirement**: FUEL-14

**Done when**:

- [ ] `not_found` responds 404. `unauthorized` is not mapped here. Every other `RuleViolationException` code responds 422. The body is only `code`.
- [ ] An unreadable body responds 422 `required_field`. A `date` that is not `YYYY-MM-DD` responds 422 `invalid_date`.
- [ ] Any other exception responds 500 `{"code":"internal_error"}`. The exception text stays in the log and stays out of the body.
- [ ] `ApiExceptionHandlerTest` display name is `C30 unhandled error returns 500 internal_error and logs boom-marker`. The body does not contain `boom-marker` or `message`. Gate passes: `./mvnw -q test`.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): map refuel failures to the error body`

---

### T13: Require the owner token

**What**: A missing or wrong bearer token stops before the controller.
**Where**: `src/main/java/watchmyride/api/SecurityConfig.java`
**Depends on**: T1
**Reuses**: none
**Requirement**: FUEL-13

**Done when**:

- [ ] `SecurityConfigTest` display name `C26 missing or wrong bearer returns 401 and stores nothing`: no header, and a wrong bearer, both respond 401 `{"code":"unauthorized"}`. A POST with a wrong token leaves the table empty.
- [ ] Display name `C28 no registration route exists`: `POST /owners` and `POST /register` with the owner token respond 404, not 201.
- [ ] The filter does not log the token. Gate passes: `./mvnw -q test`. No prior test was deleted.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): require the owner bearer token`

---

### T14: POST a refuel

**What**: `POST /refuels` returns the stored row or the rule's code.
**Where**: `src/main/java/watchmyride/api/RefuelController.java`
**Depends on**: T7, T12, T13
**Reuses**: `RefuelService`
**Requirement**: FUEL-01, FUEL-02, FUEL-03, FUEL-04, FUEL-05, FUEL-06, FUEL-07, FUEL-13

**Done when**:

- [ ] The request DTO uses `BigDecimal` for liters, price, and total. Paths `/consumption` and `/spending` are declared before `/{id}`.
- [ ] `RefuelApiTest` adds these display names, each asserting the status, the body, and that a failure stores nothing:
  - `C1 price only returns 201 total 235.96 and price 5.899`
  - `C2 total only returns 201 price 5.899 and total 235.96`
  - `C3 total 235.97 within one cent is stored as sent`
  - `C4 missing or out of range fields return 422 and store nothing`
  - `C5 total 235.98 returns inconsistent_amount and stores nothing`
  - `C6 omitted date at 2026-11-08T02:00:00Z stores 2026-11-07`
  - `C7 omitted station is null and an 80 character station is stored`
  - `C8 odometer 9999 after 10000 returns regressive_odometer`
  - `C9 the same odometer returns duplicate_odometer`
  - `C13 two concurrent posts leave a strictly increasing sequence`
  - `C27 a body owner field does not add an owner to the response`
  - `C31 a non boolean full_tank returns required_field`
  - `C32 a date that is not YYYY-MM-DD returns invalid_date`
  - `C33 an 81 character station returns invalid_station`
  - `C35 zero or negative money returns invalid_liters invalid_price or invalid_amount`
- [ ] C4 asserts `required_field`, `invalid_liters`, `invalid_price`, `invalid_amount`, and `invalid_odometer` on separate requests.
- [ ] Gate passes: `./mvnw -q test`. No prior test was deleted.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): post a refuel`

---

### T15: GET one refuel and the list

**What**: `GET /refuels` and `GET /refuels/{id}` return the owner's rows.
**Where**: `src/main/java/watchmyride/api/RefuelController.java`
**Depends on**: T8, T14
**Reuses**: `RefuelService`
**Requirement**: FUEL-09, FUEL-10, FUEL-14

**Done when**:

- [ ] `RefuelApiTest` adds:
  - `C14 list returns 200 in descending odometer_km`
  - `C15 get by id returns 200 with the stored fields`
  - `C16 an unknown id returns 404 not_found`
  - `C19 no response contains vehicle or vehicle_id`
  - `C29 error bodies for 401 404 and 422 contain only code`
- [ ] C15 asserts liters, `price_per_liter`, `total_amount`, `date`, `odometer_km`, `full_tank`, and `station`.
- [ ] C29 uses one 401, one 404, and one 422 response. None of them has a `message` field.
- [ ] Gate passes: `./mvnw -q test`. No prior test was deleted.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): get a refuel and the list`

---

### T16: PUT a refuel

**What**: `PUT /refuels/{id}` applies the same money and neighbor rules as create.
**Where**: `src/main/java/watchmyride/api/RefuelController.java`
**Depends on**: T9, T15
**Reuses**: `RefuelService`
**Requirement**: FUEL-08, FUEL-10

**Done when**:

- [ ] `RefuelApiTest` adds:
  - `C10 put odometer 11500 between 10000 and 12000 returns 200`
  - `C11 put odometer 12000 on the middle row returns regressive_odometer`
  - `C12 put the only row to 9000 returns 200`
  - `C17 put price 5.899 and 40 liters returns 200 total 235.96`
- [ ] C11 leaves the stored odometer unchanged.
- [ ] Gate passes: `./mvnw -q test`. No prior test was deleted.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): put a refuel`

---

### T17: DELETE a refuel

**What**: `DELETE /refuels/{id}` removes the row and returns 204.
**Where**: `src/main/java/watchmyride/api/RefuelController.java`
**Depends on**: T10, T16
**Reuses**: `RefuelService`
**Requirement**: FUEL-10

**Done when**:

- [ ] Display name `C18 delete an existing id returns 204 and the next get is 404`.
- [ ] Delete of an unknown id returns 404 `not_found`.
- [ ] Gate passes: `./mvnw -q test`. No prior test was deleted.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): delete a refuel over http`

---

### T18: GET consumption

**What**: `GET /refuels/consumption` returns the segments from the stored rows.
**Where**: `src/main/java/watchmyride/api/RefuelController.java`
**Depends on**: T11, T17
**Reuses**: `RefuelService`
**Requirement**: FUEL-11

**Done when**:

- [ ] `RefuelApiTest` adds:
  - `C20 three fills return km 200 liters 22.000 and km_per_liter 9.091`
  - `C21 liters after the last full tank return open with null km_per_liter`
  - `C22 only partials or one full tank return empty segments and null open`
  - `C34 deleting the 10200 fill returns empty segments and open of 10 liters`
- [ ] C20 asserts the 10000 km fill's liters are absent from the segment.
- [ ] Gate passes: `./mvnw -q test`. No prior test was deleted.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): get consumption segments`

---

### T19: GET spending

**What**: `GET /refuels/spending` sums the month.
**Where**: `src/main/java/watchmyride/api/RefuelController.java`
**Depends on**: T11, T18
**Reuses**: `RefuelService`
**Requirement**: FUEL-12

**Done when**:

- [ ] `RefuelApiTest` adds:
  - `C23 spending month 2026-11 sums 10.00 and ignores 2026-10-31`
  - `C24 spending month 2026-13 returns invalid_month`
  - `C25 omitted month at 2026-11-08T15:00:00Z returns month 2026-11`
- [ ] The response body is `month` and `total_amount`.
- [ ] Gate passes: `./mvnw -q test`. The full suite still contains every `C1` through `C35` display name.

**Tests**: integration
**Gate**: full
**Commit**: `feat(api): get monthly spending`

---

## Checks

**C1** - `POST /refuels` with `liters` 40.000 and `price_per_liter` 5.899 and no `total_amount` returns 201 and stores `total_amount` 235.96 and `price_per_liter` 5.899 (FUEL-01, AC 1)
Proof: run test | PASSED C1 price only returns 201 total 235.96 and price 5.899

**C2** - `POST /refuels` with `liters` 40.000 and `total_amount` 235.96 and no `price_per_liter` returns 201 and stores `price_per_liter` 5.899 and `total_amount` 235.96 (FUEL-02, AC 2)
Proof: run test | PASSED C2 total only returns 201 price 5.899 and total 235.96

**C3** - `POST /refuels` with price 5.899 and total 235.97 returns 201 and stores 235.97 (FUEL-03, AC 3)
Proof: run test | PASSED C3 total 235.97 within one cent is stored as sent

**C4** - A POST missing liters, odometer, or full tank, missing both money fields, or sending a number outside the limit returns 422 `required_field`, `invalid_liters`, `invalid_price`, `invalid_amount`, or `invalid_odometer` and stores nothing (FUEL-04, AC 4)
Proof: run test | PASSED C4 missing or out of range fields return 422 and store nothing

**C5** - Price 5.899 and total 235.98 return 422 `inconsistent_amount` and store neither value in place of the other (FUEL-05, AC 5)
Proof: run test | PASSED C5 total 235.98 returns inconsistent_amount and stores nothing

**C6** - An omitted date at clock `2026-11-08T02:00:00Z` stores civil date `2026-11-07` (FUEL-01, AC 6)
Proof: run test | PASSED C6 omitted date at 2026-11-08T02:00:00Z stores 2026-11-07

**C7** - An omitted station is stored as null, and a station of 80 characters is stored (FUEL-01, AC 7)
Proof: run test | PASSED C7 omitted station is null and an 80 character station is stored

**C8** - `odometer_km` 9999 after a stored 10000 returns 422 `regressive_odometer` and stores nothing (FUEL-06, AC 1)
Proof: run test | PASSED C8 odometer 9999 after 10000 returns regressive_odometer

**C9** - A POST with an odometer already stored returns 422 `duplicate_odometer` and stores nothing (FUEL-07, AC 2)
Proof: run test | PASSED C9 the same odometer returns duplicate_odometer

**C10** - `PUT /refuels/{id}` of the middle row from 11000 to 11500, between 10000 and 12000, returns 200 (FUEL-08, AC 3)
Proof: run test | PASSED C10 put odometer 11500 between 10000 and 12000 returns 200

**C11** - `PUT` of that middle row to 12000 returns 422 `regressive_odometer` and leaves the row unchanged (FUEL-08, AC 4)
Proof: run test | PASSED C11 put odometer 12000 on the middle row returns regressive_odometer

**C12** - `PUT` of the only row to 9000 returns 200 (FUEL-08, AC 5)
Proof: run test | PASSED C12 put the only row to 9000 returns 200

**C13** - Two concurrent POSTs with odometers 5000 and 4000 leave a strictly increasing stored sequence, and the rejected call returns 422 (FUEL-06, AC 6)
Proof: run test | PASSED C13 two concurrent posts leave a strictly increasing sequence

**C14** - `GET /refuels` returns 200 with the owner's rows in descending `odometer_km` (FUEL-09, AC 1)
Proof: run test | PASSED C14 list returns 200 in descending odometer_km

**C15** - `GET /refuels/{id}` returns 200 with liters, `price_per_liter`, `total_amount`, `date`, `odometer_km`, `full_tank`, and `station` (FUEL-09, AC 2)
Proof: run test | PASSED C15 get by id returns 200 with the stored fields

**C16** - An unknown id returns 404 `not_found` (FUEL-10, AC 3)
Proof: run test | PASSED C16 an unknown id returns 404 not_found

**C17** - `PUT /refuels/{id}` with 40 liters and `price_per_liter` 5.899 and no total returns 200 and stores `total_amount` 235.96 (FUEL-10, AC 4)
Proof: run test | PASSED C17 put price 5.899 and 40 liters returns 200 total 235.96

**C18** - `DELETE /refuels/{id}` of an existing row returns 204, and the next GET of that id returns 404 (FUEL-10, AC 5)
Proof: run test | PASSED C18 delete an existing id returns 204 and the next get is 404

**C19** - No response body contains `vehicle` or `vehicle_id` (FUEL-09, AC 6)
Proof: run test | PASSED C19 no response contains vehicle or vehicle_id

**C20** - A full tank at 10000 km, 10 liters at 10050 km, and 12 liters full at 10200 km return `km` 200, `liters` 22.000, and `km_per_liter` 9.091, without the liters from the 10000 km fill (FUEL-11, AC 1)
Proof: run test | PASSED C20 three fills return km 200 liters 22.000 and km_per_liter 9.091

**C21** - Liters after the last full tank are returned in `open` with a null `km_per_liter`, and that tail has no average (FUEL-11, AC 2)
Proof: run test | PASSED C21 liters after the last full tank return open with null km_per_liter

**C22** - Only partials, or one full tank with nothing after it, return empty `segments` and null `open` (FUEL-11, AC 3)
Proof: run test | PASSED C22 only partials or one full tank return empty segments and null open

**C23** - `GET /refuels/spending?month=2026-11` returns 200 and the sum of `total_amount` for dates in that month, ignoring `2026-10-31` (FUEL-12, AC 4)
Proof: run test | PASSED C23 spending month 2026-11 sums 10.00 and ignores 2026-10-31

**C24** - A `month` that is not `YYYY-MM` returns 422 `invalid_month` (FUEL-12, AC 5)
Proof: run test | PASSED C24 spending month 2026-13 returns invalid_month

**C25** - An omitted `month` at clock `2026-11-08T15:00:00Z` uses `2026-11` (FUEL-12, AC 6)
Proof: run test | PASSED C25 omitted month at 2026-11-08T15:00:00Z returns month 2026-11

**C26** - A request with no bearer, or with a bearer other than `OWNER_TOKEN`, returns 401 `unauthorized` and stores nothing (FUEL-13, AC 1)
Proof: run test | PASSED C26 missing or wrong bearer returns 401 and stores nothing

**C27** - A created refuel is the configured owner's, and an `owner` field in the body does not appear in the response (FUEL-13, AC 2)
Proof: run test | PASSED C27 a body owner field does not add an owner to the response

**C28** - `POST /owners` and `POST /register` with the owner token return 404 and create nothing (FUEL-13, AC 3)
Proof: run test | PASSED C28 no registration route exists

**C29** - A 401, a 404, and a 422 each respond with JSON that has only `code` and no `message` (FUEL-14, AC 1)
Proof: run test | PASSED C29 error bodies for 401 404 and 422 contain only code

**C30** - An unhandled exception returns 500 `{"code":"internal_error"}`, and the text `boom-marker` stays in the log and out of the body (FUEL-14, AC 2)
Proof: run test | PASSED C30 unhandled error returns 500 internal_error and logs boom-marker

**C31** - A `full_tank` that is not a boolean returns 422 `required_field` and stores nothing (FUEL-04, edge)
Proof: run test | PASSED C31 a non boolean full_tank returns required_field

**C32** - A `date` that is not `YYYY-MM-DD` returns 422 `invalid_date` and stores nothing (FUEL-04, edge)
Proof: run test | PASSED C32 a date that is not YYYY-MM-DD returns invalid_date

**C33** - A station of 81 characters returns 422 `invalid_station` and stores nothing (FUEL-01, edge)
Proof: run test | PASSED C33 an 81 character station returns invalid_station

**C34** - Deleting the full tank at 10200 km and calling `GET /refuels/consumption` returns empty `segments` and `open` of 10 liters (FUEL-11, edge)
Proof: run test | PASSED C34 deleting the 10200 fill returns empty segments and open of 10 liters

**C35** - Zero or negative liters, price, or total return 422 `invalid_liters`, `invalid_price`, or `invalid_amount` and store nothing (FUEL-04, edge)
Proof: run test | PASSED C35 zero or negative money returns invalid_liters invalid_price or invalid_amount

---

## Handoff

**Settled mid-build:** T1 done locally, no commit, per user. Testcontainers 2 artifacts are `testcontainers-junit-jupiter` and `testcontainers-postgresql`. `junit-platform-launcher` is an explicit test dependency so `PassedTestLogger` compiles. `OWNER_TOKEN` may be empty until the security task.
**Dispatched:** none
**Walked:** none

---

## Phase Execution Map

Tasks run one at a time, in id order. Arrows repeat the dependencies above.

```
Phase 1: T1 → T2 → T3
Phase 1: T2 → T4
Phase 2: T5 → T6 → T7 → T8 → T9 → T10 → T11
Phase 3: T12 → T14
Phase 3: T13 → T14
Phase 3: T14 → T15 → T16 → T17 → T18 → T19
```
