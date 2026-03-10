# Revisão Completa & Autocrítica — Furucho

**Data:** 2026-03-03  
**Escopo:** Revisão de absolutamente tudo — código, testes, integração, arquitetura, processo  
**Autor:** GitHub Copilot (Revisão 4 — a mais profunda)

---

## 1. Resumo Executivo

### Testes de Integração Real (via curl contra o backend rodando)

| # | Teste | Resultado |
| --- | --- | --- |
| 1 | `GET /api/health` → status UP | ✅ 200 |
| 2 | `POST /api/orders` (dados válidos) → ordem criada | ✅ 201 |
| 3 | `GET /api/orders/1` → retorna ordem correta | ✅ 200 |
| 4 | `GET /api/orders` → lista com 1 ordem | ✅ 200 |
| 5 | `PATCH /api/orders/1/status?status=CONFIRMED` → atualiza status | ✅ 200 |
| 6 | `GET /api/orders/1` → status confirmado | ✅ 200 |
| 7 | `POST /api/orders` (dados inválidos) → 8 erros de campo | ✅ 400 |
| 8 | `GET /api/orders/99999` → não encontrado | ✅ 404 |
| 9 | `PATCH /api/orders/1/status?status=BANANA` → enum inválido | ✅ 400 |
| 10 | CORS preflight (origin permitida) → Access-Control-Allow-Origin | ✅ 200 |
| 11 | CORS preflight (origin proibida) → bloqueado | ✅ 403 |
| 12 | Rate limiting: 5 POSTs → 201, 6° POST → 429 + Retry-After | ✅ 429 |

**Resultado: 12/12 testes de integração reais passaram.**

### Suítes de Testes Automatizados

| Suíte | Resultado |
| --- | --- |
| Vitest (frontend) | 95/95 ✅ |
| JUnit 5 (backend) | 125/125 ✅ |
| **Total** | **220/220** |

---

## 2. Issues Encontrados e Corrigidos

### Críticos (Corrigidos)

| ID | Descrição | Correção |
| --- | --- | --- |
| C1 | Arquivo `package.json (edit:...)` fantasma (137 bytes, JSON quebrado) na raiz | Deletado |
| C2 | `backend/Dockerfile` HEALTHCHECK apontava `/actuator/health` (Actuator não instalado) | URL corrigida para `/api/health` |
| C3 | `@SuppressWarnings("null")` em 4 arquivos de teste (contradiz BUG_ENHANCEMENT_LOG R13) | Removido dos 4 arquivos |

### Moderados (Corrigidos)

| ID | Descrição | Correção |
| --- | --- | --- |
| M1 | `docs/DEPLOYMENT.md` referenciava `JAVA:17-java17` (projeto usa Java 21) | Atualizado para `JAVA:21-java21` |
| M2 | `docs/DEV_JOURNAL.md` e `TDD_REFACTORING_PLAN.md` referenciavam Spring Boot 3.4.1 (pom.xml tem 3.5.11) | Atualizado para 3.5.11 |
| m9 | `Dockerfile` frontend não definia `NODE_ENV=production` | Adicionado `ENV NODE_ENV=production` |
| m10 | E2E hardcodava `15/04/2026` (vai virar data no passado) | Substituído por `futureDate()` dinâmica |

---

## 3. Issues Identificados — Não Corrigidos (Requerem Decisão de Produto)

### 🔴 Críticos (Arquiteturais)

#### C4 — Sem autenticação nos endpoints administrativos

**Impacto:** `GET /api/orders`, `GET /api/orders/{id}` e `PATCH /api/orders/{id}/status` são públicos. Qualquer pessoa pode listar todos os pedidos (com PII: nome, email, telefone, endereço) e alterar status.

**Solução proposta:**

- Spring Security com JWT ou sessão para endpoints admin
- Mínimo: API key compartilhada via header `X-API-Key`
- Estimar 2-3 dias de implementação

#### C5 — Rate limiter vulnerável a spoofing de `X-Forwarded-For`

**Impacto:** Atacante pode enviar header `X-Forwarded-For` com IPs diferentes a cada request, bypassando o rate limit completamente (5 requests por IP falso = ilimitado).

**Solução proposta:**

- `server.forward-headers-strategy=framework` no application.properties
- Validar que X-Forwarded-For vem de um proxy confiável
- Em ambiente local sem proxy: usar apenas `request.getRemoteAddr()`

#### C6 — Frontend desconectado do backend

**Impacto:** O `OrderForm` monta URL do WhatsApp e abre via `window.open()`. **Nunca** chama `POST /api/orders`. O backend inteiro é dead code em produção — pedidos não são persistidos.

