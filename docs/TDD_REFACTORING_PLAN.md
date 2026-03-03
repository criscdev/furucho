# Plano de Refatoração TDD — Furucho

> **Autor:** AI Assistant (Claude Opus 4.6) sob direção de Cristina Carvalho (NTT Data)
> **Data de criação:** 2026-02-23
> **Última revisão:** 2026-02-23 (rev 2 — pós upgrade Java 21)
> **Status:** CONCLUÍDO — todas as tasks executadas (0A–5A)

---

## 1. Contexto e Motivação

### 1.1 O que é este documento

Registro de decisões técnicas, passos executados e planejamento de refatoração completa
do repositório **criscdev/furucho** usando TDD (Test-Driven Development). Serve como:

- **Rastreabilidade**: cada decisão tem justificativa documentada
- **Continuidade**: permite retomar trabalho em qualquer prompt futuro sem perda de contexto
- **Qualidade**: prioriza profundidade sobre velocidade — tarefas fatiadas pequenas

### 1.2 Estado do projeto antes da refatoração

O Furucho é uma plataforma de encomendas de bonecas artesanais da Roberta Furucho.

**Stack:**

- Frontend: React 19 + React Router 7 + TailwindCSS 4 + TypeScript 5
- Backend: Spring Boot 3.5.11 + Java 21 + H2 (dev) / PostgreSQL (prod)
- Testes: Vitest + RTL + jest-axe (front) / JUnit 5 + Mockito + MockMvc (back)

**Audit realizado em 2026-02-23** revelou:

| Área | Testes existentes | Gaps críticos |
| ------ | ------------------- | --------------- |
| Frontend — Header | 7 testes, excelente | — |
| Frontend — Gallery | 12 testes, excelente | — |
| Frontend — OrderForm | 8 testes, bom | Focus bug, validações faltando, monólito 431 linhas |
| Frontend — Welcome | **0 testes** | Componente inteiro sem cobertura |
| Frontend — root.tsx | **0 testes** | ErrorBoundary sem cobertura, `lang="en"` errado |
| Frontend — Home route | **0 testes** | Meta tags, composição sem cobertura |
| Backend — HealthController | 3 testes, completo | — |
| Backend — OrderController | 5 testes, parcial | GET all, PATCH status, rate limiting sem teste |
| Backend — OrderService | **0 testes** | Service layer inteira sem cobertura |
| Backend — Integração | **0 testes** | Nenhum @SpringBootTest |
| E2E — Playwright | **Não instalado** | Zero infraestrutura E2E |
| Infra — Cobertura | **Não configurado** | Sem @vitest/coverage-v8 |
| Infra — MSW | **Não instalado** | Sem mock de API para frontend |

### 1.3 Bugs conhecidos encontrados no audit

| # | Bug | Localização | Severidade |
| --- | ----- | ------------- | ------------ |
| B1 | Focus no primeiro campo com erro usa state stale (React `setErrors` é async, mas `errors` é lido imediatamente após) | `OrderForm.tsx` ~L155-160 | Alta |
| B2 | `<html lang="en">` deveria ser `"pt-BR"` — site inteiro é em português | `root.tsx` ~L32 | Alta (a11y/SEO) |
| B3 | CORS duplicado e conflitante: `WebConfig.java` (global) vs `@CrossOrigin` no `OrderController` (parcial, sem prod domains) | Backend | Média |
| B4 | `src/index.html` é órfão — referencia `/src/main.jsx` inexistente | `src/index.html` | Baixa (lixo) |
| B5 | Lombok declarado no `pom.xml` mas nunca usado — dependência morta | `pom.xml` | Baixa |
| B6 | `@SuppressWarnings("null")` mascarando potenciais NPEs | `OrderService`, `OrderControllerTest` | Média |
| B7 | `@ts-nocheck` em `setupTests.ts` e `test-utils.ts` — viola CODING_STANDARDS.md | Infra de testes | Média |
| B8 | `vitest.config.ts` exporta como `any` — sem tipagem | Config | Baixa |
| B9 | Gallery `id="gallery-heading"` estático — colisão se dois Gallery na página | `Gallery.tsx` | Baixa |

---

## 2. Princípios de Execução

### 2.1 TDD Rigoroso

Cada mudança segue o ciclo:

