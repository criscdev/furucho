# TDD Refactoring — Log de Execução

> Registro de progresso da refatoração TDD do projeto Furucho.
> Plano completo: [TDD_REFACTORING_PLAN.md](TDD_REFACTORING_PLAN.md)

---

## Checklist de Progresso

- [x] **Batch 0A** — Corrigir infra existente (@ts-nocheck, vitest config, index.html)
- [x] **Batch 0B** — Configurar cobertura (@vitest/coverage-v8)
- [x] **Batch 0C** — Instalar e configurar Playwright
- [x] **Batch 0D** — Instalar e configurar MSW
- [x] **Batch 1A** — Fix `lang="en"` + testes root.tsx
- [x] **Batch 1B** — Fix focus OrderForm + teste
- [x] **Batch 1C** — Fix CORS duplicado
- [x] **Batch 1D** — Limpeza backend: Lombok + SuppressWarnings + Dockerfile Java 21
- [x] **Batch 1E** — Fix datas hardcoded em testes backend
- [x] **Batch 2A** — OrderService: createOrder + normalizações
- [x] **Batch 2B** — OrderService: queries e update
- [x] **Batch 2C** — OrderController: endpoints faltantes
- [x] **Batch 2D** — Rate limiting: testes + extração
- [x] **Batch 2E** — Integração backend + Repository
- [x] **Batch 3A** — Welcome component: testes
- [x] **Batch 3B** — Decompor OrderForm: hook de validação
- [x] **Batch 3C** — Decompor OrderForm: utility WhatsApp
- [x] **Batch 3D** — OrderForm: validações faltantes
- [x] **Batch 3E** — Home route + Gallery useId
- [x] **Batch 4A** — E2E P0: Happy path + validação
- [x] **Batch 4B** — E2E P0: Keyboard + a11y
- [x] **Batch 4C** — E2E P1-P2: Secundários
- [ ] **Batch 5A** — CI + documentação final

---

## Entradas

### Batch 0A — 2026-02-23

**Alterações:**

- `src/setupTests.ts`: removido `@ts-nocheck`, import side-effect mantido
- `src/test-utils.ts`: removido `@ts-nocheck`, adicionado tipagem (`ReactElement`, `RenderOptions`), JSDoc
- `vitest.config.ts`: substituído `export default {...} as any` por `defineConfig()` de `vitest/config`
- `src/index.html`: deletado (órfão — referenciava `/src/main.jsx` inexistente)

**Resultado:** `npm run typecheck` ✅ | 37/37 testes ✅ | zero regressões

---

### Batch 0B — 2026-02-23

**Alterações:**

- Instalado `@vitest/coverage-v8@0.32.4` (compatível com vitest 0.32)
- Adicionado script `test:coverage` em `package.json`
- Configurado provider v8 com reporters text/html/lcov e thresholds 80% em `vitest.config.ts`
- Nota: vitest 0.32 usa `lines`/`functions`/`branches`/`statements` como props flat (não `thresholds: {}`)

**Baseline de cobertura:** 97.32% stmts | 90.62% branches | 100% funcs | 97.32% lines

**Resultado:** `npm run typecheck` ✅ | `npm run test:coverage` ✅ | 37/37 testes ✅

---

### Batch 0C — 2026-02-23

**Alterações:**

- Instalado `@playwright/test@1.58.2` + browsers Chromium e Firefox
- Criado `playwright.config.ts` com 3 projetos: Chromium, Firefox, Mobile Chrome (Pixel 5)
- Criado `e2e/smoke.spec.ts` — 2 testes: heading visível + título da página
- Adicionado scripts `test:e2e` e `test:e2e:ui` em `package.json`
- Adicionado `exclude: ['e2e/**']` em `vitest.config.ts` para evitar conflito
- Atualizado `.gitignore` com artefatos Playwright

**Resultado:** 6/6 Playwright testes ✅ (2 × 3 browsers) | 37/37 vitest ✅ | zero regressões

---

### Batch 0D — 2026-02-23

**Alterações:**

