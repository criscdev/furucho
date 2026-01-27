# AI Guidelines

Rules for AI-assisted development on this project.

## 1. Critical Thinking First

- Verify claims and requirements before implementing
- Question ambiguous instructions
- Research before providing answers

## 2. Code Quality

- Follow CODING_STANDARDS.md strictly
- Never use `@ts-nocheck` or `any` without justification
- Include accessibility from the start, not as afterthought
- Write tests alongside implementation

## 3. Accessibility Non-Negotiables

- Every interactive element must be keyboard accessible
- All form inputs must have visible labels
- Color alone must not convey meaning
- Focus management must be intentional

## 4. No Guesswork

- If uncertain about requirements, ask for clarification
- State assumptions clearly
- Provide sources for technical decisions

## 5. Project Context

**Furucho** is a portfolio and order platform for Roberta Furucho's handmade dolls.

Key requirements:

- Portuguese language UI (Brazilian Portuguese)
- Soft pastel aesthetic (rose, lavender, mint, cream)
- WCAG 2.2 AA accessibility compliance
- Mobile-first responsive design
- WhatsApp integration for orders (common in Brazil)

## 6. Technology Stack

- **Frontend:** React 19, React Router 7, TailwindCSS 4, TypeScript 5
- **Testing:** Vitest, React Testing Library
- **Backend:** Java Spring Boot (separate service)
- **Deployment:** Azure (backend), DigitalOcean/Heroku (frontend)
