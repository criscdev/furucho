# Development Journal - Roberta Furucho Platform

This journal tracks the development progress, decisions, and lessons learned during the creation of the Roberta Furucho handmade dolls platform.

---

## 2026-01-27 - Code Review & Bug Fixes

### Issues Fixed

- **Removed malformed file**: Deleted rogue `package.json (edit: add a "test" script...)` file
- **Spring Boot upgrade**: Updated from 3.2.5 to 3.4.1 for latest features
- **Bucket4j API**: Migrated from deprecated `Bandwidth.simple()` to new builder API
- **Test annotation**: Replaced deprecated `@MockBean` with `@MockitoBean` for Spring Boot 3.4
- **Markdown formatting**: Fixed all tables, code blocks, and list formatting across docs
- **Gallery props**: Made `items` prop optional with default placeholder data
- **Created DEV_JOURNAL.md**: Added development tracking documentation

### Files Modified

- `backend/pom.xml` - Spring Boot 3.4.1
- `backend/.../OrderController.java` - New Bucket4j builder API
- `backend/.../OrderControllerTest.java` - @MockitoBean annotation
- `README.md` - Fixed formatting, updated Spring version
- `docs/*.md` - Fixed markdown linting issues

---

## 2026-01-27 - Project Implementation Complete

### What Was Done

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
| Frontend Tests | 15 passing |
| Components | 4 (Header, Welcome, Gallery, OrderForm) |
| API Endpoints | 4 |
| Documentation Files | 6 |
| Total Files Created | ~30 |

---

## File Structure Summary

```text
furucho/
├── app/                    # React Router routes
├── src/component/          # Reusable components
├── backend/                # Spring Boot API
├── docs/                   # Project documentation
└── .github/workflows/      # CI/CD pipelines
```

---

## Last Updated

2026-01-27
