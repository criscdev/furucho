# Bug & Enhancement Log

Issues found during code reviews. Each entry tracks the problem, fix applied, and date.

---

## Legend

- **BUG** — incorrect behavior or latent defect
- **ENH** — code quality / elegance / succinctness improvement
- **INFRA** — build, config, or tooling issue

---

## Entries

### R1 · ENH · 2026-02-23 — handlers.ts: verbose POST body mapping

**File:** `src/mocks/handlers.ts`
**Before:** 9× `String(body['x'] ?? '')` lines manually mapping each field.
**Fix:** Typed `CreateBody = Omit<MockOrderResponse, 'id' | 'createdAt' | 'status'>`, spread `...body`.
**Status:** Fixed

---

### R2 · ENH · 2026-02-23 — handlers.ts: inline status union type

**File:** `src/mocks/handlers.ts`
**Before:** `'PENDING' | 'CONFIRMED' | ... | 'CANCELLED'` defined inline in the interface.
**Fix:** Extracted named `OrderStatus` type for reuse.
**Status:** Fixed

---

### R3 · ENH · 2026-02-23 — handlers.ts: loose module-level mutable state

**File:** `src/mocks/handlers.ts`
**Before:** `let nextId`, `let orders` as module-level variables + separate `resetOrders()` function.
**Fix:** Encapsulated in `createOrderStore()` factory → `db` object with `create/all/findById/updateStatus/reset`.
**Status:** Fixed

---

### R4 · ENH · 2026-02-23 — server.ts: redundant JSDoc example

**File:** `src/mocks/server.ts`
**Before:** 10-line JSDoc showing usage that duplicated `setupTests.ts`.
**Fix:** Trimmed to 1-line comment.
**Status:** Fixed

---

### R5 · BUG · 2026-02-23 — orderFactory.ts: hardcoded past date

**File:** `src/test/factories/orderFactory.ts`
**Before:** `receiveDate: '15/03/2025'` — already in the past. Same class of bug as backend's `@Future` validation flakiness.
**Fix:** Dynamic `futureDate()` helper (today + 90 days).
**Status:** Fixed

---

### R6 · ENH · 2026-02-23 — setupTests.ts: coupled to handler internals

**File:** `src/setupTests.ts`
**Before:** Imported `resetOrders` directly from `handlers.ts`.
**Fix:** Import `db` object instead → `db.reset()`. Single interface to the store.
**Status:** Fixed

---

### R7 · INFRA · 2026-02-23 — vitest.config.ts: mocks not excluded from coverage

**File:** `vitest.config.ts`
**Before:** `src/mocks/**` not in coverage exclusions — mock infra counted as application code.
**Fix:** Added `'src/mocks/**'` to `coverage.exclude`.
**Status:** Fixed

---

### R8 · BUG · 2026-02-23 — root.tsx: lang="en" on a pt-BR app

**File:** `app/root.tsx`
**Before:** `<html lang="en">` — incorrect locale for a Brazilian Portuguese app.
**Fix:** Changed to `<html lang="pt-BR">`.
**Status:** Fixed (Batch 1A)

---

### R9 · ENH · 2026-02-23 — root.tsx: ErrorBoundary messages in English

**File:** `app/root.tsx`
**Issue:** ErrorBoundary displays English text ("The requested page could not be found", "An unexpected error occurred") in a pt-BR app.
**Status:** Open — tracked for future batch

---

### R10 · BUG · 2026-02-23 — OrderForm: focus never moves to first error field

**File:** `src/component/OrderForm/OrderForm.tsx`
**Before:** `handleSubmit` read `Object.keys(errors)` (React stale state) after `validateForm()` — `errors` was still `{}`, so `firstErrorField` was undefined and focus never moved.
**Fix:** `validateForm()` returns `FormErrors` (was `boolean`); `handleSubmit` uses the returned object directly for focus.
**Status:** Fixed (Batch 1B)

---

### R11 · BUG · 2026-02-23 — OrderController: duplicate CORS config

**File:** `backend/src/main/java/com/robertafurucho/order/OrderController.java`
**Before:** `@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})` on the controller — redundant with global `WebConfig.corsConfigurer()` which already covers `/api/**` with more complete settings (production origins, explicit methods, credentials).
**Fix:** Removed the annotation; CORS centralized in `WebConfig`.
**Status:** Fixed (Batch 1C)

