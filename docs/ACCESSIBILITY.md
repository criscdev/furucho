# Accessibility (WCAG 2.2 AA)

Guidelines for building accessible components in Furucho.

## Target Users (Reality Context)

Furucho is a custom doll ordering platform for Roberta's handmade doll business.

- **Primary:** Parents/grandparents ordering custom dolls (often less tech-savvy)
- **Secondary:** Gift buyers for special occasions
- **Key needs:** Mobile-friendly, WhatsApp integration, Portuguese language (Brazil)

## Core Requirements

- Use semantic HTML: `<header>`, `<nav>`, `<main>`, `<footer>`, `<button>`, `<a>`
- Headings in order: one `<h1>` per page, sequential levels
- Keyboard navigation: all interactive elements reachable via Tab
- Visible focus rings using `--color-focus` token (3:1+ contrast)
- Color contrast: text ≥ 4.5:1, UI elements ≥ 3:1

## Forms

- Labels tied via `for`/`id` attributes
- Error messages linked with `aria-describedby`
- Required fields marked with `aria-required="true"`
- Group related fields with `<fieldset>` and `<legend>`

## Motion & Animations

- Respect `prefers-reduced-motion` media query
- Avoid auto-playing animations
- Keep transitions subtle (≤300ms)

## Live Regions

- Use `aria-live="polite"` for async updates (form success/error)
- Avoid `aria-live="assertive"` unless critical

## Images

- Decorative images: `alt=""`
- Meaningful images: descriptive `alt` text
- Gallery images: include doll description in alt

## Skip Link

- First focusable element in Header
- Links to `#main` landmark
- Visible on focus

## Testing

- Manual: keyboard traversal, screen reader spot-check
- Automated: axe-core checks in unit tests
- CI: run a11y checks on every PR
