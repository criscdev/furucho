# TDD Refactoring — Log de Execução

> Registro de progresso da refatoração TDD do projeto Furucho.
> Plano completo: [TDD_REFACTORING_PLAN.md](TDD_REFACTORING_PLAN.md)

---

## Checklist de Progresso

- [x] **Batch 0A** — Corrigir infra existente (@ts-nocheck, vitest config, index.html)
- [x] **Batch 0B** — Configurar cobertura (@vitest/coverage-v8)
- [ ] **Batch 0C** — Instalar e configurar Playwright
- [ ] **Batch 0D** — Instalar e configurar MSW
- [ ] **Batch 1A** — Fix `lang="en"` + testes root.tsx
- [ ] **Batch 1B** — Fix focus OrderForm + teste
- [ ] **Batch 1C** — Fix CORS duplicado
- [ ] **Batch 1D** — Limpeza backend: Lombok + SuppressWarnings + Dockerfile Java 21
- [ ] **Batch 1E** — Fix datas hardcoded em testes backend
- [ ] **Batch 2A** — OrderService: createOrder + normalizações
- [ ] **Batch 2B** — OrderService: queries e update
- [ ] **Batch 2C** — OrderController: endpoints faltantes
- [ ] **Batch 2D** — Rate limiting: testes + extração
- [ ] **Batch 2E** — Integração backend + Repository
- [ ] **Batch 3A** — Welcome component: testes
- [ ] **Batch 3B** — Decompor OrderForm: hook de validação
- [ ] **Batch 3C** — Decompor OrderForm: utility WhatsApp
- [ ] **Batch 3D** — OrderForm: validações faltantes
- [ ] **Batch 3E** — Home route + Gallery useId
- [ ] **Batch 4A** — E2E P0: Happy path + validação
- [ ] **Batch 4B** — E2E P0: Keyboard + a11y
- [ ] **Batch 4C** — E2E P1-P2: Secundários
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