1. **RED**: Escrever teste que falha (descreve o comportamento desejado)
2. **GREEN**: Implementar o mínimo para o teste passar
3. **REFACTOR**: Melhorar design sem quebrar testes

### 2.2 Fatiamento de Tarefas

- Cada "batch" (prompt) foca em **1-3 tarefas pequenas** no máximo
- Cada tarefa deve ser **verificável isoladamente** (teste passa ou falha)
- Nenhuma tarefa depende de outra dentro do mesmo batch (quando possível)
- Ao final de cada batch: rodar testes para confirmar não-regressão

### 2.3 JSDoc como Contexto para IA

Todo código produzido inclui JSDoc com:

- `@description` — o que faz
- `@param` / `@returns` — contratos
- `@example` — uso esperado
- `@see` — referências cruzadas
- `@a11y` (custom) — considerações de acessibilidade quando aplicável
- `@decision` (custom) — justificativa de decisão técnica quando relevante

### 2.4 Qualidade > Velocidade

- Preferir fazer 2 tarefas bem feitas por prompt do que 10 superficiais
- Cada teste deve testar **comportamento**, não implementação
- Cada refatoração deve manter testes existentes passando (regressão)

---

## 3. Plano de Execução — Fatiamento em Batches

### Batch 0: Infraestrutura de Testes (pré-requisito para tudo)

> **Motivação:** Sem infra sólida, não dá para fazer TDD. Playwright não existe,
> cobertura não é mensurável, e a infra existente tem problemas de tipagem.

#### Batch 0A — Corrigir infra existente

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 0A.1 | Remover `@ts-nocheck` de `setupTests.ts` | Viola CODING_STANDARDS.md; esconde erros reais de tipo | `npm run typecheck` passa |
| 0A.2 | Remover `@ts-nocheck` de `test-utils.ts` | Idem | `npm run typecheck` passa |
| 0A.3 | Tipar `vitest.config.ts` corretamente (remover `as any`) | Tipagem fraca impede IDE de ajudar | `npm run typecheck` passa |
| 0A.4 | Deletar `src/index.html` órfão | Arquivo confuso, referencia código inexistente | Arquivo não existe mais |

**Estimativa:** 1 prompt

#### Batch 0B — Configurar cobertura

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 0B.1 | Instalar `@vitest/coverage-v8` | TESTING_STRATEGY.md exige ≥80% mas não há forma de medir | `npm run test:coverage` gera relatório |
| 0B.2 | Adicionar script `test:coverage` no `package.json` | Conveniência e CI | Script funciona |
| 0B.3 | Configurar thresholds em `vitest.config.ts` | Falhar CI se cobertura cair abaixo de 80% | Config presente |

**Estimativa:** 1 prompt

#### Batch 0C — Instalar e configurar Playwright

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 0C.1 | Instalar `@playwright/test` | E2E é pilar da pirâmide de testes (TESTING_STRATEGY.md) | `npx playwright --version` |
| 0C.2 | Criar `playwright.config.ts` | Configurar browsers, baseURL, webServer | Arquivo existe e é válido |
| 0C.3 | Criar primeiro teste smoke: página carrega e tem heading | Validar que setup funciona end-to-end | `npx playwright test` passa |
| 0C.4 | Adicionar scripts no `package.json`: `test:e2e`, `test:e2e:ui` | Conveniência | Scripts funcionam |

**Estimativa:** 1 prompt

#### Batch 0D — Instalar e configurar MSW

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 0D.1 | Instalar `msw` | Mock de API para testes de integração frontend | Pacote instalado |
| 0D.2 | Criar `src/mocks/handlers.ts` com handlers para `/api/orders` | Handlers mock prontos | Arquivo existe |
| 0D.3 | Criar `src/mocks/server.ts` para setup do server MSW | Integração com Vitest | Arquivo existe |
| 0D.4 | Integrar MSW no `setupTests.ts` | Server liga/desliga automaticamente | Testes existentes continuam passando |

**Estimativa:** 1 prompt

---

### Batch 1: Bug Fixes com TDD

> **Motivação:** Bugs devem ser corrigidos antes de novas features.
> Cada fix começa com um teste que reproduz o bug (RED), depois corrige (GREEN).