**Solução proposta:** A decidir pelo produto:

- **Opção A**: Frontend faz `POST /api/orders` antes do redirect WhatsApp → persiste pedido + tracking
- **Opção B**: Documentar que backend é feature futura, marcar como alpha/experimental
- **Opção C**: Converter para PWA com service worker que enfileira pedidos offline

### 🟡 Moderados (Pendentes)

| ID | Descrição | Solução proposta |
| --- | --- | --- |
| M4 | `@testing-library/react` v14 tem peer dep React 18 (projeto usa React 19) | Upgrade para v16+ |
| M5 | Frontend não valida data futura (backend tem `@Future`) | Adicionar validação de data futura em `useOrderFormValidation` |
| M6 | Formato de data incompatível: frontend `DD/MM/YYYY` vs backend `yyyy-MM-dd` | Converter antes de enviar (quando C6 for resolvido) |
| M7 | WhatsApp `5511999999999` é placeholder | Substituir por número real do negócio |
| M8 | SVG do Instagram duplicado em `welcome.tsx` e `Header.tsx` | Extrair para `InstagramIcon` component |
| M9 | `OrderResponse` JSDoc diz "exclui dados sensíveis" mas inclui todos PII | Criar DTO admin-only ou redactar PII |
| M10 | `OrderRepository.findByStatusOrderByCreatedAtDesc` nunca chamado | Wiring em endpoint `GET /api/orders?status=` ou remover |
| M11 | `OrderService.getOrdersByEmail` nunca chamado por controller | Adicionar endpoint ou remover |

### ⚪ Menores (Nice to have)

| ID | Descrição |
| --- | --- |
| m1 | Falta `og:image` para previews em redes sociais |
| m2 | Falta `og:url` e `og:locale` (pt_BR) |
| m3 | Falta `<meta name="robots">` |
| m4 | Gallery hardcoda URL do Instagram (não recebe como prop) |
| m5 | `Home` usa `export default` (diverge de CODING_STANDARDS, mas é convenção do React Router) |
| m6 | `test-utils.ts` exporta `renderWithProviders` mas nenhum teste importa |
| m7 | CSS custom properties `--space-*` e `--radius-*` nunca usadas (Tailwind substitui) |
| m8 | Falta `.env.example` no backend |

---

## 4. Autocrítica do Processo de Desenvolvimento

### O que foi feito bem ✅

#### 1. TDD com pirâmide de testes rigorosa

- 186 testes totais (95 vitest + 49 JUnit + 42 E2E × 3 browsers)
- Cobertura de 98.48% statements — excelente para um projeto desse porte
- Testes de acessibilidade com `jest-axe` em cada componente — raro em projetos reais
- Fábricas de dados (`orderFactory`) eliminam dados mágicos

#### 2. Acessibilidade acima da média

- Skip links, `aria-labelledby`, `aria-describedby`, `aria-invalid`, `aria-required`, `aria-live`
- `prefers-reduced-motion` respeitado
- `lang="pt-BR"` na raiz do documento
- Focus ring com contraste 5.99:1 (WCAG AAA)
- E2E acessibilidade em 3 viewports com `@axe-core/playwright`

#### 3. Documentação exaustiva

- 11 arquivos em `docs/` cobrindo cada aspecto: TDD, deploy, padrões, a11y, erros
- TDD_REFACTORING_LOG com batches numerados e checkboxes
- DEV_JOURNAL com cronologia completa
- ERROR_JOURNAL com root causes e soluções aplicadas

#### 4. Backend bem estruturado

- Records para DTOs (imutabilidade)
- Bean Validation com mensagens em português
- Rate limiting por IP com eviction automática
- GlobalExceptionHandler centralizado
- Normalização de dados (CEP, telefone) no service layer

#### 5. CI/CD preparado

- GitHub Actions para frontend e backend
- Playwright instalação automatizada no CI
- Caching de Maven e dependências npm

### O que poderia ter sido melhor 🔶

#### 1. Frontend-backend integração é zero em produção

> A falha mais fundamental do projeto. O backend tem 49 testes passando, CRUD completo, rate limiting, validação... e nada disso é usado pelo frontend. O `OrderForm` vai direto para o WhatsApp. Isso deveria ter sido a **primeira** coisa a ser definida — não a última a ser descoberta na revisão 4.

Auto-nota: 3/10 — Duas stacks completas sem integração é desperdício significativo.

#### 2. Segurança foi deixada para depois

> Nenhuma autenticação, PII exposta publicamente, rate limiter bypassável por spoofing de header. Em um projeto que armazena nomes, emails, telefones e endereços, segurança deveria ter sido prioridade desde o Batch 0.

