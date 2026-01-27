# Testing Strategy

Goal: Confidence in functionality and accessibility without over-testing.

## Test Pyramid

- **Unit/Component (RTL):** Majority of tests
- **Integration (RTL + MSW):** Key user flows
- **E2E (Playwright):** Happy paths, form submission, navigation

## Tools

- **Vitest:** Test runner
- **React Testing Library:** Component testing
- **MSW:** Network mocking (when backend integration added)
- **axe-core:** Accessibility testing via `@axe-core/react` or `jest-axe`

## Coverage Targets

- Lines: ≥ 80%
- Critical components (Header, OrderForm): ≥ 90%

## Commands

```bash
# Run all tests
npm test

# Watch mode
npm run test:watch
```

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