#### Batch 1A — Fix `lang="en"` → `"pt-BR"` + teste

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 1A.1 | Criar `app/root.test.tsx` com teste que verifica `lang="pt-BR"` | RED: teste falha porque lang é "en" | Teste existe e falha |
| 1A.2 | Corrigir `lang` em `root.tsx` | GREEN: teste passa | `npm test root` passa |
| 1A.3 | Adicionar teste para ErrorBoundary (404 e genérico) nesse mesmo arquivo | Aproveitar o novo arquivo de teste | Testes passam |

**Estimativa:** 1 prompt

#### Batch 1B — Fix focus no OrderForm + teste

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 1B.1 | Escrever teste: submit vazio → `document.activeElement` é o primeiro campo com erro | RED: teste falha (focus não funciona, state stale) | Teste existe e falha |
| 1B.2 | Refatorar `handleSubmit` para usar `newErrors` ao invés de `errors` state | GREEN: teste passa | `npm test OrderForm` passa |
| 1B.3 | Verificar que testes existentes do OrderForm continuam passando | Regressão | Todos 8+ testes passam |

**Estimativa:** 1 prompt

#### Batch 1C — Fix CORS duplicado

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 1C.1 | Criar teste de integração CORS que verifica headers | RED se necessário | Teste existe |
| 1C.2 | Remover `@CrossOrigin` do `OrderController`, centralizar em `WebConfig` | Eliminar conflito, SRP | Teste CORS passa |

**Estimativa:** 1 prompt

#### Batch 1D — Limpeza backend: Lombok + SuppressWarnings + Dockerfile

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 1D.1 | Remover Lombok do `pom.xml` (dependência morta — nenhum `import lombok` no source) | Limpeza, reduz complexidade de build | `./mvnw compile` passa |
| 1D.2 | Remover `@SuppressWarnings("null")` de `OrderService` e `OrderControllerTest`, tratar nulls corretamente | Código mais seguro, warnings reais ficam visíveis | Testes passam sem warnings |
| 1D.3 | Atualizar `backend/Dockerfile`: `eclipse-temurin:17-jdk-alpine` → `21-jdk-alpine`, `17-jre-alpine` → `21-jre-alpine` | Dockerfile ainda referencia Java 17 após upgrade para 21 | `docker build` funciona |

**Estimativa:** 1 prompt

#### Batch 1E — Fix datas hardcoded em testes backend

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 1E.1 | Substituir datas hardcoded em `OrderControllerTest` por `LocalDate.now().plusMonths(6)` | Datas no passado causam falha `@Future` — já quebrou no upgrade Java 21 | Testes passam hoje e daqui a 1 ano |
| 1E.2 | Verificar e corrigir qualquer outra data hardcoded em testes backend | Prevenir flakiness futura | Grep por datas literais retorna zero |

**Estimativa:** 1 prompt

---

### Batch 2: Backend TDD — OrderService

> **Motivação:** Service layer é onde vive a lógica de negócio.
> Zero cobertura aqui é risco alto. TDD puro: escrever testes primeiro.

#### Batch 2A — OrderService: createOrder + normalizações

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 2A.1 | Criar `OrderServiceTest.java` com testes para `createOrder()` | Mapeamento de campos, status default PENDING | Testes passam |
| 2A.2 | Testes para `normalizePhone()` — formatos brasileiros | Lógica de normalização sem cobertura | Testes passam |
| 2A.3 | Testes para `normalizeCep()` — formato 8 dígitos | Lógica de normalização sem cobertura | Testes passam |
| 2A.4 | Adicionar JSDoc/Javadoc em métodos testados | Contexto para IA futura | Docs presentes |

**Estimativa:** 1 prompt

#### Batch 2B — OrderService: queries e update

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 2B.1 | Testes para `getAllOrders()` — ordenação, lista vazia | Verificar comportamento de listagem | Testes passam |
| 2B.2 | Testes para `getOrderById()` — happy path + OrderNotFoundException | Exceção customizada sem cobertura | Testes passam |
| 2B.3 | Testes para `getOrdersByEmail()` — filtro funciona | Método existe mas não é exposto via controller | Testes passam |
| 2B.4 | Testes para `updateOrderStatus()` — transição + não encontrado | Estado da order muda corretamente | Testes passam |

**Estimativa:** 1 prompt

