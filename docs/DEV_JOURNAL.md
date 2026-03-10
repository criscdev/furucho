# Development Journal - Roberta Furucho Platform

This journal tracks the development progress, decisions, and lessons learned during the creation of the Roberta Furucho handmade biscuit dolls platform.

---

## 2026-03-03 — WhatsApp Chatbot Bug Review & Fixes

### Summary

Comprehensive code review of the WhatsApp chatbot implementation (12 production + 5 test files). Identified 15 production bugs (4 CRITICAL, 5 MAJOR, 6 MINOR) and 18 test quality issues (3 CRITICAL, 8 MAJOR, 7 MINOR). All CRITICAL and MAJOR issues fixed in one session, plus selected MINOR fixes. Test count rose from 123 to 125, all green.

### What Was Done

#### CRITICAL Production Fixes (4)

| Bug | File | Fix |
| --- | --- | --- |
| `findActiveByWaId` multi-row crash | `ConversationRepository.java` | Native query with `LIMIT 1` |
| `findLastCompletedByWaId` multi-row crash | `ConversationRepository.java` | Native query with `LIMIT 1` |
| No optimistic locking (concurrent updates overwrite data) | `ConversationState.java` | Added `@Version private Long version` |
| Correction flow forces re-entry of all remaining fields | `ConversationService.java` | Added `correcting` flag — returns to CONFIRM after single-field correction |

#### MAJOR Production Fixes (5)

| Bug | File | Fix |
| --- | --- | --- |
| `subscribe()` without error consumer → `ErrorCallbackNotImplemented` | `WhatsAppClient.java` | Added error consumer lambda to `subscribe()` |
| No auto-expire after maxRetries (user stuck forever) | `ConversationService.java` | Auto-expires conversation and notifies user |
| `/status` and `/ajuda` create orphan conversations | `ConversationService.java` | Moved stateless commands before find-or-create |
| `setFieldForStep(ASK_DATE)` no array bounds check | `ConversationState.java` | Added `parts.length != 3` guard |
| `buildSummaryText` renders "null" for missing fields | `InteractiveMessageBuilder.java` | Added null-safe `safe()` helper |

#### MINOR Production Fixes (1)

| Bug | File | Fix |
| --- | --- | --- |
| Individual `save()` in expiration loop | `ConversationExpirationJob.java` | Batch `saveAll()` |

#### Test Fixes (7 changes)

- Removed `Strictness.LENIENT` from `ConversationServiceTest` (was hiding unnecessary stubbings)
- Fixed dead `stateAt()` call and duplicate `stubSave()` in `sendsGreeting`
- `confirmCreatesOrder` now uses `ArgumentCaptor` to verify all 7 field mappings
- Renamed `maxRetriesHint` → `maxRetriesAutoExpires` with EXPIRED state assertion
- Added `orderCreationFailure` test for exception path
- Added `correctionReturnsToConfirm` test for new correction flow
- Updated integration `correctionFlow` — single field correction, no re-fill

### Files Modified (10)

| File | Changes |
| --- | --- |
| `ConversationRepository.java` | Native queries with `LIMIT 1` |
| `ConversationState.java` | `@Version`, `correcting` field, date bounds check |
| `ConversationService.java` | Command ordering, correction flow, auto-expire |
| `WhatsAppClient.java` | Error consumer on `subscribe()` |
| `InteractiveMessageBuilder.java` | Null-safe summary, `safe()` helper |
| `ConversationExpirationJob.java` | Batch `saveAll()` |
| `ConversationServiceTest.java` | 7 test fixes, 2 new tests, removed LENIENT |
| `WhatsAppChatbotIntegrationTest.java` | Updated correction flow test |

### Test Counts

| Suite | Before | After | Status |
| --- | --- | --- | --- |
| JUnit 5 (backend) | 123 | 125 | ✅ BUILD SUCCESS |

### Key Technical Decisions

