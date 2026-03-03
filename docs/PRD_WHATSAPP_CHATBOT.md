# PRD — Chatbot WhatsApp de Encomendas

**Versão:** 1.0  
**Data:** 2026-03-03  
**Status:** Proposto  
**Autor:** Tech Lead Review  
**Depende de:** Nenhum (independente da PRD Galeria Instagram)  

---

## 1. Problema

### Fluxo Atual (Broken by Design)

O formulário web (`OrderForm.tsx`) coleta 8 campos do cliente, valida localmente, e abre uma URL `wa.me/` com texto pré-formatado. **O backend nunca é chamado.** O fluxo:

```text
Visitante → preenche form web → clica "Enviar" → abre WhatsApp com texto colado → fim
```

**Problemas identificados (issue C6 do REVIEW_AND_IMPROVEMENTS.md):**

| Problema | Impacto |
| --- | --- |
| Backend é código morto | `POST /api/orders` nunca recebe chamadas em produção |
| Sem persistência | Pedidos existem apenas como mensagens de texto no WhatsApp da Roberta |
| Sem rastreamento | Impossível saber quantos pedidos foram feitos, taxa de conversão, etc. |
| Fluxo unidirecional | Se Roberta quiser tirar dúvidas, precisa copiar dados manualmente |
| UX desconexa | Cliente sai do site, abre WhatsApp, cola texto — popup pode ser bloqueado |
| Sem status | Cliente não tem como saber o andamento do pedido |

### Oportunidade

98% do público-alvo de Roberta (artesanato brasileiro) já usa WhatsApp diariamente. Um chatbot conversacional que guia o pedido passo a passo, valida cada campo, e persiste no banco é a ponte entre o frontend existente e o backend que já está pronto mas nunca é usado.

---

## 2. Solução

**Chatbot WhatsApp** construído sobre a **WhatsApp Cloud API** (oficial Meta) integrado diretamente ao backend Spring Boot existente. O chatbot conduz uma conversa guiada de 8 perguntas (mesmos campos do `OrderForm`), valida cada resposta em tempo real, e ao final cria o pedido via `OrderService.createOrder()`.

### Fluxo Proposto

```text
Cliente envia "Oi" no WhatsApp
  → Backend recebe webhook
  → Inicia conversa guiada (8 perguntas sequenciais)
  → Valida cada resposta (mesmas regras de useOrderFormValidation)
  → Mostra resumo com botões interativos [✓ Confirmar] [✗ Corrigir]
  → Chama OrderService.createOrder()
  → Envia confirmação com nº do pedido
  → Roberta recebe notificação de novo pedido
```

**Resultado:**

- Backend deixa de ser código morto
- Pedidos persistidos em banco com status rastreável
- Cliente faz pedido sem sair do WhatsApp
- Roberta gerencia tudo num lugar só

---

## 3. Objetivos & Métricas de Sucesso

| Objetivo | Métrica | Meta |
| --- | --- | --- |
| Eliminar backend como código morto | Pedidos criados via `POST /api/orders` | > 0/semana |
| Conversão de conversa em pedido | % de conversas iniciadas que resultam em pedido | ≥ 40% |
| Tempo médio do fluxo completo | Tempo entre primeira msg e confirmação | ≤ 5 min |
| Validação efetiva | % de pedidos com dados limpos (sem erros de formato) | 100% |
| Satisfação | Reclamações sobre o processo de pedido | 0 |
| Disponibilidade | Webhook respondendo em < 5s (requisito Meta) | 99.9% |

---

## 4. Público-Alvo

| Persona | Necessidade |
| --- | --- |
| **Cliente** | Fazer pedido de boneca artesanal sem sair do WhatsApp |
| **Roberta (dona)** | Receber pedidos estruturados e rastreáveis, não texto solto |
| **Admin** | Consultar histórico de pedidos, métricas de conversão |

---

## 5. Escopo

### 5.1 Incluído (MVP)

- **RF-01**: Webhook endpoint para receber mensagens do WhatsApp Cloud API
- **RF-02**: Máquina de estados conversacional com 8 passos (mesmos campos do `OrderForm`)
- **RF-03**: Validação campo-a-campo espelhando `useOrderFormValidation.ts`
- **RF-04**: Mensagens interativas (botões / listas) para seleções
- **RF-05**: Resumo do pedido com botões de confirmação/correção
- **RF-06**: Criação do pedido via `OrderService.createOrder()` existente
- **RF-07**: Mensagem de confirmação com número do pedido
- **RF-08**: Template message de boas-vindas (para iniciar conversa proativamente)
- **RF-09**: Template message de atualização de status do pedido
- **RF-10**: Tratamento de timeout (conversa abandonada após 30 min de inatividade)

### 5.2 Excluído (pós-MVP)