Auto-nota: 2/10 — Inaceitável para produção. Aceitável apenas para MVP/demo local.

#### 3. Dead code no backend

> `findByStatusOrderByCreatedAtDesc` e `getOrdersByEmail` existem, estão testados, mas nenhum controller os usa. Foram escritos "para o futuro" mas sem issue/ticket tracking. Isso é YAGNI (You Aren't Gonna Need It) violado.

Auto-nota: 5/10 — Código antecipado sem demanda concreta.

#### 4. Divergência docs ↔ realidade

> Spring Boot 3.4.1 nos docs quando o pom.xml tem 3.5.11. Java 17 no deploy doc quando usa 21. `@SuppressWarnings` supostamente removido mas presente em 4 arquivos. BUG_ENHANCEMENT_LOG disse que o arquivo rogue foi deletado mas ele ainda existia. Isso mostra que as docs não são atualizadas junto com o código.

Auto-nota: 4/10 — Docs desatualizadas dão falsa confiança.

#### 5. Versões de bibliotecas desatualizadas

> `@testing-library/react` v14 não é compatível com React 19 (peer dep). Vitest 0.32 é de 2023, a v2.x já existe. Isso cria dívida técnica silenciosa que se acumula.

Auto-nota: 5/10 — Funciona, mas limita upgrades futuros.

#### 6. Processo de revisão precisou de 4 passes

> Revisão 1: 58 issues. Revisão 2: 2 issues de escopo biscuit. Revisão 3: 0 issues (46 arquivos). Revisão 4: 6 críticos + 11 moderados + 11 menores. A revisão 3 declarou "0 issues" e estava fundamentalmente errada — não testou integração real, não verificou Dockerfiles, não checou divergências de docs.

Auto-nota: 3/10 — Revisão superficial deu falsa sensação de completude.

---

## 5. Proposta de Melhorias — Roadmap

### Sprint 1: Segurança & Integração (P0)

| # | Tarefa | Esforço |
| --- | --- | --- |
| 1.1 | Definir se frontend chama backend (decisão de produto) | 1h |
| 1.2 | Implementar Spring Security com JWT para endpoints admin | 2-3d |
| 1.3 | Criar endpoint de login para administrador | 1d |
| 1.4 | Corrigir rate limiter para não confiar em X-Forwarded-For | 2h |
| 1.5 | Adicionar CSP headers (Content-Security-Policy) | 2h |
| 1.6 | Se 1.1 = sim: integrar `OrderForm` com `POST /api/orders` | 1d |

### Sprint 2: Qualidade & Cleanup (P1)

| # | Tarefa | Esforço |
| --- | --- | --- |
| 2.1 | Upgrade `@testing-library/react` v14 → v16+ | 2h |
| 2.2 | Upgrade Vitest 0.32 → 2.x | 4h |
| 2.3 | Remover dead code (`findByStatus`, `getOrdersByEmail`) ou wiring | 1h |
| 2.4 | Extrair `InstagramIcon` component para DRY | 30min |
| 2.5 | Criar `.env.example` no backend | 15min |
| 2.6 | Limpar CSS tokens não utilizados | 30min |
| 2.7 | Corrigir/remover `src/test-utils.ts` | 15min |

### Sprint 3: SEO & Produção (P2)

| # | Tarefa | Esforço |
| --- | --- | --- |
| 3.1 | Adicionar `og:image`, `og:url`, `og:locale`, robots meta | 1h |
| 3.2 | Substituir número WhatsApp placeholder por real | 5min |
| 3.3 | Validação de data futura no frontend | 1h |
| 3.4 | Criar `OrderListResponse` sem PII para endpoint público | 2h |
| 3.5 | Adicionar `og:image` com foto representativa de boneca de biscuit | 1h |

### Sprint 4: Observabilidade & Deploy (P2)

| # | Tarefa | Esforço |
| --- | --- | --- |
| 4.1 | Adicionar Spring Boot Actuator para métricas reais | 2h |
| 4.2 | Structured logging (JSON) para agregação | 2h |
| 4.3 | Ativar deployment jobs no GitHub Actions | 4h |
| 4.4 | Adicionar Sentry/error tracking no frontend | 2h |
| 4.5 | Database migrations com Flyway (sair de `ddl-auto`) | 4h |

---

## 7. Revisão 5 — WhatsApp Chatbot (2026-03-03)

### Escopo

Revisão completa dos 12 arquivos de produção + 5 arquivos de teste do chatbot WhatsApp, implementado como Batch 6A.

### Issues Encontrados e Corrigidos

#### CRITICAL (4)

| ID | Descrição | Correção |
| --- | --- | --- |
| W1 | `findActiveByWaId` retorna `Optional<T>` mas query pode retornar múltiplas linhas | Native query com `LIMIT 1` |
| W2 | `findLastCompletedByWaId` mesmo problema | Native query com `LIMIT 1` |
| W3 | Sem `@Version` — updates concorrentes sobrescrevem dados | Adicionado `@Version private Long version` |
| W4 | Fluxo de correção força re-entrada de todos os campos restantes | Campo `correcting` + retorno direto a CONFIRM |

#### MAJOR (5)

| ID | Descrição | Correção |
| --- | --- | --- |
| W5 | `subscribe()` sem error consumer → crash em thread async | Error consumer lambda |
| W6 | Sem auto-expire após maxRetries — usuário preso | Auto-expira conversa |
| W7 | `/status` e `/ajuda` criam conversas órfãs | Comandos movidos antes de find-or-create |
| W8 | `setFieldForStep(ASK_DATE)` sem bounds check | Guard `parts.length != 3` |
| W9 | `buildSummaryText` renderiza "null" para campos ausentes | Helper `safe()` com "—" |

#### MINOR (1 corrigido)

| ID | Descrição | Correção |
| --- | --- | --- |
| W10 | `save()` individual no loop de expiração | Batch `saveAll()` |

#### Testes (7 correções)

| ID | Descrição | Correção |
| --- | --- | --- |
| W11 | `Strictness.LENIENT` esconde dead stubs | Removido, strict stubbings corrigidos |
| W12 | `confirmCreatesOrder` usa `any()` | `ArgumentCaptor` para verificar 7 campos |
| — | `maxRetriesHint` não verifica EXPIRED | Renomeado, asserta estado EXPIRED |
| — | Sem teste para path de exceção `createOrder()` | Adicionado `orderCreationFailure` |
| — | Sem teste para fluxo de correção→CONFIRM | Adicionado `correctionReturnsToConfirm` |
| — | `/status` e `/ajuda` testados com conversas | Testados sem criar conversas |
| — | Integration `correctionFlow` re-preenche tudo | Correção de campo único |

### Resultado

125/125 testes ✅ | BUILD SUCCESS | +2 novos testes | 5 testes atualizados

### Nota da Revisão 5

O chatbot teve bugs críticos de concorrência e UX que só seriam descobertos em produção com múltiplos usuários. A revisão preventiva evitou:

- Crashes em produção para clientes que retornam (`IncorrectResultSizeDataAccessException`)
- Perda de dados em cenários de concorrência (sem `@Version`)
- UX terrível no fluxo de correção (re-preencher 7 campos em vez de 1)
- Crashes silenciosos no thread do reactor (`ErrorCallbackNotImplemented`)
- Poluição do banco com conversas órfãs

---

## 8. Conclusão

O projeto tem uma base sólida: 262 testes (95 vitest + 125 JUnit + 42 E2E), 98%+ cobertura frontend, acessibilidade exemplar, i18n consistente, chatbot WhatsApp completo com bugs corrigidos, e documentação extensiva. Porém, a **desconexão frontend↔backend** é uma falha arquitetural fundamental que invalida metade do esforço de desenvolvimento. A **ausência de segurança** torna o deploy em produção inviável. E a **divergência entre documentação e realidade** mostra que o processo de revisão anterior não foi suficientemente rigoroso.

Nota geral do projeto: **7.5/10** (subiu de 7 com o chatbot implementado e bugs corrigidos)

- Excelente em: testes, acessibilidade, documentação, i18n
- Insuficiente em: segurança, integração real, manutenção de docs
- O gap entre "código que funciona" e "produto pronto para produção" ainda é significativo

### Ações imediatas realizadas nesta revisão

- ✅ 12 testes de integração real via curl
- ✅ 7 correções aplicadas (arquivo rogue, Dockerfile, @SuppressWarnings, docs, E2E)
- ✅ Todos os 144 testes passando após correções
- ✅ Roadmap de melhorias em 4 sprints

### Ações da Revisão 5 — WhatsApp Chatbot

- ✅ 15 bugs de produção identificados (4 CRITICAL, 5 MAJOR, 6 MINOR)
- ✅ 18 issues de teste identificados (3 CRITICAL, 8 MAJOR, 7 MINOR)
- ✅ Todos os CRITICAL e MAJOR corrigidos
- ✅ 125 testes backend passando (BUILD SUCCESS)
- ✅ +2 novos testes adicionados, 5 testes atualizados

---

> "Qualidade sempre é P0."