| Decision | Rationale |
| --- | --- |
| Native query with LIMIT 1 | Prevents `IncorrectResultSizeDataAccessException` without changing return type |
| `@Version` optimistic locking | Prevents silent data loss on concurrent webhook updates |
| `correcting` boolean field | Single-field correction returns to CONFIRM vs. re-entering all remaining fields |
| Auto-expire at maxRetries | Prevents user from being stuck in infinite retry loop |
| Move /status /ajuda before find-or-create | Stateless commands should not create orphan conversation rows |

---

## 2026-03-03 — WhatsApp Chatbot Implementation

### Summary

Full implementation of PRD 2 (WhatsApp Chatbot) — 12 production files + 5 test files, 74 new backend tests. Chatbot collects 8 order fields via WhatsApp Cloud API with conversation state machine, HMAC-SHA256 webhook validation, interactive buttons/lists, correction flow, and scheduled expiration.

### What Was Done

- 12 production Java files in `com.robertafurucho.whatsapp` package
- 5 test files (unit + integration) with 74 new tests
- Configuration: `application.properties`, `application-prod.properties`, `pom.xml`, `@EnableScheduling`
- Full state machine: GREETING → 8 fields → CONFIRM → COMPLETED
- Interactive messages: buttons (order scope, confirmation) + lists (correction)
- Special commands: /cancelar, /status, /ajuda
- Scheduled expiration job (60s polling, configurable timeout)
- Idempotency via lastMessageId deduplication

### Test Counts

| Suite | Before | After | Status |
| --- | --- | --- | --- |
| JUnit 5 (backend) | 49 | 123 | ✅ BUILD SUCCESS |

---

## 2026-03-03 — Markdown Lint Cleanup

### Summary

Fixed 448 markdown lint warnings across 6 documentation files + cleaned unused Java imports in 3 test files. Zero markdown errors remaining.

### Files Fixed

- `docs/TDD_REFACTORING_LOG.md` — 4 fixes
- `docs/DEV_JOURNAL.md` — 17 fixes
- `docs/REVIEW_AND_IMPROVEMENTS.md` — ~430 fixes (tables, MD036, MD032, MD022, MD026)
- `docs/PRD_INSTAGRAM_GALLERY.md` — ~22 fixes (MD060, MD040)
- `docs/WHATSAPP_CHATBOT_RESEARCH.md` — ~50 fixes (MD041, MD032, MD031, MD060, MD024, MD033, MD009, MD040, MD026, MD034)
- `docs/PRD_WHATSAPP_CHATBOT.md` — ~51 fixes (MD040, MD060, MD032, MD031, MD036, MD034)

---

## 2026-03-03 — TDD Refactoring Complete

### Summary

Full TDD refactoring of the Furucho codebase, from 15 frontend tests and 8 backend tests to a comprehensive 186-test suite across 3 layers.

### What Was Done

#### Testing Infrastructure (Batches 0A–0D)

- Removed `@ts-nocheck`, fixed `vitest.config.ts` with `defineConfig()`
- Installed `@vitest/coverage-v8` with 80% thresholds
- Installed Playwright 1.58.2 with 3 browser projects (Chromium, Firefox, Mobile Chrome)
- Installed MSW 2.12.10 with in-memory order store and 4 endpoint handlers

#### Bug Fixes (Batches 1A–1E)

- Fixed `lang="en"` → `lang="pt-BR"` on `<html>`
- Fixed form focus bug (stale state in `handleSubmit`)
- Removed redundant `@CrossOrigin` from controller (already in `WebConfig`)
- Removed unused Lombok dependency, upgraded Dockerfile to Java 21
- Replaced hardcoded dates with dynamic `LocalDate.now().plusMonths(6)`

#### Backend Tests (Batches 2A–2E)

- OrderService: 17 tests (createOrder, queries, update, normalizations)
- OrderController: 13 tests (CRUD, validation, error handling)
- Rate Limiting Filter: 4 tests (standalone MockMvc, per-IP buckets)
- Integration: 6 tests (@SpringBootTest + H2, full lifecycle)
- Repository: 6 tests (@DataJpaTest, custom queries)
- Health: 3 tests (basic, ready, live)