---

### R12 · ENH · 2026-02-23 — pom.xml: unused Lombok dependency

**File:** `backend/pom.xml`
**Before:** Lombok declared as dependency + excluded in spring-boot-maven-plugin, but zero `import lombok` in source code.
**Fix:** Removed dependency and plugin exclusion block.
**Status:** Fixed (Batch 1D)

---

### R13 · ENH · 2026-02-23 — @SuppressWarnings("null") masking real warnings

**Files:** `OrderService.java`, `OrderControllerTest.java`
**Before:** Class-level `@SuppressWarnings("null")` suppressed all null-related warnings, hiding potential issues.
**Fix:** Removed the annotation from both files.
**Status:** Fixed (Batch 1D)

---

### R14 · BUG · 2026-02-23 — Dockerfile still references Java 17

**File:** `backend/Dockerfile`
**Before:** `eclipse-temurin:17-jdk-alpine` and `17-jre-alpine` — project was already upgraded to Java 21.
**Fix:** Updated to `21-jdk-alpine` and `21-jre-alpine`.
**Status:** Fixed (Batch 1D)

---

### R15 · BUG · 2026-02-23 — OrderControllerTest: hardcoded future dates will expire

**File:** `backend/src/test/java/com/robertafurucho/order/OrderControllerTest.java`
**Before:** 5× `2027-03-15` / `LocalDate.of(2027, 3, 15)` — will become past dates and break `@Future` validation.
**Fix:** `FUTURE_DATE = LocalDate.now().plusMonths(6)` constant + `.formatted()` for JSON text blocks.
**Status:** Fixed (Batch 1E)

---

## WhatsApp Chatbot Bug Review — 2026-03-03

### W1 · BUG · 2026-03-03 — ConversationRepository: findActiveByWaId multi-row crash

**File:** `backend/src/main/java/com/robertafurucho/whatsapp/conversation/ConversationRepository.java`
**Before:** JPQL query with `ORDER BY createdAt DESC` returning `Optional<T>` — calls `getSingleResult()` internally. If a race condition creates duplicate active conversations, every subsequent message from that user crashes with `IncorrectResultSizeDataAccessException`.
**Fix:** Changed to native query with `LIMIT 1`.
**Severity:** CRITICAL
**Status:** Fixed (Batch 6B)

---

### W2 · BUG · 2026-03-03 — ConversationRepository: findLastCompletedByWaId multi-row crash

**File:** `backend/src/main/java/com/robertafurucho/whatsapp/conversation/ConversationRepository.java`
**Before:** Same issue as W1 — a returning customer with multiple completed conversations triggers `IncorrectResultSizeDataAccessException` on `/status` command.
**Fix:** Changed to native query with `LIMIT 1`.
**Severity:** CRITICAL
**Status:** Fixed (Batch 6B)

---

### W3 · BUG · 2026-03-03 — ConversationState: no @Version for optimistic locking

**File:** `backend/src/main/java/com/robertafurucho/whatsapp/conversation/ConversationState.java`
**Before:** No concurrency protection. Two concurrent webhook updates to the same conversation silently overwrite each other's data (last-write-wins).
**Fix:** Added `@Version private Long version` field for JPA optimistic locking.
**Severity:** CRITICAL
**Status:** Fixed (Batch 6B)

---

### W4 · BUG · 2026-03-03 — ConversationService: correction flow forces re-entry of all remaining fields

**File:** `backend/src/main/java/com/robertafurucho/whatsapp/conversation/ConversationService.java`
**Before:** When user selects "edit name" at CONFIRM, the flow goes ASK_NAME → ASK_EMAIL → ASK_PHONE → ... → CONFIRM (re-enters ALL fields after the corrected one). Should return directly to CONFIRM after the corrected field.
**Fix:** Added `correcting` boolean to `ConversationState`. When true, `handleDataCollection` and `handleOrderScope` return to CONFIRM instead of calling `step.next()`.
**Severity:** CRITICAL
**Status:** Fixed (Batch 6B)

---

### W5 · BUG · 2026-03-03 — WhatsAppClient: subscribe() without error consumer