- Pagamentos via WhatsApp Pay
- Envio de fotos do progresso da boneca pelo chatbot
- Integração com transportadoras para tracking
- Multi-idioma (apenas português BR)
- Atendimento humano com handoff (Roberta responde manualmente quando necessário)
- IA generativa para perguntas fora do escopo

### 5.3 Pré-requisitos

| Pré-requisito | Responsável | Status |
| --- | --- | --- |
| WhatsApp Business Account | Roberta | Pendente |
| Meta Business Portfolio verificado | Roberta | Pendente |
| Número de telefone dedicado (não pode ser o pessoal) | Roberta | Pendente |
| Meta Developer App com use case "WhatsApp" | Dev | Pendente |
| System User com token permanente | Dev | Pendente |
| Domínio com HTTPS para webhook (já existente) | DevOps | Existente |
| Backend com endpoint público acessível | DevOps | Existente |

---

## 6. Requisitos Funcionais

### RF-01: Webhook Endpoint

**Endpoint:** `POST /api/webhooks/whatsapp`  
**Endpoint de verificação:** `GET /api/webhooks/whatsapp` (challenge handshake)

O webhook recebe todas as notificações do WhatsApp Cloud API. Deve:

1. **GET** — Responder ao challenge de verificação com o `hub.challenge` token
2. **POST** — Processar mensagens recebidas, status updates, erros
3. Responder com `200 OK` em **< 5 segundos** (requisito Meta — caso contrário marca como falha)
4. Processar a mensagem de forma **assíncrona** (aceitar webhook, processar depois)
5. Validar `X-Hub-Signature-256` com o App Secret para evitar spoofing

```java
// Exemplo conceitual
@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {

    @GetMapping
    public ResponseEntity<String> verify(
        @RequestParam("hub.mode") String mode,
        @RequestParam("hub.verify_token") String token,
        @RequestParam("hub.challenge") String challenge) {
        // Valida mode == "subscribe" e token == VERIFY_TOKEN configurado
        return ResponseEntity.ok(challenge);
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String payload,
        @RequestHeader("X-Hub-Signature-256") String signature) {
        // 1. Valida HMAC signature
        // 2. Enfileira para processamento assíncrono
        // 3. Retorna 200 imediatamente
        return ResponseEntity.ok().build();
    }
}
```

**Segurança:**

- HMAC-SHA256 validation obrigatória em todo POST
- Verify token configurável via env var
- Rate limiting separado do existente (o `RateLimitingFilter` atual só cobre `/api/orders` — webhook não é afetado)
- Endpoint NÃO requer autenticação (Meta precisa acessar diretamente)
- CORS não necessário (server-to-server)

---

### RF-02: Máquina de Estados Conversacional

**Entidade:** `ConversationState`

```sql
CREATE TABLE conversation_states (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    wa_id         VARCHAR(20)   NOT NULL,  -- WhatsApp ID do cliente (+5511999...)
    current_step  VARCHAR(30)   NOT NULL DEFAULT 'GREETING',
    name          VARCHAR(200),
    email         VARCHAR(100),
    phone         VARCHAR(15),
    address       VARCHAR(200),
    postal_code   VARCHAR(10),
    order_scope   VARCHAR(100),
    order_scope_detail VARCHAR(800),
    receive_date  DATE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at  TIMESTAMP,
    expired       BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_conversation_wa_id ON conversation_states(wa_id);
CREATE INDEX idx_conversation_step  ON conversation_states(current_step);
```

**Enum `ConversationStep`:**

```text
GREETING          → Mensagem de boas-vindas + pergunta do nome
ASK_NAME          → "Qual seu nome completo?"
ASK_EMAIL         → "Qual seu email?"
ASK_PHONE         → "Qual seu telefone (com DDD)?"
ASK_ADDRESS       → "Qual o endereço de entrega?"
ASK_POSTAL_CODE   → "Qual o CEP?"
ASK_ORDER_SCOPE   → "Que tipo de boneca?" [botões interativos]
ASK_DETAIL        → "Descreva os detalhes (cores, tamanho, tema...)"
ASK_DATE          → "Para quando precisa? (DD/MM/AAAA)"
CONFIRM           → Resumo completo + botões [✓ Confirmar] [✗ Corrigir campo]
CORRECTION        → "Qual campo deseja corrigir?" [lista interativa]
COMPLETED         → Pedido criado, conversa encerrada
EXPIRED           → Timeout 30 min sem resposta
```

**Regras:**

- Uma conversa ativa por `wa_id` (se existir ativa, retoma de onde parou)
- Conversa expira após 30 min de inatividade → marca `expired = true`
- Nova mensagem após expiração inicia conversa nova
- Estado persiste em banco (sobrevive restart do server)

---

### RF-03: Validação Campo-a-Campo