- Instalado `msw@2.12.10` (MSW v2 com `http` handlers)
- Criado `src/mocks/handlers.ts` — handlers para 4 endpoints `/api/orders` (POST, GET, GET/:id, PATCH/:id/status), store in-memory com `resetOrders()`, tipagem `MockOrderResponse`
- Criado `src/mocks/server.ts` — `setupServer()` com handlers exportado
- Atualizado `src/setupTests.ts` — lifecycle MSW (`beforeAll` → `listen`, `afterEach` → `resetHandlers` + `resetOrders`, `afterAll` → `close`), `onUnhandledRequest: 'warn'` para não quebrar testes existentes

**Resultado:** `npx tsc --noEmit` ✅ | 37/37 testes ✅ | zero regressões

---

### Batch 1A — 2026-02-23

**TDD Cycle:**

- RED: `app/root.test.tsx` — teste `lang="pt-BR"` falha (`lang="en"` encontrado)
- GREEN: `app/root.tsx` — alterado `lang="en"` → `lang="pt-BR"`
- Adicionado: 2 testes ErrorBoundary (404 route error + generic error)
- Mock de componentes React Router (Meta, Links, Scripts, ScrollRestoration) para isolar Layout
- `renderToStaticMarkup` para testar atributo `lang` no `<html>` sem conflito jsdom

**Resultado:** `npx tsc --noEmit` ✅ | 40/40 testes ✅ | zero regressões

---

### Batch 1B — 2026-02-23

**TDD Cycle:**

- RED: teste `focuses the first invalid field on empty submit` falha — focus fica no botão submit
- ROOT CAUSE: `handleSubmit` lia `Object.keys(errors)` (state stale) ao invés do `newErrors` retornado por `validateForm()`
- GREEN: `validateForm()` agora retorna `FormErrors` (não `boolean`); `handleSubmit` usa o objeto retornado diretamente
- Todos os 9 testes existentes do OrderForm continuam passando

**Resultado:** `npx tsc --noEmit` ✅ | 41/41 testes ✅ | zero regressões

---

### Batch 1C — 2026-02-23

**Alterações:**

- Removido `@CrossOrigin(origins = {...})` de `OrderController.java` — redundante com `WebConfig.corsConfigurer()`
- `WebConfig` já cobre `/api/**` com localhost + produção, métodos, headers, credentials, maxAge
- A anotação no controller era um subconjunto (só localhost, sem métodos/headers explícitos)

**Resultado:** `./mvnw clean test` ✅ (8/8) | zero regressões

---

### Batch 1D — 2026-02-23

**Alterações:**

- Removido Lombok do `pom.xml` (dependência + plugin exclusion) — zero `import lombok` no código-fonte
- Removido `@SuppressWarnings("null")` de `OrderService.java` e `OrderControllerTest.java`
- Atualizado `backend/Dockerfile`: `eclipse-temurin:17-jdk-alpine` → `21-jdk-alpine`, `17-jre-alpine` → `21-jre-alpine`

**Resultado:** `./mvnw clean test` ✅ (8/8) | zero regressões

---

### Batch 1E — 2026-02-23

**Alterações:**

- Substituído `LocalDate.of(2027, 3, 15)` e `"2027-03-15"` por constantes dinâmicas em `OrderControllerTest.java`
- Adicionado `FUTURE_DATE = LocalDate.now().plusMonths(6)` e `FUTURE_DATE_STR = FUTURE_DATE.toString()`
- JSON text blocks usam `"%s".formatted(FUTURE_DATE_STR)` para interpolação
- Grep por datas literais retorna zero resultados

**Resultado:** `./mvnw clean test` ✅ (8/8) | zero regressões

---

### Batch 2A — 2026-02-23

**TDD Cycle:** GREEN (testes escritos cobrindo código já existente no service)

**Alterações:**

- Criado `OrderServiceTest.java` com `@Nested class CreateOrder` contendo 6 testes:
  - `mapsAllFields` — todos os campos do request mapeados para a entidade
  - `setsStatusPending` — status inicial sempre PENDING
  - `returnsResponse` — response com ID da entidade salva
  - `normalizesPhone` — strips non-digits (ex: `(11) 98765-4321` → `11987654321`)
  - `normalizesCep` — remove hífen (ex: `01234-567` → `01234567`)
  - `keepsCepWithoutHyphen` — CEP sem hífen permanece inalterado