#### Batch 2C — OrderController: endpoints faltantes

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 2C.1 | Teste para `GET /api/orders` — lista todos, lista vazia | Endpoint sem cobertura | Testes passam |
| 2C.2 | Teste para `PATCH /api/orders/{id}/status` — status válido, inválido, not found | Endpoint sem cobertura | Testes passam |
| 2C.3 | Testes de validação adicionais: `@Future`, `@Pattern` telefone, `@Size` nome | Constraints não verificadas | Testes passam |
| 2C.4 | Adicionar Javadoc nos métodos do controller | Contexto futuro | Docs presentes |

**Estimativa:** 1 prompt

#### Batch 2D — Rate limiting: testes + extração

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 2D.1 | Teste de rate limiting: 6 requests → 429 no 6º | Bucket4j nunca testado | Teste passa |
| 2D.2 | Extrair rate limiting para `RateLimitingFilter` (interceptor) | SRP: controller não deve gerenciar buckets | Refatoração limpa |
| 2D.3 | Testes unitários para `RateLimitingFilter` isolado | Filter testável sem controller | Testes passam |

**Estimativa:** 1 prompt

#### Batch 2E — Integração backend + Repository

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 2E.1 | Criar `OrderIntegrationTest.java` com `@SpringBootTest` | Fluxo POST→GET→PATCH→GET com H2 real | Teste passa |
| 2E.2 | Testes de `OrderRepository` — custom queries | `findByEmail`, `findByStatus` | Testes passam |

**Estimativa:** 1 prompt

---

### Batch 3: Frontend TDD — Welcome + decomposição OrderForm

#### Batch 3A — Welcome component: testes novos

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 3A.1 | Criar `app/welcome/welcome.test.tsx` | Componente com zero cobertura | Arquivo existe |
| 3A.2 | Testes: heading h1, CTA scroll, Instagram link, about cards | Funcionalidades core | Testes passam |
| 3A.3 | Teste axe a11y | Padrão do projeto | Teste passa |
| 3A.4 | JSDoc no welcome.tsx | Contexto | Doc presente |

**Estimativa:** 1 prompt

#### Batch 3B — Decompor OrderForm: extrair hook de validação

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 3B.1 | Criar `useOrderFormValidation.test.ts` com testes para o hook | RED: hook não existe ainda | Testes escritos |
| 3B.2 | Extrair `useOrderFormValidation()` de OrderForm | GREEN: testes passam, responsabilidade isolada | Hook funciona |
| 3B.3 | OrderForm usa o hook extraído | Refator: componente menor, mesma funcionalidade | Testes existentes passam |

**Estimativa:** 1 prompt

#### Batch 3C — Decompor OrderForm: extrair utility WhatsApp

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 3C.1 | Criar `formatWhatsAppMessage.test.ts` com testes | RED: utility não existe ainda | Testes escritos |
| 3C.2 | Extrair `formatWhatsAppMessage()` de OrderForm | GREEN: testes passam, lógica isolada | Utility funciona |
| 3C.3 | OrderForm usa a utility extraída | Refator: componente menor | Testes existentes passam |

**Estimativa:** 1 prompt

#### Batch 3D — OrderForm: testes de validação faltantes

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 3D.1 | Teste validação telefone (10-11 dígitos) | Regex no source sem teste unitário | Teste passa |
| 3D.2 | Teste validação CEP (8 dígitos) | Regex sem teste unitário | Teste passa |
| 3D.3 | Teste validação data (DD/MM/AAAA) | Formato sem teste unitário | Teste passa |
| 3D.4 | Teste `name.length > 200` | Boundary untested | Teste passa |
| 3D.5 | Teste estado de erro do submit (`submitStatus === "error"`) | Path de erro nunca testado | Teste passa |

**Estimativa:** 1 prompt

#### Batch 3E — Home route + Gallery fix

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 3E.1 | Criar `app/routes/home.test.tsx` — meta tags, composição | Route sem cobertura | Testes passam |
| 3E.2 | Gallery: `id` estático → `React.useId()` | Evitar colisão de IDs | Testes Gallery passam |
| 3E.3 | JSDoc no home.tsx e Gallery atualizado | Contexto | Docs presentes |

**Estimativa:** 1 prompt

---

### Batch 4: Playwright E2E

