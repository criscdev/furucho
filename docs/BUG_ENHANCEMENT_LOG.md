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