- Javadoc já presente em todos os métodos do `OrderService`

**Resultado:** `./mvnw test` ✅ (14/14) | zero regressões

---

### Batch 2B — 2026-02-23

**TDD Cycle:** GREEN (tests written for existing service methods)

**Alterações:**

- Adicionado helper `buildOrder(id, email, status)` no `OrderServiceTest.java`
- Adicionado `@Nested class GetAllOrders` (3 testes): empty list, mapeamento, preservação de ordem
- Adicionado `@Nested class GetOrderById` (3 testes): happy path, `OrderNotFoundException`, mensagem da exception contém o ID
- Adicionado `@Nested class GetOrdersByEmail` (2 testes): filtro por email, lista vazia
- Adicionado `@Nested class UpdateOrderStatus` (3 testes): atualiza status, persiste no repo, `OrderNotFoundException`

**Resultado:** `./mvnw test` ✅ (25/25) | zero regressões

---

### Batch 2C — 2026-02-23

**TDD Cycle:** RED→GREEN (new tests written for untested controller paths)

**Alterações:**

- `GlobalExceptionHandler.java`: adicionado `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` → retorna 400 com `"Parâmetro inválido"` (em vez de cair no catch-all 500)
- `OrderControllerTest.java` — 8 novos testes:
  - `getAllOrders_ReturnsListOfOrders` — lista de 2 pedidos, verifica status 200, tamanho e campos
  - `getAllOrders_WhenEmpty_ReturnsEmptyList` — lista vazia retorna `[]` com 200
  - `updateOrderStatus_WithValidStatus_ReturnsUpdatedOrder` — PATCH status válido retorna 200 + status atualizado
  - `updateOrderStatus_WhenNotFound_ReturnsNotFound` — OrderNotFoundException → 404
  - `updateOrderStatus_WithInvalidStatus_ReturnsBadRequest` — enum inválido → 400 `"Parâmetro inválido"`
  - `createOrder_WithPastDate_ReturnsBadRequest` — `@Future` violation → 400 + `fieldErrors.receiveDate`
  - `createOrder_WithInvalidPhone_ReturnsBadRequest` — `@Pattern` violation → 400 + `fieldErrors.phone`
  - `createOrder_WithNameTooLong_ReturnsBadRequest` — `@Size(max=200)` violation → 400 + `fieldErrors.name`

**Resultado:** `./mvnw test` ✅ (33/33) | zero regressões

### Batch 2D — 2026-02-28 (hardened 2026-03-02)

**TDD Cycle:** REFACTOR (extract rate limiting out of `OrderController` + harden)

**Alterações (extração):**

- `RateLimitingFilter.java` (novo) — classe avulsa (sem `@Component`); rate limit de 5 req/min por IP via bucket4j; per-IP bucketing com `ConcurrentHashMap`
- `WebConfig.java` — adicionado bean `FilterRegistrationBean<RateLimitingFilter>` registrado para `/api/orders` e `/api/orders/` com `order=1`; mantém `@WebMvcTest` isolado
- `OrderController.java` — removidos todos os imports e lógica de bucket4j; `createOrder()` simplificado a `@RequestBody` apenas, retorna `ResponseEntity<OrderResponse>`
- `RateLimitingFilterTest.java` (novo) — 4 testes via standaloneSetup MockMvc

**Alterações (hardening — code review pass):**

- `RateLimitingFilter.java` — `ObjectMapper` via construtor (JSON seguro, sem string manual); header `Retry-After: 60` em 429 (RFC 6585); `ScheduledExecutorService` daemon para evicção periódica de buckets stale; `destroy()` para shutdown do scheduler; `isRateLimited()` simplificado p/ check de método apenas (URL scoping delegado ao container); `extractClientIp()` → `private`; `resetBuckets()` removido (dead code)
- `WebConfig.java` — injeta `ObjectMapper` no construtor do filtro
- `RateLimitingFilterTest.java` — teste direto `extractClientIp` removido (comportamento coberto por `xForwardedFor_SeparatesBucketsPerIp`); assertiva de header `Retry-After` adicionada no teste 429; `@SuppressWarnings("null")` mantido com comentário (quirk Spring MockMvc)
- `pom.xml` — pin de `maven-compiler-plugin 3.13.0` adicionado e depois removido (era workaround para JRE-only; JDK agora instalado)