Espelha as regras do frontend (`useOrderFormValidation.ts`) **complementadas** pelas constraints do backend (`CreateOrderRequest.java`):

| Campo | Regex/Regra Frontend | Regra Chatbot (idêntica) |
| --- | --- | --- |
| name | `!trim()` + max 200 | `!isBlank()` + max 200 chars |
| email | `/^[^\s@]+@[^\s@]+\.[^\s@]+$/` | Mesma regex + max 100 chars (backend `@Email` + `@Size(max=100)`) |
| phone | `/^\d{10,11}$/` (após strip não-dígitos) | Mesma regex, strip `\D` antes |
| address | `!trim()` | `!isBlank()` + max 200 chars (backend `@Size(max=200)`) |
| postalCode | `/^\d{5}-?\d{3}$/` | Mesma regex |
| orderScope | `!trim()` | `!isBlank()` + max 100 chars |
| orderScopeDetail | `!trim()` | `!isBlank()` + max 800 chars |
| receiveDate | `/^\d{2}\/\d{2}\/\d{4}$/` + data válida | Mesma regex + parse `LocalDate` + `isFuture()` |

**Comportamento em erro:**

- Mensagem de erro em português (mesmas strings do frontend)
- Re-pergunta o campo sem avançar
- Máximo 3 tentativas por campo → sugere "Digite /cancelar para recomeçar"

**Classe:**

```java
@Component
public class StepValidator {

    public Optional<String> validate(ConversationStep step, String input) {
        return switch (step) {
            case ASK_NAME -> validateName(input);
            case ASK_EMAIL -> validateEmail(input);
            case ASK_PHONE -> validatePhone(input);
            // ... demais campos
            default -> Optional.empty();
        };
    }

    private Optional<String> validateName(String input) {
        if (input == null || input.isBlank()) {
            return Optional.of("Nome é obrigatório");
        }
        if (input.length() > 200) {
            return Optional.of("Nome deve ter no máximo 200 caracteres");
        }
        return Optional.empty(); // válido
    }
    // ... idem para cada campo
}
```

---

### RF-04: Mensagens Interativas

Usa Interactive Messages da WhatsApp Cloud API para campos com opções:

**orderScope (tipo de boneca) — Botões:**

```json
{
  "type": "interactive",
  "interactive": {
    "type": "button",
    "body": { "text": "Que tipo de boneca você gostaria?" },
    "action": {
      "buttons": [
        { "type": "reply", "reply": { "id": "tipo_amigurumi", "title": "Amigurumi" }},
        { "type": "reply", "reply": { "id": "tipo_pano", "title": "Boneca de Pano" }},
        { "type": "reply", "reply": { "id": "tipo_outro", "title": "Outro tipo" }}
      ]
    }
  }
}
```

**Correção — Lista interativa:**

```json
{
  "type": "interactive",
  "interactive": {
    "type": "list",
    "body": { "text": "Qual campo deseja corrigir?" },
    "action": {
      "button": "Selecionar campo",
      "sections": [{
        "title": "Campos",
        "rows": [
          { "id": "fix_name", "title": "Nome", "description": "Atual: Maria Silva" },
          { "id": "fix_email", "title": "Email", "description": "Atual: maria@..." },
          { "id": "fix_phone", "title": "Telefone" },
          { "id": "fix_address", "title": "Endereço" },
          { "id": "fix_cep", "title": "CEP" },
          { "id": "fix_scope", "title": "Tipo de boneca" },
          { "id": "fix_detail", "title": "Detalhes" },
          { "id": "fix_date", "title": "Data de entrega" }
        ]
      }]
    }
  }
}
```

**Confirmação — Botões:**

```json
{
  "type": "interactive",
  "interactive": {
    "type": "button",
    "body": {
      "text": "🧸 *Resumo do Pedido*\n\n*Nome:* Maria...\n*Email:* ...\n...\n\nEstá tudo certo?"
    },
    "action": {
      "buttons": [
        { "type": "reply", "reply": { "id": "confirm_yes", "title": "✓ Confirmar" }},
        { "type": "reply", "reply": { "id": "confirm_edit", "title": "✏ Corrigir" }},
        { "type": "reply", "reply": { "id": "confirm_cancel", "title": "✗ Cancelar" }}
      ]
    }
  }
}
```

---

### RF-05: Resumo do Pedido

Antes de criar o pedido, exibe resumo formatado:

```text
🧸 *Resumo do seu Pedido*

*Nome:* Maria Silva
*Email:* maria@email.com
*Telefone:* (11) 99999-1234
*Endereço:* Rua das Flores, 123 - São Paulo/SP
*CEP:* 01234-567

*Tipo:* Amigurumi
*Detalhes:* Ursinho de crochê, cor azul bebê, 25cm, tema marinheiro
*Data desejada:* 15/04/2026

Está tudo certo? 👇
```