#### Batch 4A — E2E P0: Happy path + validação

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 4A.1 | Teste: carrega página, formulário visível | Smoke test | `npx playwright test` passa |
| 4A.2 | Teste: preenche form → submit → WhatsApp redirect | Fluxo principal | Teste passa |
| 4A.3 | Teste: submit vazio → erros visíveis → corrigir → erros somem | Validação UX | Teste passa |

**Estimativa:** 1 prompt

#### Batch 4B — E2E P0: Keyboard + a11y

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 4B.1 | Teste: Tab through page inteira (skip→nav→CTA→gallery→form) | a11y P0 | Teste passa |
| 4B.2 | Teste: axe scan em 3 viewports (375px, 768px, 1280px) | a11y audit automatizado | Teste passa |
| 4B.3 | Teste: skip link Tab→Enter→focus no #main | a11y core | Teste passa |

**Estimativa:** 1 prompt

#### Batch 4C — E2E P1-P2: Secundários

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 4C.1 | Teste: CTA "Fazer Encomenda" scroll → #order-form visível | UX flow | Teste passa |
| 4C.2 | Teste: Mobile responsive — gallery 1-coluna em 375px | Responsividade | Teste passa |
| 4C.3 | Teste: Meta tags SEO (title, og:title) | SEO | Teste passa |
| 4C.4 | Teste: Instagram link attrs (target, rel) | Segurança | Teste passa |

**Estimativa:** 1 prompt

---

### Batch 5: Fechamento

#### Batch 5A — CI + documentação final

| Task | O que | Por quê | Verificação |
| ------ | ------- | --------- | ------------- |
| 5A.1 | Script `test:ci` que roda tudo: vitest + playwright + backend | Pipeline automatizado | Script funciona |
| 5A.2 | Atualizar TESTING_STRATEGY.md com ferramentas instaladas | Doc reflete realidade | Doc atualizado |
| 5A.3 | Atualizar DEV_JOURNAL.md com registro do refactoring | Rastreabilidade | Doc atualizado |
| 5A.4 | Snapshot de cobertura — registrar baseline | Baseline mensurável | Números documentados |

**Estimativa:** 1 prompt

---

## 4. Decisões Técnicas e Justificativas

### D1: Remover Lombok ao invés de adotá-lo

**Decisão:** Remover `lombok` do `pom.xml`.

**Justificativa:**

- Apenas 1 entity (`Order.java`) no projeto inteiro
- Getters/setters já foram escritos manualmente
- Lombok adiciona complexidade de build (annotation processing) sem ROI
- Para 1 entity, Java records ou getters manuais são suficientes
- Menos "magia" = mais fácil de entender para contribuidores novos

**Alternativa considerada:** Converter `Order.java` para usar `@Data`. Rejeitada porque
entities JPA com `@Data` geram `equals()`/`hashCode()` que podem causar problemas com
lazy loading e proxies Hibernate.

### D2: Extrair rate limiting para Filter/Interceptor

**Decisão:** Mover lógica Bucket4j do `OrderController` para um `RateLimitingFilter`.

**Justificativa:**

- **SRP**: Controller não deve gerenciar buckets de rate limiting
- **Testabilidade**: filter pode ser testado isoladamente com MockMvc
- **Reuso**: se futuros controllers precisarem de rate limiting, é plug-and-play
- **Separação de concerns**: HTTP concern (rate limiting) vs business concern (criar order)

### D3: Decompor OrderForm em hook + utility

**Decisão:** Extrair `useOrderFormValidation()` e `formatWhatsAppMessage()`.

**Justificativa:**

- OrderForm tem 431 linhas — viola SRP
- Validação pura (sem UI) é perfeitamente testável como hook isolado
- Formatação de mensagem WhatsApp é lógica pura (string manipulation)
- Componente resultante foca apenas em renderização e orquestração
- Testes unitários ficam mais rápidos (sem render) para lógica extraída

### D4: Playwright com Chromium + Firefox + Mobile Chrome

**Decisão:** 3 projetos de browser, sem Safari/WebKit.

**Justificativa:**

- WebKit no Playwright requer dependências de sistema específicas
- Safari real requer macOS — em Linux (OS do dev), WebKit é simulação
- Chromium + Firefox cobrem ~85% do mercado brasileiro
- Mobile Chrome (viewport 375px) cobre mobile que é o caso de uso principal
- Pragmático: cobrir o que importa, sem CI frágil