**Resultado:** `./mvnw test` ✅ (37/37) | zero regressões | 0 Problems

### Batch 2E — 2026-03-02

**TDD Cycle:** GREEN (testes escritos cobrindo código já existente)

**Alterações:**

- `OrderRepositoryTest.java` (novo) — 6 testes com `@DataJpaTest` + H2:
  - `findByEmailOrderByCreatedAtDesc`: retorna pedidos do email, vazio quando não encontra
  - `findByStatusOrderByCreatedAtDesc`: filtra por status, vazio quando não encontra
  - `findAllByOrderByCreatedAtDesc`: retorna todos, vazio quando sem dados
- `OrderIntegrationTest.java` (novo) — 6 testes com `@SpringBootTest` + `@AutoConfigureMockMvc(addFilters = false)` + H2:
  - `fullLifecycle_CreateGetPatchGet`: POST → GET → PATCH → GET completo, verifica todos os campos
  - `getAllOrders_ReturnsListNewestFirst`: 2 pedidos, verifica ordem
  - `getById_NotFound`: ID inexistente → 404
  - `createOrder_InvalidBody_Returns400`: validação retorna 400 + fieldErrors
  - `updateStatus_InvalidStatus_Returns400`: enum inválido → 400
  - `dataPersists_AcrossRequests`: verify count via repository
- `addFilters = false` para desabilitar `RateLimitingFilter` em testes de integração (testado à parte)

**Resultado:** `./mvnw test` ✅ (49/49) | zero regressões | 0 Problems

### Batch 3A — 2026-03-02

**TDD Cycle:** GREEN (testes escritos cobrindo componente existente)

**Alterações:**

- `app/welcome/welcome.test.tsx` (novo) — 17 testes com Vitest + RTL + jest-axe:
  - **Hero Section**: h1 "Roberta Furucho", subtitle "Bonecas Artesanais", parágrafo descritivo
  - **CTA Button**: renderiza "Fazer Encomenda" com `type="button"`, scrollIntoView + focus no `#order-form`, não lança quando elemento ausente
  - **Instagram Link**: URL padrão, `target="_blank"` + `rel="noopener noreferrer"`, URL customizada via prop, aria-label descritivo
  - **About Section**: heading h2 "Sobre as Bonecas", 3 cards h3 (Feito à Mão, Personalizado, Materiais de Qualidade), parágrafos descritivos
  - **Semantic Structure**: `<main id="main">`, hero `aria-labelledby="hero-heading"`, about `aria-labelledby="about-heading"`
  - **Accessibility**: axe audit sem violações
- JSDoc já existente em `welcome.tsx` — nenhuma alteração necessária

**Resultado:** `npx vitest run` ✅ (58/58) | `tsc --noEmit` ✅ | `./mvnw test` ✅ (49/49) | 0 Problems | dev server + browser ✅

### Batch 3B — 2026-03-02

**TDD Cycle:** RED → GREEN → REFACTOR (hook extraído de componente existente)

**Alterações:**

- `useOrderFormValidation.test.ts` (novo) — 17 testes com Vitest + RTL (`renderHook`):
  - Estado inicial: todos os campos vazios, sem erros
  - `handleChange`: atualiza campo e limpa erro ao editar
  - `validate`: nome obrigatório, email formato, telefone 10-11 dígitos, CEP 8 dígitos, data DD/MM/AAAA, endereço obrigatório, escopo obrigatório, múltiplos erros simultâneos
  - Regex: rejeita telefone com letras, CEP curto, data inválida
- `useOrderFormValidation.ts` (novo) — custom hook extraído de `OrderForm.tsx`:
  - `formData`, `errors`, `handleChange`, `validate` retornados
  - Lógica de validação idêntica à original
- `OrderForm.tsx` — refatorado para usar `useOrderFormValidation()` (427 → 348 linhas, −79)