Seguido dos botões interativos [✓ Confirmar] [✏ Corrigir] [✗ Cancelar].

---

### RF-06: Criação do Pedido

Ao confirmar, o chatbot:

1. Constrói um `CreateOrderRequest` a partir do `ConversationState`
2. Chama `OrderService.createOrder(request)` — **a mesma lógica existente**
3. Recebe o `OrderResponse` com o ID do pedido
4. Marca `ConversationState.completedAt` = `now()`
5. Move para step `COMPLETED`

```java
// ConversationService.java
private void handleConfirmation(ConversationState state) {
    // Nota: NÃO normalizar phone aqui — OrderService.createOrder() já normaliza internamente
    CreateOrderRequest request = new CreateOrderRequest(
        state.getName(),
        state.getEmail(),
        state.getPhone(),
        state.getAddress(),
        state.getPostalCode(),
        state.getOrderScope(),
        state.getOrderScopeDetail(),
        state.getReceiveDate()
    );

    OrderResponse order = orderService.createOrder(request);

    state.setCurrentStep(ConversationStep.COMPLETED);
    state.setCompletedAt(LocalDateTime.now());
    conversationRepository.save(state);

    sendConfirmation(state.getWaId(), order);
}
```

**Isso resolve o issue C6** — o backend finalmente recebe pedidos reais.

---

### RF-07: Mensagem de Confirmação

```text
✅ *Pedido Confirmado!*

Seu pedido nº *#{{ orderId }}* foi registrado com sucesso!

A Roberta vai analisar seu pedido e entrar em contato em breve para combinar os detalhes finais.

Obrigada pela confiança! 🧸💕
```

---

### RF-08: Template Message — Boas-Vindas

**Nome:** `welcome_order`  
**Categoria:** Marketing  
**Idioma:** pt_BR  

```text
Olá! 👋 Sou a assistente da Roberta Furucho - Bonecas Artesanais.

Quer encomendar uma boneca exclusiva feita à mão? Eu te ajudo!

É só responder "Quero encomendar" que eu te guio pelo processo. 🧸
```

**Uso:** Enviada proativamente quando Roberta quiser ativar contatos.  
**Custo:** Cobrada como marketing template (~$0.0625 USD para Brasil, jan/2026).

---

### RF-09: Template Message — Atualização de Status

**Nome:** `order_status_update`  
**Categoria:** Utility  
**Idioma:** pt_BR  
**Variáveis:** `{{1}}` = nº pedido, `{{2}}` = novo status

```text
🧸 Atualização do Pedido *#{{1}}*

Novo status: *{{2}}*

Alguma dúvida? Responda esta mensagem!
```

**Uso:** Enviada quando Roberta altera status no painel admin (PATCH endpoint existente).  
**Custo:** Cobrada como utility template fora do CSW (~$0.0080 USD para Brasil, jan/2026). **Grátis** se enviada dentro de janela de atendimento aberta.

---

### RF-10: Timeout de Conversa

- **30 minutos** sem resposta → conversa marcada como `EXPIRED`
- Mensagem de despedida (non-template, dentro do CSW):

```text
Parece que você ficou ocupado(a)! 😊
Sem problemas — quando quiser retomar, é só mandar uma mensagem.
Seus dados até agora foram salvos.
```

- Implementação: `@Scheduled(fixedRate = 60000)` job que varre conversas com `updatedAt < now() - 30min` e `step != COMPLETED && step != EXPIRED`

---

## 7. Requisitos Não-Funcionais

### 7.1 Performance

| Requisito | Meta |
| --- | --- |
| Tempo de resposta do webhook (aceitar POST) | < 500ms |
| Tempo de resposta ao cliente (enviar mensagem) | < 3s |
| Processamento assíncrono | Via `@Async` ou `CompletableFuture` |
| Throughput | 100 conversas simultâneas |

### 7.2 Segurança

| Requisito | Implementação |
| --- | --- |
| Validação de webhook | HMAC-SHA256 com App Secret |
| Token armazenamento | Env var `WHATSAPP_ACCESS_TOKEN` (nunca em código) |
| Dados PII | Mesma proteção do Order existente (TODO: issue C4 pendente) |
| Verify token | Env var `WHATSAPP_VERIFY_TOKEN` |
| Phone number ID | Env var `WHATSAPP_PHONE_NUMBER_ID` |

### 7.3 Resiliência

| Cenário | Comportamento |
| --- | --- |
| WhatsApp API down | Retry com backoff exponencial (3 tentativas) |
| Webhook timeout | Meta re-envia (built-in retry) |
| Server restart | Estado persiste em banco, retoma conversa |
| Mensagem duplicada | Idempotência via `message_id` do WhatsApp |
| Token inválido | Log error + alerta (token é permanente, mas pode ser revogado) |

### 7.4 Observabilidade