### D5: MSW para mocks de API ao invés de mocks manuais

**Decisão:** Instalar MSW para interceptar requests de rede.

**Justificativa:**

- Documentado no TESTING_STRATEGY.md como planejado
- Intercepta no nível de rede (não no nível de módulo) — mais realista
- Quando backend estiver integrado no frontend, testes de integração são triviais
- Padrão da indústria para React Testing Library

### D6: Fatiamento em ~18 batches de 1 prompt cada

**Decisão:** Dividir todo o trabalho em batches pequenos e independentes.

**Justificativa:**

- Qualidade > velocidade (diretriz da Cristina)
- Cada batch é verificável (testes passam/falham)
- Se um batch falhar, não contamina os outros
- Permite revisão entre batches
- Mantém contexto gerenciável por prompt (não sobrecarrega)
- Batches originalmente sobrecarregados (1C, 2D) foram fatiados para manter foco único

### D7: JSDoc como documentação de contexto para IA

**Decisão:** Adicionar JSDoc com tags custom (`@decision`, `@a11y`) em todo código produzido.

**Justificativa:**

- LLMs usam JSDoc como contexto principal para entender intenção do código
- Tags custom como `@decision` registram "por quê" direto no código
- `@a11y` no JSDoc mantém acessibilidade como first-class concern visível
- Reduz dependência de docs externos — o código se auto-documenta
- Em um projeto onde IA assiste o desenvolvimento, é investimento de ROI alto

### D8: Dockerfile deve acompanhar versão Java do pom.xml

**Decisão:** Atualizar `backend/Dockerfile` de `eclipse-temurin:17-*` para `21-*` sempre que Java for upgradado.

**Justificativa:**

- Dockerfile estava referenciando Java 17 após upgrade do pom.xml para Java 21
- Divergência entre build local (Java 21) e container (Java 17) causa bugs sutis
- Regra: `pom.xml java.version` e `Dockerfile FROM` devem ser sempre sincronizados

### D9: Testes não devem usar datas absolutas hardcoded

**Decisão:** Substituir `LocalDate.of(2025, 3, 15)` por `LocalDate.now().plusMonths(6)` em testes.

**Justificativa:**

- `@Future` validation rejeita datas no passado — testes quebraram durante upgrade Java 21
  porque `2025-03-15` já havia passado em fevereiro de 2026

- Datas relativas (`now().plusMonths(n)`) nunca expiram
- Evita flakiness temporal que só aparece meses depois de escrever o teste

---

## 5. Glossário de Tags JSDoc Custom

| Tag | Uso | Exemplo |
| ----- | ----- | --------- |
| `@a11y` | Considerações de acessibilidade | `@a11y Keyboard: Enter/Space trigger submit` |
| `@decision` | Justificativa de decisão técnica | `@decision Uses newErrors instead of state to avoid stale closure` |
| `@see` | Referência cruzada | `@see OrderService.createOrder for business logic` |
| `@todo` | Trabalho pendente | `@todo Replace placeholder WhatsApp number` |
| `@bug` | Bug conhecido documentado | `@bug Focus logic uses stale state — fix in Batch 1B` |

---

## 6. Ordem de Execução Recomendada

```text
Batch 0A → 0B → 0C → 0D    (Infraestrutura — sequencial, cada um depende do anterior)
      ↓
Batch 1A → 1B → 1C → 1D → 1E (Bug fixes — 1A/1B/1C paralelos; 1D/1E independentes)
      ↓
Batch 2A → 2B → 2C → 2D → 2E (Backend TDD — sequencial, 2D-2E dependem de 2A-2C)
      ↓
Batch 3A ──────────────────  (Welcome — independente)
Batch 3B → 3C               (OrderForm decomposição — sequencial)
Batch 3D                     (OrderForm validações — independente, mas após 3B)
Batch 3E                     (Home route + Gallery — independente)
      ↓
Batch 4A → 4B → 4C          (E2E — sequencial, depende de Batch 0C)
      ↓
Batch 5A                     (Fechamento — final)
```

---

> **Execution tracking:** See [TDD_REFACTORING_LOG.md](TDD_REFACTORING_LOG.md)