**Resultado:** `npx vitest run` ✅ (75/75) | zero regressões | 0 Problems

### Batch 3C — 2026-03-02

**TDD Cycle:** RED → GREEN → REFACTOR (utility extraída de componente existente)

**Alterações:**

- `formatWhatsAppMessage.test.ts` (novo) — 6 testes:
  - Retorno URI-encoded, todos os campos presentes, labels bold (*Nome:*, etc.), header com emoji 🧸, caracteres especiais, campos vazios
- `formatWhatsAppMessage.ts` (novo) — utility pura com JSDoc:
  - Recebe `OrderFormData`, retorna `encodeURIComponent(...)` da mensagem formatada
- `OrderForm.tsx` — refatorado para importar utility (348 → 334 linhas, −14)

**Resultado:** `npx vitest run` ✅ (81/81) | zero regressões | 0 Problems

### Batch 3D — 2026-03-02

**TDD Cycle:** GREEN (teste cobrindo path de erro existente)

**Alterações:**

- `OrderForm.test.tsx` — +1 teste:
  - `shows error alert when window.open throws`: mock `window.open` → throw → verifica `role="alert"` com "Erro ao processar"
- Tasks 3D.1-3D.4 (telefone, CEP, data, nome) já cobertas em `useOrderFormValidation.test.ts` (Batch 3B)

**Resultado:** `npx vitest run` ✅ (82/82) | zero regressões | 0 Problems

### Batch 3E — 2026-03-02

**TDD Cycle:** RED → GREEN + REFACTOR

**Alterações:**

- `app/routes/home.test.tsx` (novo) — 9 testes:
  - **meta()**: título com "Roberta Furucho", description, Open Graph tags (title/desc/type), theme-color
  - **Composição**: renderiza Header (navigation), Welcome (h1), Gallery (h2), OrderForm (h2)
  - **Accessibility**: axe audit sem violações
- `Gallery.tsx` — `id="gallery-heading"` estático → `useId()` dinâmico (evita colisão de IDs)
- `Gallery.test.tsx` — atualizado teste `aria-labelledby` para verificar ligação dinâmica heading↔section
- `home.tsx` — JSDoc adicionado (composição, SEO, referências @see)

**Resultado:** `npx vitest run` ✅ (91/91) | zero regressões | 0 Problems

### Batch 4A–4C — 2026-03-02

**TDD Cycle:** RED → GREEN (E2E specs + contrast fix)

**Fix de contraste WCAG AA (P0):**

- `app.css`: `--color-focus` escurecido de `#9D6BA8` → `#7B508C` (amethyst profundo)
  - Sobre cream `#FFFAF5`: 3.96:1 → **5.99:1** ✅
  - Sob white `#FFFFFF`: 4.11:1 → **6.23:1** ✅
  - Mesma família de matiz (~290° purple), mantém estética artesanal
- `--color-focus-ring` atualizado para `rgba(123, 80, 140, 0.5)`
- Dependência: `@axe-core/playwright@4.11.1` adicionada (E2E a11y audits)

**E2E specs criados:**

- `e2e/happy-path.spec.ts` — 3 testes (Batch 4A):
  - Página carrega + form visível
  - Preenche form → submit → WhatsApp redirect + success message
  - Submit vazio → erros → corrige nome → erro limpa, outros permanecem
- `e2e/a11y.spec.ts` — 5 testes (Batch 4B):
  - Tab: skip link → Tab through page → CTA "Fazer Encomenda" alcançável
  - axe scan 3 viewports: desktop 1280px, tablet 768px, mobile 375px — 0 violações
  - Skip link Tab → Enter → `#main` visível
- `e2e/secondary.spec.ts` — 4 testes (Batch 4C):
  - CTA scroll → `#order-form` in viewport
  - Gallery single-column em 375px
  - Meta tags SEO (title, description, OG, theme-color)
  - Instagram links security attrs (target, rel)
- `e2e/smoke.spec.ts` — 2 testes (já existente)

**Resultado:** `npx vitest run` ✅ (91/91) | `npx playwright test` ✅ (42/42 × 3 browsers) | 0 regressões