#### Frontend Tests (Batches 3A–3E)

- Welcome: 17 tests (hero, CTA, about cards, semantic structure, axe)
- useOrderFormValidation hook: 19 tests (extracted from OrderForm for SRP)
- formatWhatsAppMessage utility: 6 tests (extracted for SRP)
- OrderForm: 11 tests (submission, popup detection, a11y)
- Home route: 10 tests (meta tags, composition)
- Integration: 9 tests (full order flow with Header + Gallery + OrderForm)

#### E2E Tests (Batches 4A–4C)

- Happy path: form submit → WhatsApp redirect, validation errors
- Keyboard & a11y: skip link, tab order, axe scans at 3 viewports
- Secondary: CTA scroll, responsive gallery, SEO meta tags, Instagram security
- WCAG fix: focus color contrast from 3.96:1 to 5.99:1

#### Biscuit Scope Correction

- All content updated to exclusively reference biscuit (porcelana fria) dolls
- Removed all references to pano, feltro, crochê, amigurumi, tecidos

#### CI & Documentation (Batch 5A)

- Added `test:ci` script (vitest + playwright)
- Updated CI workflows: JDK 17→21, added Playwright to frontend CI
- Updated TESTING_STRATEGY.md with actual tools, versions, and counts
- Coverage snapshot documented (98.48% stmts, 97.61% branches)

#### 3 Senior Reviews

- Review 1: 58 issues found, 18 fixed (focus, popup, i18n, a11y, rate limiter)
- Review 2: 2 issues found, 2 fixed (Gallery alt text + JSDoc)
- Review 3: Full 46-file audit, 0 issues found — codebase clean

### Final Test Counts

| Suite | Count | Status |
| --- | --- | --- |
| Vitest (frontend unit + integration) | 95 | ✅ |
| JUnit 5 (backend) | 49 | ✅ |
| Playwright (E2E × 3 browsers) | 42 | ✅ |
| **Total** | **186** | ✅ |

### Coverage (Frontend)

| Metric | Value |
| --- | --- |
| Statements | 98.48% |
| Branches | 97.61% |
| Functions | 88.23% |
| Lines | 98.48% |

### Key Technical Decisions

| Decision | Rationale |
| --- | --- |
| Extract useOrderFormValidation hook | SRP — OrderForm went from 431→343 lines |
| Extract formatWhatsAppMessage utility | Pure function, independently testable |
| RateLimitingFilter (not @Component) | Isolates from @WebMvcTest slices |
| Smart eviction (only full buckets) | Prevents premature bucket removal under load |
| @axe-core/playwright for E2E a11y | Catches viewport-specific violations |
| Dynamic dates in all tests | Prevents future @Future annotation flakes |
| userEvent over fireEvent | Realistic event sequencing, better a11y testing |

---

## 2026-01-27 - Code Review & Bug Fixes

### Issues Fixed

- **Removed malformed file**: Deleted rogue `package.json (edit: add a "test" script...)` file
- **Spring Boot upgrade**: Updated from 3.2.5 to 3.5.11 for latest features
- **Bucket4j API**: Migrated from deprecated `Bandwidth.simple()` to new builder API
- **Test annotation**: Replaced deprecated `@MockBean` with `@MockitoBean` for Spring Boot 3.4
- **Markdown formatting**: Fixed all tables, code blocks, and list formatting across docs
- **Gallery props**: Made `items` prop optional with default placeholder data
- **Created DEV_JOURNAL.md**: Added development tracking documentation

### Files Modified

- `backend/pom.xml` - Spring Boot 3.5.11
- `backend/.../OrderController.java` - New Bucket4j builder API
- `backend/.../OrderControllerTest.java` - @MockitoBean annotation
- `README.md` - Fixed formatting, updated Spring version
- `docs/*.md` - Fixed markdown linting issues

---

## 2026-01-27 - Project Implementation Complete