| Aspecto | Implementação |
| --- | --- |
| Logs | SLF4J structured logging por conversa |
| Métricas | Contadores: conversas iniciadas, completadas, expiradas, erros |
| Health check | `/api/health` já existente — adicionar status da integração WhatsApp |

### 7.5 Testabilidade

| Camada | Tipo de Teste | Framework |
| --- | --- | --- |
| `StepValidator` | Unitário | JUnit 5 + AssertJ |
| `ConversationService` | Unitário + Integração | JUnit 5 + Mockito |
| `WhatsAppWebhookController` | `@WebMvcTest` | MockMvc |
| Fluxo conversacional completo | Integração | `@SpringBootTest` + WireMock |
| Template messages | Manual | WhatsApp Business Manager |

---

## 8. Arquitetura

### 8.1 Diagrama de Fluxo

```text
┌──────────┐     POST webhook     ┌──────────────────┐
│ WhatsApp │ ──────────────────── │ WebhookController │
│ Cloud API│                      │ (valida HMAC)     │
│          │ ◄─── send message ── │                   │
└──────────┘                      └────────┬──────────┘
                                           │ async
                                  ┌────────▼──────────┐
                                  │ ConversationService│
                                  │ (state machine)    │
                                  └───┬───────────┬────┘
                                      │           │
                              ┌───────▼───┐ ┌─────▼────────┐
                              │ StepValid.│ │ WhatsAppClient│
                              │ (validates)│ │ (send msgs)  │
                              └───────────┘ └──────────────┘
                                      │
                              ┌───────▼────────┐
                              │ OrderService   │  (EXISTENTE)
                              │ createOrder()  │
                              └───────┬────────┘
                                      │
                              ┌───────▼────────┐
                              │ OrderRepository│  (EXISTENTE)
                              └────────────────┘
```

### 8.2 Pacote Novo

```text
backend/src/main/java/com/robertafurucho/
├── order/                          # EXISTENTE — não modificar
│   ├── Order.java
│   ├── OrderController.java
│   ├── OrderService.java
│   ├── CreateOrderRequest.java
│   ├── OrderResponse.java
│   ├── OrderStatus.java
│   └── OrderRepository.java
├── whatsapp/                       # NOVO
│   ├── WhatsAppWebhookController.java
│   ├── WhatsAppClient.java            # HTTP client para Cloud API
│   ├── WhatsAppConfig.java            # @ConfigurationProperties
│   ├── WhatsAppSignatureValidator.java # HMAC-SHA256
│   ├── conversation/
│   │   ├── ConversationState.java      # @Entity
│   │   ├── ConversationStep.java       # enum
│   │   ├── ConversationRepository.java # JPA
│   │   ├── ConversationService.java    # state machine
│   │   └── StepValidator.java          # validação por step
│   ├── message/
│   │   ├── WhatsAppMessage.java        # Record para payload recebido
│   │   ├── InteractiveMessageBuilder.java
│   │   └── TemplateMessageBuilder.java
│   └── scheduler/
│       └── ConversationExpirationJob.java  # @Scheduled
└── config/                          # EXISTENTE
    ├── WebConfig.java               # adicionar CORS para webhook? NÃO — server-to-server
    └── ...
```

### 8.3 Configuração

```properties
# application.properties (novo bloco)
whatsapp.enabled=false
whatsapp.api.base-url=https://graph.facebook.com/v25.0
whatsapp.api.phone-number-id=${WHATSAPP_PHONE_NUMBER_ID:}
whatsapp.api.access-token=${WHATSAPP_ACCESS_TOKEN:}
whatsapp.webhook.verify-token=${WHATSAPP_VERIFY_TOKEN:}
whatsapp.app.secret=${WHATSAPP_APP_SECRET:}
whatsapp.conversation.timeout-minutes=30
whatsapp.conversation.max-retries-per-field=3
```

```properties
# application-prod.properties
whatsapp.enabled=true
```

**Feature flag:** `whatsapp.enabled=false` em dev/test, `true` em prod. Webhook controller retorna 404 quando desabilitado.

---

## 9. Modelo de Custos

### 9.1 Pricing Model (vigente desde 01/07/2025)

A WhatsApp Cloud API usa **precificação por mensagem** (não mais por conversa):

| Tipo de Mensagem | Custo |
| --- | --- |
| **Non-template** (text, image, interactive) dentro do CSW (24h) | **Grátis** |
| **Utility template** dentro do CSW | **Grátis** |
| **Utility template** fora do CSW | ~$0.0080 USD (Brasil) |
| **Marketing template** | ~$0.0625 USD (Brasil) |
| **Mensagens recebidas** do cliente | **Grátis** (sempre) |

> **CSW** = Customer Service Window — janela de 24h aberta quando o **cliente** envia uma mensagem.

### 9.2 Análise de Custo para Roberta

