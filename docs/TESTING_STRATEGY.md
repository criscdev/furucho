# Testing Strategy

Goal: Confidence in functionality and accessibility without over-testing.

## Test Pyramid

- **Unit/Component (RTL + Vitest):** Majority of tests — 95 passing
- **Integration (RTL + MSW):** Key user flows (orderFlow.test.tsx)
- **E2E (Playwright):** Happy paths, form submission, keyboard nav, a11y — 42 tests × 3 browsers
- **Backend Unit (JUnit 5 + Mockito + MockMvc):** Controller, Service, Repository layers — 49 passing

## Tools

| Tool | Version | Purpose |
| --- | --- | --- |
| Vitest | 0.32 | Frontend test runner |
| React Testing Library | 14.1.0 | Component testing (role-based queries) |
| userEvent | 14.4.3 | Realistic user interaction simulation |
| MSW | 2.12.10 | API mock server (Node.js) for integration tests |
| jest-axe | 10.0.0 | Component-level a11y audit |
| @vitest/coverage-v8 | 0.32.4 | Code coverage (V8 provider) |
| Playwright | 1.58.2 | E2E browser testing (Chromium, Firefox, Mobile Chrome) |
| @axe-core/playwright | 4.11.1 | E2E a11y audit (3 viewports) |
| JUnit 5 | 5.11 | Backend unit/integration test framework |
| Mockito | 5.x | Backend mock framework |
| Spring MockMvc | — | Controller + filter HTTP testing |
| H2 | 2.3 | In-memory database for backend integration tests |

## Coverage Targets & Baseline (2026-03-03)

| Metric | Target | Actual |
| --- | --- | --- |
| Statements | ≥ 80% | 98.48% |
| Branches | ≥ 80% | 97.61% |
| Functions | ≥ 80% | 88.23% |
| Lines | ≥ 80% | 98.48% |

## Commands

```bash
# Run frontend unit + integration tests
npm test

# Watch mode
npm run test:watch

# Coverage report
npm run test:coverage

# E2E tests (3 browsers)
npm run test:e2e

# Full CI suite (vitest + playwright)
npm run test:ci

# Backend tests
cd backend && ./mvnw test
```

## Test Counts (2026-03-03)

| Suite | Count | Status |
| --- | --- | --- |
| Vitest (unit + integration) | 95 | ✅ |
| Backend (JUnit 5) | 49 | ✅ |
| E2E (Playwright × 3 browsers) | 42 | ✅ |
| **Total** | **186** | ✅ |

## Patterns

### Role-Based Queries (Preferred)

```tsx
// ✅ Good
screen.getByRole('button', { name: /enviar/i })
screen.getByLabelText(/nome/i)

// ❌ Avoid
screen.getByTestId('submit-button')
```

### Accessibility Checks

```tsx
import { axe, toHaveNoViolations } from 'jest-axe';

expect.extend(toHaveNoViolations);

it('has no accessibility violations', async () => {
  const { container } = render(<Component />);
  const results = await axe(container);
  expect(results).toHaveNoViolations();
});
```

### Factory Usage

Use `orderFactory` for consistent test data:

```tsx
import { orderFactory } from '../../test/factories/orderFactory';

const order = orderFactory({ name: 'Custom Name' });
```

## What to Test

- User interactions (click, type, submit)
- Form validation and error states
- Accessibility (keyboard nav, ARIA attributes)
- Loading and error states

## What NOT to Test

- Implementation details (internal state)
- Third-party libraries
- Styling (unless critical to UX)
