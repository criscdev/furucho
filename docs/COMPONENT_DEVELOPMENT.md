# Component Development Guide

Step-by-step process for building accessible, typed components.

## 1. Define Purpose & Accessibility

- Describe user goal and interaction model
- Choose native semantic elements first
- Identify ARIA attributes only if native semantics insufficient

## 2. TypeScript Props

```tsx
export interface Props {
  /** Label shown on the button */
  label: string;
  /** Click handler */
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void;
  /** Visual style variant */
  variant?: 'primary' | 'secondary';
}
```

## 3. JSDoc Documentation

```tsx
/**
 * Button — accessible action control.
 *
 * A11y:
 * - Uses native <button> semantics
 * - Visible focus ring via tokens
 * - Keyboard: Enter/Space trigger
 *
 * @example
 * <Button label="Enviar" onClick={handleSubmit} />
 */
```

## 4. Implementation

```tsx
export function Button({ label, onClick, variant = 'primary' }: Props) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`btn btn-${variant}`}
    >
      {label}
    </button>
  );
}
```

## 5. Accessibility Checklist

- [ ] Keyboard operable (Tab, Enter, Space)
- [ ] Visible focus ring
- [ ] Sufficient color contrast
- [ ] ARIA labels where needed
- [ ] Respects `prefers-reduced-motion`

## 6. Testing

```tsx
describe('Button', () => {
  it('calls onClick when clicked', async () => {
    const handleClick = vi.fn();
    render(<Button label="Test" onClick={handleClick} />);
    
    await userEvent.click(screen.getByRole('button', { name: /test/i }));
    
    expect(handleClick).toHaveBeenCalledOnce();
  });

  it('has no accessibility violations', async () => {
    const { container } = render(<Button label="Test" />);
    expect(await axe(container)).toHaveNoViolations();
  });
});
```

## 7. Component Folder Structure

```text
src/component/
└── Button/
    ├── Button.tsx        # Component implementation
    └── Button.test.tsx   # Tests
```

## Form Component Specifics

For form inputs, always include:

- `<label>` with `htmlFor` linking to input `id`
- Error message `<span>` with unique id
- Input `aria-describedby` pointing to error span
- `aria-invalid="true"` when validation fails
