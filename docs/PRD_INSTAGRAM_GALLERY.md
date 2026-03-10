# PRD — Galeria Instagram

**Versão:** 1.0  
**Data:** 2026-03-03  
**Status:** Aprovado  
**Autor:** Tech Lead Review  

---

## 1. Problema

O site da Roberta Furucho exibe 6 placeholders (`placehold.co`) na seção Gallery onde deveriam estar fotos reais dos trabalhos artesanais. A Roberta já publica regularmente no Instagram (@robertafurucho1), mas precisaria manualmente copiar cada foto para o site. Isso não escala, gera trabalho duplicado, e o site fica desatualizado.

**Dor do usuário:** Visitantes não veem trabalhos reais — apenas retângulos coloridos com texto. Não há prova social, não há portfólio demonstrável, não há razão para confiar na qualidade do trabalho.

**Dor da Roberta:** Precisa manter dois canais em sincronia manualmente. Na prática, só mantém o Instagram.

---

## 2. Solução

Integração direta com a **Instagram Graph API v25.0** (oficial Meta). O backend busca automaticamente as publicações do @robertafurucho1 a cada 15 minutos e as persiste em banco. O frontend consome esses dados via API REST própria, com SSR para SEO.

**Resultado:** Roberta posta no Instagram → aparece no site automaticamente. Zero intervenção manual.

---

## 3. Objetivos & Métricas de Sucesso

| Objetivo | Métrica | Meta |
| --- | --- | --- |
| Galeria com conteúdo real | Nº de placeholders visíveis | 0 |
| Atualização automática | Tempo entre post no IG e aparição no site | ≤ 15 min |
| SEO da galeria | Imagens indexáveis no primeiro HTML (SSR) | 100% dos posts |
| Disponibilidade | Site funciona se a API do Instagram cair | Sim (graceful degradation) |
| Qualidade | Testes passando após implementação | 100% |
| Acessibilidade | Violations axe na rota /galeria | 0 |

---

## 4. Público-Alvo

| Persona | Necessidade |
| --- | --- |
| **Visitante** | Ver portfólio real de bonecas antes de encomendar |
| **Roberta (dona)** | Publicar uma vez (Instagram) e ter o site atualizado automaticamente |
| **Google/SEO** | Indexar imagens reais com alt text descritivo |

---

## 5. Escopo

### 5.1 Incluído ✅

- Consumo automático de publicações (fotos e carrosséis) via Instagram Graph API
- Novo endpoint REST `GET /api/instagram/feed` com paginação
- Novo endpoint REST `GET /api/instagram/feed/latest?count=6` para teaser na home
- Nova rota `/galeria` com galeria paginada e SSR via `loader`
- Teaser na home: Gallery exibe os 6 posts mais recentes em vez de placeholders
- Link "Ver galeria completa →" da home para `/galeria`
- Link "Galeria" no Header (navegação principal)
- Refresh automático do token de longa duração (60 dias) a cada ~15 dias
- Graceful degradation: home mostra placeholders se backend indisponível
- Testes unitários, integração, E2E e acessibilidade

### 5.2 Excluído ❌

- Vídeos e Reels (apenas IMAGE e CAROUSEL_ALBUM)
- Stories do Instagram
- Métricas de engajamento no site (likes, comments) — simplifica UI
- Autenticação nos endpoints da galeria (são públicos)
- Carousel inline interativo no card (usa badge "1/N" + link pro Instagram)
- Download/proxy das imagens para CDN própria (usa `media_url` direto no `<img>`)
- Webhook de novo post (não existe na API do Instagram)

### 5.3 Pré-requisitos (manuais, fora do código)

1. Conta @robertafurucho1 convertida para Business ou Creator
2. Meta Developer App criado com produto "Instagram API with Instagram Login"
3. Permissão `instagram_business_basic` (Standard Access — sem App Review para conta própria)
4. Token de longa duração gerado e anotado como variável de ambiente

---

## 6. Requisitos Funcionais

### RF-01 — Sincronização automática do feed