#### Cenário: 50 pedidos/mês

| Fluxo | Tipo | Custo |
| --- | --- | --- |
| Cliente inicia conversa → abre CSW | Grátis | $0 |
| 8 perguntas + validações + resumo + confirmação (~12 msgs) | Non-template dentro CSW | $0 |
| Atualização de status (dentro de 24h) | Utility template dentro CSW | $0 |
| Atualização de status (depois de 24h) | Utility template fora CSW | $0.008 × 50 = $0.40 |
| **Total mensal** | | **~$0.40 USD (~R$2,50)** |

**Se Roberta quiser enviar mensagens proativas (marketing):**

- Template de boas-vindas para 200 contatos/mês = $0.0625 × 200 = $12.50 USD (~R$75)

**BRL billing:** Disponível a partir de 01/07/2026 para empresas brasileiras.

### 9.3 Custo de Infraestrutura

- **WhatsApp Cloud API**: Grátis (hosted by Meta)
- **Backend adicional**: Marginal (reusa o mesmo Spring Boot server)
- **Banco de dados**: +1 tabela, ~50 rows/mês — negligível
- **Total infra adicional**: $0

---

## 10. Fluxo Conversacional Detalhado

### 10.1 Exemplo Completo

```text
CLIENTE:  Oi
BOT:      🧸 Olá! Bem-vindo(a) à Roberta Furucho - Bonecas Artesanais!
          Vou te ajudar a fazer sua encomenda. São só algumas perguntas rápidas.
          
          Qual o seu *nome completo*?

CLIENTE:  Maria Silva
BOT:      Legal, Maria! 😊
          Qual o seu *email*?

CLIENTE:  maria@
BOT:      ❌ Email inválido. 
          Por favor, informe um email válido (ex: nome@email.com)

CLIENTE:  maria@email.com
BOT:      Qual o seu *telefone* com DDD? (só números, ex: 11999991234)

CLIENTE:  11 99999-1234
BOT:      Qual o *endereço* completo de entrega?

CLIENTE:  Rua das Flores, 123 - Apt 45 - São Paulo/SP
BOT:      Qual o *CEP*? (formato: 00000-000)

CLIENTE:  01234-567
BOT:      Que *tipo de boneca* você gostaria?
          [Botão: Amigurumi] [Botão: Boneca de Pano] [Botão: Outro tipo]

CLIENTE:  [clica: Amigurumi]
BOT:      Descreva os *detalhes* do que imagina: cores, tamanho, tema, personagem...
          (até 800 caracteres)

CLIENTE:  Ursinho de crochê azul bebê, 25cm, tema marinheiro com chapéu
BOT:      Para *quando* você precisa? (formato: DD/MM/AAAA)

CLIENTE:  15/04/2026
BOT:      🧸 *Resumo do seu Pedido*
          
          *Nome:* Maria Silva
          *Email:* maria@email.com
          *Telefone:* 11999991234
          *Endereço:* Rua das Flores, 123 - Apt 45 - São Paulo/SP
          *CEP:* 01234-567
          *Tipo:* Amigurumi
          *Detalhes:* Ursinho de crochê azul bebê, 25cm, tema marinheiro com chapéu  
          *Data desejada:* 15/04/2026
          
          Está tudo certo? 👇
          [Botão: ✓ Confirmar] [Botão: ✏ Corrigir] [Botão: ✗ Cancelar]

CLIENTE:  [clica: ✓ Confirmar]
BOT:      ✅ *Pedido Confirmado!*
          Seu pedido nº *#42* foi registrado com sucesso!
          A Roberta vai analisar e entrar em contato em breve. 
          Obrigada pela confiança! 🧸💕
```

### 10.2 Fluxo de Correção

```text
CLIENTE:  [clica: ✏ Corrigir]
BOT:      Qual campo deseja corrigir?
          [Lista interativa com todos os 8 campos + valor atual]

CLIENTE:  [seleciona: Email — Atual: maria@email.com]
BOT:      Qual o novo *email*?

CLIENTE:  maria.silva@gmail.com
BOT:      ✓ Email atualizado!
          [Exibe resumo novamente com botões de confirmação]
```

### 10.3 Comandos Especiais

| Comando | Efeito |
| --- | --- |
| `/cancelar` | Cancela conversa atual, inicia nova |
| `/status` | Consulta status do último pedido (se existir) |
| `/ajuda` | Exibe comandos disponíveis |
| Qualquer texto fora do fluxo | "Não entendi. [re-pergunta o campo atual]" |

---

## 11. WhatsApp Cloud API — Detalhes Técnicos

### 11.1 Envio de Mensagens

```http
POST https://graph.facebook.com/v25.0/{phone-number-id}/messages
Authorization: Bearer {access-token}
Content-Type: application/json

{
  "messaging_product": "whatsapp",
  "recipient_type": "individual",
  "to": "5511999991234",
  "type": "text",
  "text": { "body": "Qual o seu nome completo?" }
}
```

