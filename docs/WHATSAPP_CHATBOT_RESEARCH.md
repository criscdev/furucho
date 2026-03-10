# WhatsApp Chatbot Research — Furucho Order Bot



> **Date:** March 3, 2026  
> **Context:** Small artisanal biscuit doll business (Roberta Furucho) in Brazil  
> **Goal:** Replace web form → WhatsApp link with a conversational WhatsApp bot that IS the form

---

## Table of Contents

1. [Current Architecture](#1-current-architecture)
2. [Official WhatsApp Cloud API (Meta)](#2-official-whatsapp-cloud-api-meta)
3. [Twilio WhatsApp API](#3-twilio-whatsapp-api)
4. [Blip (Take Blip) — Brazilian Platform](#4-blip-take-blip--brazilian-platform)
5. [Low-Code Options (WATI, Chatfuel, ManyChat)](#5-low-code-options-wati-chatfuel-manychat)
6. [Architecture: Spring Boot Integration](#6-architecture-spring-boot-integration)
7. [Conversational Flow Design](#7-conversational-flow-design)
8. [Pricing Comparison](#8-pricing-comparison)
9. [Recommendation](#9-recommendation)

---

## 1. Current Architecture

### What exists today

- **Frontend:** React + React Router form (`OrderForm.tsx`) with `useOrderFormValidation` hook
- **Backend:** Spring Boot REST API at `POST /api/orders` accepting `CreateOrderRequest`
- **Flow:** User fills web form → clicks "Enviar pelo WhatsApp" → opens `wa.me/` link with pre-formatted message
- **Entity fields:** `name`, `email`, `phone`, `address`, `postalCode`, `orderScope`, `orderScopeDetail`, `receiveDate`
- **Validations:** Name (required, max 200), Email (regex), Phone (10-11 digits), CEP (00000-000), orderScope (required), orderScopeDetail (required, max 800), receiveDate (future date)
- **Backend already persists orders** with status tracking (`PENDING`, etc.)

### What we want

WhatsApp conversation **is** the form. Customer messages the business number, bot asks questions one by one, validates each answer, and creates an `Order` entity when complete.

---

## 2. Official WhatsApp Cloud API (Meta)

### Overview

The WhatsApp Cloud API is Meta's direct API for sending/receiving WhatsApp messages. It's free to use (no middleware fee) — you only pay Meta's per-message charges for template messages.

**Key facts (as of March 2026):**

- Built on Graph API, uses HTTP/JSON
- 80 messages/second throughput per phone number
- Webhooks deliver incoming messages and status updates to your server
- Test WABA and test phone number created automatically on signup
- Postman collection available for testing

### How Webhooks Work

1. **You register a webhook URL** in the Meta App Dashboard (under WhatsApp > Configuration)
2. Meta sends `POST` requests to your endpoint with JSON payloads
3. **Incoming message webhook payload structure:**

   ```json
   {
     "object": "whatsapp_business_account",
     "entry": [{
       "id": "WABA_ID",
       "changes": [{
         "value": {
           "messaging_product": "whatsapp",
           "metadata": {
             "display_phone_number": "15550783881",
             "phone_number_id": "106540352242922"
           },
           "contacts": [{
             "profile": { "name": "Customer Name" },
             "wa_id": "5511999998888"
           }],
           "messages": [{
             "from": "5511999998888",
             "id": "wamid.HBgLMTY...",
             "timestamp": "1677000000",
             "type": "text",
             "text": { "body": "Olá, quero encomendar uma boneca" }
           }]
         },
         "field": "messages"
       }]
     }]
   }
   ```

4. **Webhook verification:** Meta sends a `GET` request with `hub.mode`, `hub.verify_token`, `hub.challenge` — your server must respond with the challenge value
5. **Retry policy:** If your endpoint doesn't return 200, Meta retries with decreasing frequency for up to 7 days
6. **Payload size:** Up to 3 MB
7. **Security:** Supports mutual TLS (mTLS)

### Sending Messages

**Non-template messages** (free within customer service window):

```bash
curl 'https://graph.facebook.com/v23.0/<PHONE_NUMBER_ID>/messages' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <TOKEN>' \
  -d '{
    "messaging_product": "whatsapp",
    "to": "5511999998888",
    "type": "text",
    "text": { "body": "Qual é o seu nome completo?" }
  }'
```

**Interactive messages (buttons):**

```json
{
  "messaging_product": "whatsapp",
  "to": "5511999998888",
  "type": "interactive",
  "interactive": {
    "type": "button",
    "body": { "text": "Qual tipo de boneca?" },
    "action": {
      "buttons": [
        { "type": "reply", "reply": { "id": "tipo_anjo", "title": "Anjo" } },
        { "type": "reply", "reply": { "id": "tipo_noiva", "title": "Noiva" } },
        { "type": "reply", "reply": { "id": "tipo_custom", "title": "Personalizada" } }
      ]
    }
  }
}
```

**Interactive messages (list):**

```json
{
  "messaging_product": "whatsapp",
  "to": "5511999998888",
  "type": "interactive",
  "interactive": {
    "type": "list",
    "body": { "text": "Escolha o tipo de boneca:" },
    "action": {
      "button": "Ver opções",
      "sections": [{
        "title": "Tipos de Boneca",
        "rows": [
          { "id": "tipo_anjo", "title": "Anjo", "description": "Boneca estilo anjo com asas" },
          { "id": "tipo_noiva", "title": "Noiva", "description": "Boneca com vestido de noiva" },
          { "id": "tipo_custom", "title": "Personalizada", "description": "Descreva como quiser" }
        ]
      }]
    }
  }
}
```

**Limits on interactive messages:**

- Buttons: max 3 buttons per message
- Lists: max 10 rows per section, max 10 sections
- Quick replies render as tappable buttons

### Template Messages vs Session Messages

| Aspect | Template Messages | Session (Non-Template) Messages |
| --- | --- | --- |
| **When** | Can be sent anytime | Only within 24-hour customer service window |
| **Approval** | Require Meta approval before use | No approval needed |
| **Cost** | Charged per-message (rate depends on category + country) | **FREE** |
| **Types** | Marketing, Utility, Authentication | Text, image, video, document, interactive, location, etc. |
| **Use case** | Initiating conversations, notifications | Responding to customers, conversational flows |

### Pricing Model (Per-Message, effective July 1, 2025)

**Critical insight for this project:**

- **All non-template messages are FREE** within a 24-hour customer service window (CSW)
- The CSW opens when a **customer messages you first**
- **Utility templates within an open CSW are also FREE**
- **Only template messages are charged**, and only outside a CSW
- Brazil country code is +55 — has its own rate card
- **BRL billing coming July 1, 2026** for Brazilian businesses

**What this means for Furucho:**
> If the customer initiates the conversation (e.g., they message "Oi" or click a wa.me link), the 24-hour window opens. ALL bot responses during the order-taking conversation are FREE. You only pay if you need to proactively reach out later (e.g., order status update) with a template message.

**Brazil rates (USD, as of Jan 2026):**

- Marketing template: ~$0.0625/msg
- Utility template (outside CSW): ~$0.0080/msg  
- Authentication template: ~$0.0315/msg
- **Everything inside CSW: $0.00**

### Verification Requirements

1. **Facebook/Meta developer account** (free)
2. **Meta Business Portfolio** (formerly Business Manager)
3. **Business phone number** (can be a virtual/VoIP number — cannot be currently on WhatsApp)
4. **Business verification** (optional but recommended for higher messaging limits and Official Business Account badge)
   - Requires: Business documents (CNPJ for Brazil), website, business address
5. **Display name verification** for the business phone number

### Can Spring Boot Handle the Webhook?

**Absolutely yes.** The webhook is just an HTTP endpoint:

```java
@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsAppWebhookController {
    
    // Webhook verification (GET)
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).build();
    }
    
    // Incoming messages (POST)  
    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody WebhookPayload payload) {
        // Process message, advance conversation state
        return ResponseEntity.ok().build();
    }
}
```

**Hosting requirement:** The webhook URL must be publicly accessible via HTTPS. Options:

- Deploy on any cloud (AWS, Azure, Railway, Render, Fly.io, etc.)
- Use ngrok for local development/testing
- Your existing Spring Boot Docker setup works perfectly

---

## 3. Twilio WhatsApp API

### Twilio Overview

Twilio acts as a middleware layer between your application and WhatsApp's API. They handle the WhatsApp Business Account management and provide their own SDK/API.

### Twilio Key Features

- **Programmable Messaging API** for one-way and two-way messaging
- **Conversations API** for managing multi-party, cross-channel conversations
- **Studio** — no-code/low-code flow builder (visual, drag-and-drop)
- **WhatsApp Sandbox** — test without WhatsApp Business verification (huge DX win)
- SDKs for Java, Python, Node, PHP, Ruby, C#, Go

### Pricing vs Direct Cloud API

| Item | WhatsApp Cloud API (Direct) | Twilio |
| --- | --- | --- |
| **Platform fee** | FREE | ~$0.005-0.01/msg Twilio markup on top of WhatsApp fees |
| **WhatsApp fees** | Per-message (template only) | Same WhatsApp fees + Twilio fee |
| **Phone number** | Free test number; BYO for production | $1/month for WhatsApp-enabled number |
| **Sandbox** | Test WABA (limited) | Full sandbox, no verification needed |
| **SDK** | No official SDK (REST only, 3rd-party wrappers) | Excellent official SDK for 7+ languages |
| **Session management** | You build it yourself | Conversations API handles sessions |

### Developer Experience

**Pros:**

- Excellent Java SDK (`com.twilio.sdk`)
- WhatsApp Sandbox for instant testing without Meta verification
- Studio visual flow builder can prototype the conversational flow
- Built-in webhook handling via Twilio's callback URLs
- Conversations API provides session management out-of-the-box

**Cons:**

- Extra cost layer (Twilio markup per message)
- Another vendor dependency
- For a simple bot, the Twilio overhead may not be worth it

### Session Management

Twilio Conversations API automatically tracks:

- Conversation states (active, inactive, closed)
- Participants
- Message history

However, for a structured form flow, **you'd still need your own state machine** on your backend.

### Testing

- **Sandbox:** Join by sending "join `<word>`" to Twilio's sandbox number
- Works immediately, no WhatsApp Business verification needed
- Can test all message types including interactive

---

## 4. Blip (Take Blip) — Brazilian Platform

### Blip Overview

Blip (formerly Take.net) is a **Brazilian-born** conversational platform headquartered in Belo Horizonte, MG. They are an **official Meta WhatsApp Business Solution Provider (BSP).**

Major Brazilian brands use Blip: Coca-Cola Brazil, Ponto Frio, and others (80M+ messages in a single WhatsApp campaign for Coca-Cola).

### Blip Key Features

- **Visual Flow Builder (Blip Builder)** — drag-and-drop conversational flows
- **Official WhatsApp API integration** — they handle WABA setup
- **NLP/AI built-in** — natural language processing for understanding free-form messages
- **Team Inbox (Blip Desk)** — human handoff when bot can't handle
- **Multi-channel** — WhatsApp, Telegram, Facebook Messenger, Instagram, website chat
- **Custom backend integration** — webhooks, HTTP actions from within flows
- **Analytics dashboard**

### Pricing for Small Business

- **Free tier:** Up to 2 agents, limited conversations/month, custom flows, access to Blip Community. **No WhatsApp integration** on free tier.
- **Paid plans:** Contact sales — pricing is quote-based, not published. Likely starts around R$500-1000/month for small business plans with WhatsApp.
- **You still pay WhatsApp's per-message fees** on top of Blip's platform fee.

### Integration with Custom Backend

Blip supports:

- **HTTP Actions** within flows — call your Spring Boot API at any step
- **Webhooks** — Blip can forward events to your backend
- **Blip SDK** — programmatic access to conversations

**How it would work for Furucho:**

1. Blip Builder handles the conversational flow (asks questions, validates)
2. At the end, an HTTP Action calls `POST /api/orders` on your Spring Boot backend
3. Order is persisted, confirmation sent back to user

### Verdict

**Pros:** Brazilian company, Portuguese-first support, visual builder, official Meta partner, NLP.  
**Cons:** Paid plans not transparent (must talk to sales), overkill for a simple 8-question form, another platform to manage, less developer control.

---

## 5. Low-Code Options (WATI, Chatfuel, ManyChat)

### WATI

**What it is:** WhatsApp Business API SaaS platform with team inbox, chatbot builder, and API.

**Key features:**

- No-code chatbot builder (keyword triggers, flow automation)
- Team inbox for collaborative chat management
- API with V1 (legacy) and V3 (recommended) — REST-based
- Webhooks for events: message received, sent, delivered, read, replied, template status
- Interactive messages (list messages, button messages) via API
- Broadcast/campaign management
- Send template messages programmatically
- Contact management

**Pricing:** Starts ~$49/month (Growth plan), ~$99/month (Pro plan with chatbot builder), enterprise custom.

**Custom integration:** Full webhook support — can POST events to your Spring Boot backend. Also has direct API for sending messages programmatically.

**Can it handle a structured form flow?** Yes — the chatbot builder can create sequential question flows with conditional logic. Pro plan includes the chatbot builder feature.

### Chatfuel

**What it is:** Meta-approved tech provider for WhatsApp, Instagram, and Facebook chatbots.

**Key features:**

- Visual flow builder
- ChatGPT integration built-in
- WhatsApp Business API latest version integrated
- Lead generation, feedback collection, e-commerce flows
- Template management

**Pricing:**

- Business plan: from $39/month
- Enterprise: from $400/month
- 7-day free trial

**Custom integration:** Webhook/API integrations possible but more limited than WATI. Focused on marketing automation rather than custom backend integration.

### ManyChat

**What it is:** Popular chatbot platform for Instagram, Facebook, and WhatsApp.

**Key features:**

- Visual flow builder with drag-and-drop
- WhatsApp automation
- Multi-channel (Instagram DM, Facebook Messenger, SMS, WhatsApp)
- Built-in CRM features

**Pricing:** Free tier (limited), Pro from ~$15/month (but WhatsApp may require higher tiers).

**Custom integration:** Webhook actions, external API calls within flows.

### Comparison Table

| Feature | WATI | Chatfuel | ManyChat |
| --- | --- | --- | --- |
| **Structured form flow** | ✅ Via chatbot builder (Pro) | ✅ Via flow builder | ✅ Via flow builder |
| **Webhook to Spring Boot** | ✅ Full webhook + API | ⚠️ Basic webhook | ⚠️ Basic webhook actions |
| **API quality** | Very good (V3 is modern REST) | Limited | Limited |
| **Starting price** | ~$49-99/mo | ~$39/mo | ~$15/mo |
| **Brazilian focus** | International | International | International |
| **Team inbox** | ✅ | ⚠️ Limited | ⚠️ Limited |
| **Interactive messages** | ✅ Buttons + Lists | ✅ | ✅ |

---

## 6. Architecture: Spring Boot Integration

### Reusing the Existing Backend

**YES — the existing Spring Boot backend is ideal for receiving webhooks.** Here's the architecture:

```text
Customer                Meta                    Your Server
  │                      │                         │
  ├──"Oi"──────────────►│                         │
  │                      ├──webhook POST──────────►│
  │                      │                         ├─ Look up conversation state
  │                      │                         ├─ Determine next question
  │                      │◄──send message API──────┤  "Olá! Qual seu nome?"
  │◄─────────────────────┤                         │
  │                      │                         │
  ├──"Maria Silva"──────►│                         │
  │                      ├──webhook POST──────────►│
  │                      │                         ├─ Validate name
  │                      │                         ├─ Store in conversation state
  │                      │                         ├─ Advance to next step
  │                      │◄──send message API──────┤  "Ótimo, Maria! Qual seu email?"
  │◄─────────────────────┤                         │
  │                      │                         │
  │           ... (6 more questions) ...           │
  │                      │                         │
  │                      │                         ├─ All fields collected
  │                      │                         ├─ POST /api/orders (internal)
  │                      │                         ├─ Order entity created
  │                      │◄──send message API──────┤  "Pedido #123 criado! ✅"
  │◄─────────────────────┤                         │
```

### New Components Needed

```text
backend/src/main/java/com/robertafurucho/
├── order/                          # ← EXISTING (no changes needed)
│   ├── Order.java
│   ├── OrderController.java
│   ├── OrderService.java
│   ├── CreateOrderRequest.java
│   └── ...
├── whatsapp/                       # ← NEW
│   ├── WhatsAppWebhookController.java   # Receives webhooks from Meta
│   ├── WhatsAppMessageService.java      # Sends messages via Cloud API
│   ├── WhatsAppMessageParser.java       # Parses webhook payloads
│   └── dto/
│       ├── WebhookPayload.java          # Webhook request DTO
│       ├── WhatsAppMessage.java         # Outgoing message DTO
│       └── InteractiveMessage.java      # Buttons/lists DTO
├── conversation/                   # ← NEW
│   ├── ConversationState.java           # Entity: tracks where user is in flow
│   ├── ConversationStep.java            # Enum: GREETING, ASK_NAME, ASK_EMAIL, ...
│   ├── ConversationService.java         # State machine logic
│   ├── ConversationRepository.java      # Persists state to DB
│   └── StepValidator.java              # Per-step input validation
└── config/
    └── WhatsAppConfig.java              # API tokens, phone number ID, verify token
```

### Conversation State Machine

```java
public enum ConversationStep {
    GREETING,           // Initial greeting, explain the bot
    ASK_NAME,           // "Qual é o seu nome completo?"
    ASK_EMAIL,          // "Qual é o seu email?"
    ASK_PHONE,          // "Qual é o seu telefone?"
    ASK_ADDRESS,        // "Qual é o seu endereço completo?"
    ASK_POSTAL_CODE,    // "Qual é o seu CEP?"
    ASK_DOLL_TYPE,      // "Qual tipo de boneca?" (interactive buttons/list)
    ASK_DOLL_DETAILS,   // "Descreva os detalhes da boneca:"
    ASK_DELIVERY_DATE,  // "Qual a data desejada para entrega? (DD/MM/AAAA)"
    CONFIRM,            // Show summary, ask for confirmation
    COMPLETED,          // Order created
    CANCELLED           // User cancelled
}
```

### ConversationState Entity

```java
@Entity
@Table(name = "conversation_states")
public class ConversationState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "wa_id", unique = true, nullable = false)
    private String waId;  // WhatsApp phone number (e.g., "5511999998888")
    
    @Enumerated(EnumType.STRING)
    private ConversationStep currentStep;
    
    // Accumulated form data
    private String name;
    private String email;
    private String phone;
    private String address;
    private String postalCode;
    private String orderScope;
    private String orderScopeDetail;
    private LocalDate receiveDate;
    
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastMessageAt = LocalDateTime.now();
    }
}
```

### Per-Step Validation (Mirroring `useOrderFormValidation`)

```java
@Component
public class StepValidator {
    
    public Optional<String> validate(ConversationStep step, String input) {
        return switch (step) {
            case ASK_NAME -> {
                if (input.isBlank()) yield Optional.of("Por favor, digite seu nome.");
                if (input.length() > 200) yield Optional.of("Nome muito longo (máx 200 caracteres).");
                yield Optional.empty();
            }
            case ASK_EMAIL -> {
                if (!input.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
                    yield Optional.of("Email inválido. Exemplo: maria@email.com");
                yield Optional.empty();
            }
            case ASK_PHONE -> {
                String digits = input.replaceAll("\\D", "");
                if (!digits.matches("\\d{10,11}"))
                    yield Optional.of("Telefone deve ter 10 ou 11 dígitos. Exemplo: 11999998888");
                yield Optional.empty();
            }
            case ASK_POSTAL_CODE -> {
                if (!input.matches("\\d{5}-?\\d{3}"))
                    yield Optional.of("CEP inválido. Formato: 00000-000");
                yield Optional.empty();
            }
            case ASK_DELIVERY_DATE -> {
                try {
                    LocalDate date = LocalDate.parse(input, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    if (!date.isAfter(LocalDate.now()))
                        yield Optional.of("A data deve ser no futuro.");
                    yield Optional.empty();
                } catch (Exception e) {
                    yield Optional.of("Data inválida. Formato: DD/MM/AAAA");
                }
            }
            default -> Optional.empty();
        };
    }
}
```

### Handling User Corrections

```text
User: "Errei o endereço" / "Quero corrigir" / "Voltar"

Bot: "Qual campo você quer corrigir?"
     [Nome] [Email] [Telefone]
     [Endereço] [CEP] [Tipo de Boneca]
     
User taps: [Endereço]
Bot: "Seu endereço atual é: Rua X, 123. Qual é o novo endereço?"
User: "Rua Y, 456"
Bot: "Endereço atualizado! ✅ Vamos continuar..."
```

Implementation: detect correction keywords → show interactive buttons with field names → set step to the appropriate ASK_* step → resume from there.

### 24-Hour Messaging Window Rules

| Scenario | Can send? | Cost |
| --- | --- | --- |
| Customer messages first (opens CSW) | ✅ Any message type | FREE (non-template) |
| Within 24h of last customer message | ✅ Any message type | FREE |
| After 24h, no customer reply | ❌ Only template messages | Charged per template |
| Customer comes from Click-to-WhatsApp ad | ✅ Any message for 72h (FEP window) | FREE |

**For Furucho's use case:** The customer always initiates (they message first to start an order). The entire order conversation will take 5-15 minutes — well within the 24-hour window. **All conversational messages are free.**

You only need template messages for:

- Proactive order status updates (e.g., "Seu pedido está pronto!")
- Re-engaging customers who abandoned mid-conversation (after 24h)

---

## 7. Conversational Flow Design

### Happy Path

```text
CUSTOMER messages: "Oi" / "Quero fazer um pedido" / (any message)
    ↓
BOT: "Olá! 👋 Bem-vindo à Roberta Furucho Biscuit!
      Vou te ajudar a fazer sua encomenda de boneca.
      Vamos lá? São só 8 perguntinhas rápidas! 😊
      
      Qual é o seu nome completo?"
    ↓
CUSTOMER: "Maria Silva"
    ↓
BOT: "Obrigado, Maria! 📧 Qual é o seu email?"
    ↓
CUSTOMER: "maria@email.com"
    ↓
BOT: "Perfeito! 📱 Qual é o seu telefone? (DDD + número)"
    ↓
CUSTOMER: "11999998888"
    ↓
BOT: "Anotado! 🏠 Qual é o seu endereço completo para entrega?"
    ↓
CUSTOMER: "Rua das Flores, 123 - São Paulo, SP"
    ↓
BOT: "📮 Qual é o seu CEP? (formato: 00000-000)"
    ↓
CUSTOMER: "01234-567"
    ↓
BOT: "Agora vamos à boneca! 🎎 Qual tipo de boneca você gostaria?"
     [Interactive list with options like: Anjo, Noiva, Debutante, Personalizada, ...]
    ↓
CUSTOMER taps: "Noiva"
    ↓
BOT: "Linda escolha! ✍️ Descreva os detalhes da sua boneca noiva:
      (cores, estilo do vestido, acessórios, etc. — até 800 caracteres)"
    ↓
CUSTOMER: "Quero uma boneca noiva com vestido branco rendado, 
           véu longo, buquê de rosas vermelhas, cabelo castanho."
    ↓
BOT: "Por último! 📅 Qual a data desejada para entrega? (DD/MM/AAAA)"
    ↓
CUSTOMER: "15/06/2026"
    ↓
BOT: "Perfeito! Aqui está o resumo do seu pedido:
      
      👤 Nome: Maria Silva
      📧 Email: maria@email.com
      📱 Telefone: 11999998888
      🏠 Endereço: Rua das Flores, 123 - São Paulo, SP
      📮 CEP: 01234-567
      🎎 Tipo: Noiva
      ✍️ Detalhes: Boneca noiva com vestido branco rendado...
      📅 Entrega desejada: 15/06/2026
      
      Tudo certinho?"
     [✅ Confirmar pedido] [✏️ Corrigir algo] [❌ Cancelar]
    ↓
CUSTOMER taps: [✅ Confirmar pedido]
    ↓
BOT: "Pedido #1234 criado com sucesso! 🎉
      
      A Roberta vai analisar e entrar em contato em breve 
      para confirmar valores e prazos.
      
      Obrigado por escolher a Roberta Furucho Biscuit! 💕"
```

### Error/Correction Handling

```text
BOT: "📧 Qual é o seu email?"
CUSTOMER: "maria email"
BOT: "Hmm, parece que esse email não está no formato certo. 
      Exemplo: maria@email.com — Pode tentar de novo?"
CUSTOMER: "maria@email.com"
BOT: "Perfeito! 📱 Qual é o seu telefone?"
```

### Template Messages Needed

Only 2-3 templates required:

1. **Order confirmation** (Utility): "Olá {{1}}! Seu pedido #{{2}} foi recebido. Entraremos em contato em breve! 🎎"
2. **Order status update** (Utility): "Olá {{1}}! Atualização do pedido #{{2}}: {{3}}"
3. **Conversation re-engagement** (Marketing, optional): "Olá {{1}}! Notamos que você começou um pedido mas não finalizou. Gostaria de continuar? 😊"

---

## 8. Pricing Comparison

### Monthly Cost Estimate (assuming ~50 orders/month)

| Platform | Platform Fee | WhatsApp Fees | Total Est. |
| --- | --- | --- | --- |
| **Meta Cloud API (Direct)** | $0 | ~$0 (all user-initiated) + ~$2-4 for templates | **~$2-4/mo** |
| **Twilio** | $1/mo (number) + ~$0.005/msg markup | Same as above | **~$15-25/mo** |
| **Blip** | ~R$500+ /mo (quoted) | + WhatsApp fees | **R$500+/mo ($100+)** |
| **WATI** | $49-99/mo | + WhatsApp fees | **$50-100/mo** |
| **Chatfuel** | $39/mo | + WhatsApp fees | **$40-45/mo** |
| **ManyChat** | $15+/mo | + WhatsApp fees | **$20-25/mo** |

### Notes

- **Cloud API direct is nearly free** for this use case because all order conversations are user-initiated (free CSW), and you'd only send 1-2 utility templates per order for confirmations
- WhatsApp fees for utility templates in Brazil: ~$0.008 per message. At 50 orders × 2 templates = $0.80/month in WhatsApp fees
- Low-code platforms charge their platform fee **on top of** WhatsApp's own message fees

---

## 9. Recommendation

### 🏆 Winner: Official WhatsApp Cloud API (Direct) + Spring Boot

**For Furucho's specific situation, the direct Meta Cloud API is the clear best choice.**

#### Why

| Factor | Cloud API (Direct) | Why it wins |
| --- | --- | --- |
| **Cost** | ~$2-4/month | 20-100x cheaper than alternatives |
| **You already have Spring Boot** | Perfect fit | Webhook is just another REST endpoint |
| **Developer control** | Full | You own the flow, the data, the logic |
| **Existing Order entity** | Reuse as-is | `CreateOrderRequest` already matches the fields |
| **Existing validations** | Port from `useOrderFormValidation` | Same regex patterns, same error messages |
| **Brazil BRL billing** | Coming July 2026 | Native Brazilian currency support |
| **No vendor lock-in** | Direct Meta API | No middleware to get stuck on |
| **Interactive messages** | Buttons + Lists | Perfect for doll type selection |
| **Privacy** | Your server only | Order data never passes through third-party |

#### When would alternatives make sense?

- **Twilio** → If you needed multi-channel (SMS + WhatsApp) or had zero backend experience
- **Blip** → If you needed visual flow builder for non-technical staff, NLP, or scaled to many agents
- **WATI** → If you needed a team inbox for multiple customer service agents
- **Chatfuel/ManyChat** → If you had zero coding ability and needed pure no-code

### Implementation Roadmap

```text
Phase 1: Foundation (1-2 weeks)
├── Set up Meta Developer App + WhatsApp Business Account
├── Create WhatsAppWebhookController (verify + receive)
├── Create WhatsAppMessageService (send text + interactive)
├── Set up ngrok for local development
└── Send/receive first test message

Phase 2: Conversational Flow (1-2 weeks)
├── Create ConversationState entity + repository
├── Implement ConversationStep enum + state machine
├── Implement StepValidator (port from useOrderFormValidation)
├── Create ConversationService (main flow logic)
├── Handle validation errors (re-prompt)
└── Handle corrections (detect keywords → show edit buttons)

Phase 3: Order Creation (3-5 days)
├── Wire conversation completion → OrderService.createOrder()
├── Create 2 message templates (confirmation + status update)
├── Submit templates for Meta approval
├── Send confirmation message on order creation
└── Handle conversation timeout/abandonment

Phase 4: Production (3-5 days)
├── Deploy to production (existing Docker setup)
├── Register production phone number
├── Complete business verification (CNPJ)
├── Test end-to-end with real WhatsApp
└── Transition from wa.me link to conversational bot

Optional Phase 5: Enhancements
├── CEP auto-lookup via ViaCEP API (fill address automatically)
├── Photo upload (customer sends reference image)
├── Order status tracking via WhatsApp
├── Abandoned conversation re-engagement templates
└── Multi-language support (PT-BR primary)
```

### Key Technical Decisions

1. **State storage:** Database (already have JPA/Hibernate). Could use Redis for faster access if scale demands.
2. **Webhook security:** Validate webhook signatures from Meta. Use mutual TLS if desired.
3. **Idempotency:** Handle duplicate webhooks (Meta retries). Use message `wamid` as idempotency key.
4. **Concurrency:** One customer = one conversation state. Use `wa_id` as unique key. Handle race conditions with optimistic locking.
5. **Timeout:** If no response in 2 hours, send "Ainda está aí?" reminder. After 24h, conversation resets.
6. **Testing:** Use Meta's test WABA + test phone number. No charge for test messages.

---

## Appendix: Quick Reference Links

| Resource | URL |
| --- | --- |
| WhatsApp Cloud API Overview | <https://developers.facebook.com/docs/whatsapp/cloud-api/overview> |
| Cloud API Get Started | <https://developers.facebook.com/docs/whatsapp/cloud-api/get-started> |
| Webhooks | <https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks> |
| Sending Messages | <https://developers.facebook.com/docs/whatsapp/cloud-api/messages/send-messages> |
| Interactive Messages | <https://developers.facebook.com/docs/whatsapp/cloud-api/messages/interactive-messages> |
| Template Messages | <https://developers.facebook.com/docs/whatsapp/cloud-api/messages/template-messages> |
| WhatsApp Pricing | <https://developers.facebook.com/docs/whatsapp/pricing> |
| Graph API Webhooks Setup | <https://developers.facebook.com/docs/graph-api/webhooks/getting-started> |
| Jasper's Market Sample App | <https://github.com/fbsamples/whatsapp-business-jaspers-market> |
| WhatsApp Postman Collection | <https://www.postman.com/meta/whatsapp-business-platform/> |
| ViaCEP API (CEP lookup) | <https://viacep.com.br/> |
