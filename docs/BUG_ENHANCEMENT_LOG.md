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