### 11.2 Recebimento de Mensagens (webhook payload)

```json
{
  "object": "whatsapp_business_account",
  "entry": [{
    "id": "WABA_ID",
    "changes": [{
      "value": {
        "messaging_product": "whatsapp",
        "metadata": {
          "display_phone_number": "5511XXXXX",
          "phone_number_id": "PHONE_NUMBER_ID"
        },
        "contacts": [{ "profile": { "name": "Maria" }, "wa_id": "5511999991234" }],
        "messages": [{
          "from": "5511999991234",
          "id": "wamid.XXXXX",
          "timestamp": "1709500000",
          "type": "text",
          "text": { "body": "Oi" }
        }]
      },
      "field": "messages"
    }]
  }]
}
```

### 11.3 Interactive Button Reply (webhook payload)

```json
{
  "type": "interactive",
  "interactive": {
    "type": "button_reply",
    "button_reply": {
      "id": "confirm_yes",
      "title": "✓ Confirmar"
    }
  }
}
```

### 11.4 Token Management

- **System User Token**: Permanente (não expira como o Instagram token de 60 dias)
- Gerado via Meta Business Suite → System Users → Generate Token
- Permissões necessárias: `whatsapp_business_messaging`, `whatsapp_business_management`
- Armazenado em `WHATSAPP_ACCESS_TOKEN` env var
- Pode ser revogado manualmente — monitorar erros 401

---

## 12. Plano de Testes

### 12.1 Testes Unitários (~25 testes)

| Classe | Testes | Framework |
| --- | --- | --- |
| `StepValidator` | 16 — 2 por campo (válido + inválido) | JUnit 5 + AssertJ |
| `ConversationService` | 5 — transições de estado, expiração, retomada | JUnit 5 + Mockito |
| `WhatsAppSignatureValidator` | 2 — assinatura válida + inválida | JUnit 5 |
| `InteractiveMessageBuilder` | 2 — botões + lista | JUnit 5 |

### 12.2 Testes de Integração (~8 testes)

| Cenário | Tipo |
| --- | --- |
| Webhook verification handshake | `@WebMvcTest` + MockMvc |
| Webhook POST com HMAC válido | `@WebMvcTest` + MockMvc |
| Webhook POST com HMAC inválido → 401 | `@WebMvcTest` + MockMvc |
| Fluxo completo: 8 perguntas → pedido criado | `@SpringBootTest` + WireMock |
| Fluxo com correção de campo | `@SpringBootTest` + WireMock |
| Timeout de conversa | `@SpringBootTest` |
| Conversa retomada após restart | `@SpringBootTest` |
| Feature flag desabilitada → 404 | `@WebMvcTest` |

### 12.3 Testes Manuais

| Cenário | Ambiente |
| --- | --- |
| Enviar mensagem real via WhatsApp Business teste | Meta Developer sandbox |
| Receber webhook real | ngrok + localhost |
| Template message aprovação | WhatsApp Business Manager |
| Fluxo completo com botões interativos | WhatsApp de teste |

### Total: ~33 testes automatizados + 4 cenários manuais

---

## 13. Riscos & Mitigações

| # | Risco | Probabilidade | Impacto | Mitigação |
| --- | --- | --- | --- | --- |
| R1 | Meta rejeita templates | Média | Alto | Seguir guidelines de templates, testar na sandbox antes |
| R2 | Verificação do Meta Business Portfolio demora | Alta | Bloqueante | Iniciar processo ASAP, independente do dev |
| R3 | 5s timeout do webhook causa re-envios duplicados | Média | Médio | Processamento assíncrono + idempotência por `message_id` |
| R4 | Cliente envia foto/áudio em vez de texto | Alta | Baixo | Responder "Por favor, envie sua resposta como texto" |
| R5 | Rate limit da Cloud API (80 msg/s per phone) | Baixa | Baixo | Muito acima da demanda esperada |
| R6 | Mudança de pricing | Baixa | Baixo | Custo atual é ~$0.40/mês — irrelevante para o negócio |
| R7 | Token revogado sem perceber | Baixa | Alto | Health check que tenta enviar msg para si mesmo + alertas |
| R8 | LGPD — dados pessoais via WhatsApp | Média | Alto | Mesma política do formulário web existente; adicionar aviso de privacidade na mensagem de boas-vindas |

---

## 14. Cronograma Estimado