**File:** `backend/src/main/java/com/robertafurucho/whatsapp/WhatsAppClient.java`
**Before:** `.subscribe()` with no arguments — `doOnError()` is a side-effect operator, error still propagates to subscribe. With no error consumer, throws `ErrorCallbackNotImplemented` on the async reactor thread, crashing it.
**Fix:** Changed to `.subscribe(resp -> {}, err -> log.error(...))`.
**Severity:** MAJOR
**Status:** Fixed (Batch 6B)

---

### W6 · BUG · 2026-03-03 — ConversationService: no auto-expire after maxRetries

**File:** `backend/src/main/java/com/robertafurucho/whatsapp/conversation/ConversationService.java`
**Before:** After `maxRetries` validation failures, user sees "you can /cancelar" but conversation stays active. Each new message resets `updatedAt`, preventing scheduled expiration. User is stuck forever.
**Fix:** At `retries >= maxRetries`, conversation is auto-expired (EXPIRED + expired=true) and user notified.
**Severity:** MAJOR
**Status:** Fixed (Batch 6B)

---

### W7 · BUG · 2026-03-03 — ConversationService: /status and /ajuda create orphan conversations

**File:** `backend/src/main/java/com/robertafurucho/whatsapp/conversation/ConversationService.java`
**Before:** Special commands were handled after `findActiveByWaId().orElseGet(createNew)` — `/status` and `/ajuda` always created a new GREETING conversation row that was never used.
**Fix:** Moved `/status` and `/ajuda` handling before find-or-create block.
**Severity:** MAJOR
**Status:** Fixed (Batch 6B)

---

### W8 · BUG · 2026-03-03 — ConversationState: setFieldForStep(ASK_DATE) no array bounds check

**File:** `backend/src/main/java/com/robertafurucho/whatsapp/conversation/ConversationState.java`
**Before:** `value.split("/")` with immediate `parts[0]`, `parts[1]`, `parts[2]` access — if a malformed date bypasses validator (or is called from test code), `ArrayIndexOutOfBoundsException`.
**Fix:** Added `if (parts.length != 3)` guard before array access.
**Severity:** MAJOR
**Status:** Fixed (Batch 6B)

---

### W9 · ENH · 2026-03-03 — InteractiveMessageBuilder: buildSummaryText renders "null"

**File:** `backend/src/main/java/com/robertafurucho/whatsapp/message/InteractiveMessageBuilder.java`
**Before:** `String.formatted(state.getName(), ...)` — if any field is null, the summary text shows the literal string "null" to the customer.
**Fix:** Added `safe()` helper that returns "—" for null values.
**Status:** Fixed (Batch 6B)

---

### W10 · ENH · 2026-03-03 — ConversationExpirationJob: saves one-by-one instead of batch

**File:** `backend/src/main/java/com/robertafurucho/whatsapp/scheduler/ConversationExpirationJob.java`
**Before:** `repository.save(state)` inside the for-loop — N individual INSERT statements instead of one batch.
**Fix:** Moved to `repository.saveAll(expired)` after the loop.
**Status:** Fixed (Batch 6B)

---

### W11 · BUG · 2026-03-03 — ConversationServiceTest: Strictness.LENIENT hides dead stubs

**File:** `backend/src/test/java/com/robertafurucho/whatsapp/conversation/ConversationServiceTest.java`
**Before:** `@MockitoSettings(strictness = Strictness.LENIENT)` suppressed all warnings about unnecessary stubbings — hiding false-positive tests and dead setup code.
**Fix:** Removed LENIENT, fixed all resulting strict-stubbing violations.
**Status:** Fixed (Batch 6B)

---

### W12 · BUG · 2026-03-03 — ConversationServiceTest: confirmCreatesOrder uses any() matcher

**File:** `backend/src/test/java/com/robertafurucho/whatsapp/conversation/ConversationServiceTest.java`
**Before:** `verify(orderService).createOrder(any())` — never verified that the field mapping from ConversationState to CreateOrderRequest was correct. A bug swapping name↔email would pass.
**Fix:** Used `ArgumentCaptor<CreateOrderRequest>` to verify all 7 field values.
**Status:** Fixed (Batch 6B)