| Aspecto | Detalhe |
| --- | --- |
| **Trigger** | `@Scheduled` a cada 15 minutos |
| **Fonte** | `GET /v25.0/{userId}/media?fields=id,media_type,media_url,caption,timestamp,permalink,children{id}&limit=50` |
| **Filtro** | `media_type` IN (IMAGE, CAROUSEL_ALBUM) — exclui VIDEO |
| **Carrosséis** | `childCount` = `children.data.length` (máx 10 items por carrossel, sem paginação) |
| **Upsert** | Se `igId` já existe → atualiza `mediaUrl` + `caption` (CDN URLs expiram); se novo → insere |
| **Sync incremental** | Segue `paging.next` até encontrar `igId` já conhecido, depois para |
| **Sync inicial** | Pagina até o fim (até 10K posts mais recentes) |
| **Resiliência** | Try/catch total — loga erro, nunca joga exception, não mata o scheduler |
| **Config** | Desabilitado por padrão (`instagram.sync-enabled=false`); habilitado em prod |

### RF-02 — Refresh automático de token

| Aspecto | Detalhe |
| --- | --- |
| **Frequência** | Cron a cada dia 1 e 15 do mês (`0 0 3 1,15 * *`) |
| **Mecanismo** | `GET /refresh_access_token?grant_type=ig_refresh_token&access_token={token}` |
| **Armazenamento** | `AtomicReference<String>` em memória (sobrevive durante o uptime) |
| **Fallback** | Se refresh falhar → loga WARNING. Token original de env var continua válido até expirar (60 dias). Re-deploy manual necessário se expirar |

### RF-03 — API REST da galeria

| Endpoint | Método | Response | Descrição |
| --- | --- | --- | --- |
| `/api/instagram/feed?page=0&size=12` | GET | `Page<InstagramPostResponse>` | Feed paginado, ordenado por timestamp DESC |
| `/api/instagram/feed/latest?count=6` | GET | `List<InstagramPostResponse>` | N posts mais recentes (teaser home) |

`InstagramPostResponse`: `igId`, `mediaType`, `mediaUrl`, `caption`, `timestamp` (ISO string), `permalink`, `childCount`

### RF-04 — Rota `/galeria` com SSR

| Aspecto | Detalhe |
| --- | --- |
| **Loader** | Server-side `fetch` ao backend `/api/instagram/feed?page=0&size=12` |
| **Fallback** | Se backend não responder → `posts: []`, exibe mensagem amigável com link pro Instagram |
| **Paginação** | Botão "Carregar mais" usa `useFetcher` do React Router para buscar próxima página |
| **SEO** | `meta()` com título, og:title, og:description |
| **Layout** | `<Header />` + `<InstagramGallery posts={data} />` |

### RF-05 — Teaser na home

| Aspecto | Detalhe |
| --- | --- |
| **Loader** | Server-side `fetch` ao backend `/api/instagram/feed/latest?count=6` |
| **Gallery** | Recebe `instagramPosts` prop; se presente, usa no lugar dos placeholders |
| **Graceful degradation** | Se fetch falhar → `instagramPosts = undefined` → Gallery mostra placeholders atuais |
| **CTA** | Link "Ver galeria completa →" apontando para `/galeria` |

### RF-06 — Navegação atualizada

| Aspecto | Detalhe |
| --- | --- |
| **Header** | Novo link "Galeria" na nav usando `<NavLink to="/galeria">` |
| **Active state** | `NavLink` fornece `aria-current="page"` e classe `.active` automaticamente |
| **Ordem** | Início · Galeria · Encomendas · Instagram |

---

## 7. Requisitos Não-Funcionais

### RNF-01 — Acessibilidade (WCAG 2.2 AA)

- `<section aria-labelledby>` na galeria
- `<ul role="list">` com `<li>` semânticos (padrão Gallery existente)
- `alt` do `<img>` = caption truncado 125 chars, fallback "Foto de boneca de biscuit artesanal por Roberta Furucho"
- Badge carrossel: `<span aria-label="Carrossel com N fotos">`
- Loading: `aria-busy="true"` no grid + `aria-live="polite"` para novos posts
- Botão "Carregar mais" com text descritivo, `disabled` quando loading
- Links "Ver no Instagram" com `target="_blank" rel="noopener noreferrer"` + text externo visível
- `jest-axe` em todo teste unitário + `AxeBuilder` nos 3 viewports E2E
- Focus ring `var(--color-focus)` em todos os interativos

