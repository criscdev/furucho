# Coding Standards

Guidelines for consistent, maintainable code in Furucho.

## TypeScript

- Enable `strict` mode; no `any` unless justified with comments
- Remove all `@ts-nocheck` directives
- Define `Props` interfaces per component
- Use named exports (avoid default exports)
- Prefer `({ prop }: Props) => JSX.Element` over `React.FC`

## React

- Functional components with hooks only
- Props typed via interfaces
- Explicit accessibility props: `aria-label`, `aria-describedby`
- Use controlled inputs with `useState`
- Memoize only when measured performance benefit exists

## File Conventions

- One component per file
- Component file: `ComponentName.tsx`
- Test file: `ComponentName.test.tsx` (colocated)
- Use named exports matching filename

## Styling

- TailwindCSS utility classes
- CSS custom properties for design tokens in `app.css`
- Visible focus states on all interactive elements

## JSDoc Requirements

Each component must include:

```tsx
/**
 * ComponentName — brief description.
 *
 * A11y:
 * - Keyboard interactions
 * - ARIA roles/attributes used
 *
 * @example
 * <ComponentName prop="value" />
 */
```

## Commits

Use Conventional Commits:

- `feat:` new features
- `fix:` bug fixes
- `docs:` documentation
- `refactor:` code restructuring
- `test:` test additions/changes
- `chore:` maintenance tasks

## Testing

- Test behavior, not implementation
- Use role-based queries: `getByRole`, `getByLabelText`
- Avoid `data-testid` when semantic queries work
- Include axe-core accessibility checks

## Performance

- Lazy load images and below-fold content
- Code-split routes
- Measure before optimizing