### Progress Made

#### Frontend (React 19 + React Router 7)

- **Design System**: Created pastel color palette with CSS custom properties
  - Rose (`#F4B8C5`) - Primary accent
  - Lavender (`#D8D0E8`) - Secondary accent  
  - Mint (`#B8E0C8`) - Success states
  - Cream (`#FFFAF5`) - Backgrounds

- **Components Implemented**:
  - `Header` - Skip link, navigation, Instagram link, WCAG 2.2 AA compliant
  - `Welcome` - Hero section, about cards, CTAs
  - `Gallery` - Responsive image grid with lazy loading
  - `OrderForm` - 8-field form with validation and WhatsApp integration

- **Testing**: 15 unit tests passing
  - Role-based queries (no data-testid)
  - jest-axe accessibility checks
  - Form validation and submission tests

#### Backend (Java Spring Boot 3.4)

- **Order Management API**:
  - `POST /api/orders` - Create order with rate limiting
  - `GET /api/orders` - List all orders
  - `GET /api/orders/{id}` - Get order by ID
  - `PATCH /api/orders/{id}/status` - Update order status

- **Features**:
  - Jakarta Bean Validation with Portuguese messages
  - Rate limiting with Bucket4j (5 requests/minute per IP)
  - H2 database (dev) / PostgreSQL (prod)
  - CORS configuration for frontend origins

#### DevOps

- GitHub Actions CI/CD workflows for both frontend and backend
- Dockerfile for containerized backend deployment
- Deployment documentation for Azure + DigitalOcean (GitHub Student Pack)

### Technical Decisions

| Decision | Rationale |
| -------- | --------- |
| WhatsApp integration | Most common ordering method in Brazil |
| Pastel colors | Matches artisan/handmade aesthetic |
| Spring Boot 3.4 | Latest LTS with Java 17 support |
| Role-based test queries | Better accessibility, more resilient tests |

### Challenges & Solutions

1. **React 19 + Testing Library compatibility**
   - Issue: Peer dependency conflicts with `@testing-library/react@14`
   - Solution: Used `--legacy-peer-deps` flag, tests work correctly

2. **Bucket4j deprecated API**
   - Issue: `Bandwidth.classic()` and `Refill` deprecated
   - Solution: Migrated to `Bandwidth.simple()` API

3. **Gallery prop requirements**
   - Issue: TypeScript error for missing required `items` prop
   - Solution: Made `items` optional with default placeholder data

### Next Steps

- [ ] Replace placeholder WhatsApp number (`5511999999999`)
- [ ] Add real doll photos to Gallery
- [ ] Set up GitHub repository secrets for deployment
- [ ] Register domain with Namecheap (Student Pack)
- [ ] Deploy backend to Azure App Service
- [ ] Deploy frontend to DigitalOcean App Platform

---

## Project Statistics

| Metric | Value |
| ------ | ----- |
| Frontend Tests (Vitest) | 95 passing |
| Backend Tests (JUnit 5) | 125 passing |
| E2E Tests (Playwright) | 42 passing (× 3 browsers) |
| **Total Tests** | **262** |
| Components | 4 (Header, Welcome, Gallery, OrderForm) |
| Hooks | 1 (useOrderFormValidation) |
| Utilities | 1 (formatWhatsAppMessage) |
| API Endpoints | 4 + health (3) + webhook (2) |
| WhatsApp Chatbot Files | 12 production + 5 test |
| Documentation Files | 12 |

---

## File Structure Summary

```text
furucho/
├── app/                    # React Router routes (home, root)
├── src/component/          # Reusable components (Header, Gallery, OrderForm)
├── src/mocks/              # MSW handlers + server
├── src/test/               # Integration tests + factories
├── e2e/                    # Playwright E2E specs
├── backend/                # Spring Boot API (Java 21)
├── docs/                   # Project documentation
└── .github/workflows/      # CI/CD pipelines (frontend + backend)
```

---

## Last Updated

2026-03-03