### RNF-02 — Performance

- `<img loading="lazy" decoding="async">` em todos os cards
- SSR: primeiro HTML já contém `<img src>` reais → LCP com conteúdo, não com skeleton
- Paginação: carrega 12 posts por vez, não o feed inteiro
- Sync backend: não bloqueia requests — `@Scheduled` roda em thread separado

### RNF-03 — Segurança

- Token do Instagram **nunca** exposto ao frontend (fica apenas no backend como env var)
- Endpoints `/api/instagram/*` são apenas GET (read-only) — sem risco de mutação
- CORS já configurado para `/api/**` em `WebConfig` — cobre automaticamente

### RNF-04 — Resiliência

- Backend off → home mostra placeholders, /galeria mostra fallback com link Instagram
- API Instagram off → sync falha silenciosamente, dados cacheados no banco continuam servindo
- Token expirado → sync para de atualizar, dados existentes continuam servindo

### RNF-05 — Testabilidade

- `instagram.sync-enabled=false` por padrão → testes backend não precisam de env vars do IG
- Componente `InstagramGallery` recebe props puras → testável sem MSW
- Factory `instagramPostFactory()` para dados de teste (padrão `orderFactory`)

---

## 8. Arquitetura

```text
┌──────────────┐    @Scheduled 15min     ┌────────────────────┐
│              │ ──────────────────────►  │   Instagram        │
│  Spring Boot │    GET /v25.0/media      │   Graph API v25.0  │
│              │ ◄──────────────────────  │                    │
│  ┌────────┐  │                          └────────────────────┘
│  │ Sync   │  │
│  │Service │──┼──► H2/PostgreSQL (instagram_posts)
│  └────────┘  │
│  ┌────────┐  │    GET /api/instagram/feed
│  │ Feed   │◄─┼──────────────────────────────┐
│  │Ctrl    │──┼──► JSON response              │
│  └────────┘  │                               │
└──────────────┘                               │
                                               │
┌──────────────────────────────────────┐       │
│  React Router 7 (SSR)                │       │
│                                      │       │
│  loader() ───── fetch() ─────────────┼───────┘
│       │                              │
│       ▼                              │
│  ┌──────────────────┐                │
│  │ InstagramGallery  │               │
│  │ (props puras)     │               │
│  └──────────────────┘                │
│                                      │
│  useFetcher() ── "Carregar mais" ────┼───────► GET /api/instagram/feed?page=N
│                                      │
└──────────────────────────────────────┘
```

### Modelo de dados

```sql
CREATE TABLE instagram_posts (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    ig_id       VARCHAR(64)   NOT NULL UNIQUE,
    media_type  VARCHAR(20)   NOT NULL,  -- IMAGE | CAROUSEL_ALBUM
    media_url   VARCHAR(2048) NOT NULL,
    caption     VARCHAR(2200),
    timestamp   TIMESTAMP WITH TIME ZONE NOT NULL,
    permalink   VARCHAR(512)  NOT NULL,
    child_count INT DEFAULT 1 NOT NULL
);

CREATE INDEX idx_instagram_posts_timestamp ON instagram_posts(timestamp DESC);
```

---

## 9. Estrutura de Arquivos (novos/modificados)