| Fase | Duração | Tarefas |
| --- | --- | --- |
| **Fase 0: Pré-requisitos** | 1-2 semanas | Criar WhatsApp Business Account, Meta Business Portfolio, verificação, número dedicado, Meta App com WhatsApp use case |
| **Fase 1: Infraestrutura** | 1 semana | Webhook controller + HMAC validation + config + feature flag + testes webhook |
| **Fase 2: Core** | 1.5 semanas | State machine + StepValidator + ConversationService + WhatsAppClient + testes unitários |
| **Fase 3: Interactive** | 1 semana | Mensagens interativas (botões, listas) + correção + comandos especiais + testes integração |
| **Fase 4: Templates & Polish** | 0.5 semana | Template messages + status updates + timeout job + health check + testes manuais |
| **Total** | **~4-5 semanas** | (Fase 0 pode rodar em paralelo com Fase 1) |

---

## 15. Integração com o Sistema Existente

### 15.1 O que NÃO muda

| Componente | Status |
| --- | --- |
| `OrderForm.tsx` (frontend) | Continua funcionando — WhatsApp web é canal complementar |
| `OrderController.java` | Intacto — chatbot usa `OrderService` diretamente |
| `OrderService.java` | Intacto — reusado pelo `ConversationService` |
| `CreateOrderRequest.java` | Intacto — chatbot constrói o mesmo DTO |
| `Order.java` + `OrderStatus.java` | Intactos |
| `RateLimitingFilter` | Intacto — webhook tem rate limiting próprio |

### 15.2 O que muda

| Componente | Mudança |
| --- | --- |
| `pom.xml` | Adicionar dependência de HTTP client (WebClient ou RestTemplate) |
| `application.properties` | Novo bloco `whatsapp.*` |
| `application-prod.properties` | `whatsapp.enabled=true` |
| `WebConfig.java` | Nenhuma — webhook é server-to-server, sem CORS |
| Schema SQL | Nova tabela `conversation_states` |
| Health check | Adicionar status do WhatsApp integration |

### 15.3 Resolução do Issue C6

> **C6: Frontend nunca chama backend — orders vão diretamente para WhatsApp**

O chatbot é a **solução definitiva** para C6:

- Pedidos via chatbot chamam `OrderService.createOrder()` → persistidos em banco
- Frontend web continua como está (mudança seria em fase futura)
- Backend deixa de ser código morto
- Dados de pedidos ficam consultáveis via `GET /api/orders`

---

## 16. Variáveis de Ambiente

```bash
# Obrigatórias em produção
WHATSAPP_PHONE_NUMBER_ID=123456789012345
WHATSAPP_ACCESS_TOKEN=EAAxxxxxxx...
WHATSAPP_VERIFY_TOKEN=meu_token_secreto_verificacao
WHATSAPP_APP_SECRET=abc123def456...

# Opcionais
WHATSAPP_API_BASE_URL=https://graph.facebook.com/v25.0  # default
WHATSAPP_CONVERSATION_TIMEOUT_MINUTES=30                 # default
```

---

## 17. Definition of Done

- [ ] Webhook controller deployado e verificado pelo Meta
- [ ] Fluxo completo de 8 perguntas funcional via WhatsApp real
- [ ] Validação campo-a-campo com mensagens em português
- [ ] Mensagens interativas (botões + listas) renderizando corretamente
- [ ] Pedido criado no banco via `OrderService.createOrder()`
- [ ] Template messages aprovados pelo Meta
- [ ] Feature flag funcional (disabled em dev, enabled em prod)
- [ ] HMAC validation ativa em todo webhook POST
- [ ] Timeout de conversa funcionando (30 min)
- [ ] ~33 testes automatizados passando
- [ ] Testes manuais com WhatsApp real aprovados
- [ ] Documentação de setup atualizada em DEPLOYMENT.md
- [ ] Zero violations de segurança (token em env vars, HMAC, etc.)

---

## 18. Referências

| Recurso | URL |
| --- | --- |
| WhatsApp Cloud API Docs | <https://developers.facebook.com/docs/whatsapp/cloud-api> |
| Pricing (per-message, jul/2025) | <https://developers.facebook.com/docs/whatsapp/pricing> |
| Get Started Guide | <https://developers.facebook.com/docs/whatsapp/cloud-api/get-started> |
| Interactive Messages | <https://developers.facebook.com/docs/whatsapp/cloud-api/messages/interactive> |
| Template Messages | <https://developers.facebook.com/docs/whatsapp/cloud-api/messages/template> |
| Webhooks Reference | <https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks> |
| System User Tokens | <https://developers.facebook.com/docs/whatsapp/business-management-api/get-started> |
| Jasper's Market Sample | <https://github.com/fbsamples/whatsapp-business-jaspers-market> |
| REVIEW_AND_IMPROVEMENTS.md (issue C6) | docs/REVIEW_AND_IMPROVEMENTS.md |
| useOrderFormValidation.ts | src/component/OrderForm/useOrderFormValidation.ts |
| CreateOrderRequest.java | backend/src/main/java/com/robertafurucho/order/CreateOrderRequest.java |