```text
backend/src/main/java/com/robertafurucho/
  instagram/
    InstagramMediaType.java          ← enum
    InstagramPost.java               ← entity
    InstagramPostResponse.java       ← record DTO
    InstagramRepository.java         ← Spring Data JPA
    InstagramSyncService.java        ← @Scheduled sync + token refresh
    InstagramFeedService.java        ← read-only queries
    InstagramFeedController.java     ← REST endpoints
    InstagramProperties.java         ← @ConfigurationProperties

backend/src/test/java/com/robertafurucho/
  instagram/
    InstagramSyncServiceTest.java    ← mock RestClient + Repository
    InstagramFeedServiceTest.java    ← mock Repository
    InstagramFeedControllerTest.java ← @WebMvcTest
    InstagramRepositoryTest.java     ← @DataJpaTest
    InstagramIntegrationTest.java    ← @SpringBootTest full stack

src/component/InstagramGallery/
  InstagramGallery.tsx               ← componente puro
  InstagramGallery.test.tsx          ← vitest + RTL + axe

app/routes/
  galeria.tsx                        ← rota com loader SSR

src/test/factories/
  instagramPostFactory.ts            ← factory de dados de teste

e2e/
  galeria.spec.ts                    ← Playwright E2E

Modificados:
  app/routes.ts                      ← + route("galeria")
  app/routes/home.tsx                ← + loader para teaser
  src/component/Header/Header.tsx    ← + NavLink "Galeria"
  src/component/Gallery/Gallery.tsx  ← + instagramPosts prop + CTA
  src/mocks/handlers.ts              ← + instagram handlers
  backend/src/main/resources/application.properties    ← + instagram.*
  backend/src/main/resources/application-prod.properties ← + sync-enabled=true
```

---

## 10. Plano de Testes

| Camada | Arquivo | Foco | Qt. estimada |
| --- | --- | --- | --- |
| Backend Unit | `InstagramSyncServiceTest` | Sync, upsert, filter, token refresh | ~10 |
| Backend Unit | `InstagramFeedServiceTest` | Conversão DTO, defaults | ~4 |
| Backend Unit | `InstagramFeedControllerTest` | HTTP, paginação, JSON shape | ~6 |
| Backend Unit | `InstagramRepositoryTest` | Queries, unique, ordering | ~4 |
| Backend Integration | `InstagramIntegrationTest` | Lifecycle GET completo | ~3 |
| Frontend Unit | `InstagramGallery.test.tsx` | Grid, caption, badge, load more, empty, axe | ~10 |
| Frontend Unit | `galeria.test.tsx` | Rota + loader + meta | ~4 |
| Frontend Updates | `Header`, `Gallery`, `home` tests | NavLink, instagramPosts prop | ~6 |
| E2E | `galeria.spec.ts` | Grid, nav, axe 3 viewports | ~5 |
| **Total** | | | **~52** |

**Pós-implementação:** ~69 backend + ~110 frontend + ~47 E2E = **~226 testes totais**

---

## 11. Riscos & Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
| --- | --- | --- | --- |
| Token expira sem refresh funcionar | Média | Alto | Loga WARNING; dados cacheados continuam servindo; alerta manual |
| Meta depreca API v25.0 | Baixa | Médio | Versionamento no URL fácil de trocar; Meta avisa com ~1 ano de antecedência |
| CDN URLs do Instagram expiram | Alta | Baixo | Sync a cada 15min atualiza `mediaUrl`; se expirar entre syncs, `<img>` mostra bg lavender |
| Conta perde status Business | Baixa | Alto | Galeria para de sincronizar; dados existentes continuam servindo |
| Rate limit da Graph API | Muito baixa | Baixo | 4800 calls/24h; polling 15min = 96 calls/dia = 2% do limite |

---

## 12. Configuração de Ambiente

```bash
# Variáveis de ambiente (backend)
IG_USER_ID=1234567890          # ID numérico do usuário Instagram
IG_ACCESS_TOKEN=IGQVJx...      # Token de longa duração (60 dias)
IG_SYNC_ENABLED=true           # true em produção, false em dev/test
```

---

## 13. Definição de Pronto (DoD)

- [ ] Todos os arquivos da Seção 9 implementados
- [ ] ~52 novos testes passando (seção 10)
- [ ] 0 testes existentes quebrados (144 atuais)
- [ ] `npx vitest run` → 100% pass
- [ ] `./mvnw test` → BUILD SUCCESS
- [ ] `npx playwright test` → 100% pass
- [ ] `curl /api/instagram/feed/latest?count=6` com backend + env vars → JSON com posts reais
- [ ] `view-source:localhost:5173/galeria` → HTML contém `<img src="https://scontent...">` (SSR)
- [ ] Home Gallery mostra fotos reais do Instagram
- [ ] 0 violations axe em todos os viewports
- [ ] Graceful degradation verificada (backend off → home OK com placeholders)
- [ ] Commit + push na branch `tdd-refactoring`
