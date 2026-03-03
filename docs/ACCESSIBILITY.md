# Accessibility (WCAG 2.2 AA)

Guidelines for building accessible components in Furucho.
Compliant with **WCAG 2.2 Level AA** (W3C Recommendation, October 2023).

## WCAG 2.2 New Criteria Coverage

| SC     | Name                                 | Level | Status                                          |
| ------ | ------------------------------------ | ----- | ----------------------------------------------- |
| 2.4.11 | Focus Not Obscured (Minimum)         | AA    | ✅ No sticky overlays or modals that hide focus |
| 2.5.7  | Dragging Movements                   | AA    | ✅ N/A — no drag-and-drop UI                    |
| 2.5.8  | Target Size (Minimum)                | AA    | ✅ All interactive targets ≥ 44×44 CSS px       |
| 3.2.6  | Consistent Help                      | A     | ✅ N/A — single-page app                        |
| 3.3.7  | Redundant Entry                      | A     | ✅ N/A — single-step form                       |
| 3.3.8  | Accessible Authentication (Minimum)  | AA    | ✅ N/A — no authentication                      |

**Note:** SC 4.1.1 Parsing was removed in WCAG 2.2 (obsolete).

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

## Target Size (WCAG 2.5.8)

- All interactive elements must be ≥ 24×24 CSS px (we target 44×44 for touch)
- Nav links use `min-h-[44px] min-w-[44px]` with `inline-flex items-center`
- Buttons use generous padding (`py-3` = 12px each side)
- Icons inside links sized ≥ 24×24 (`w-6 h-6`)
- Inline links within sentences are exempt per WCAG 2.5.8 inline exception

## Testing

- Manual: keyboard traversal, screen reader spot-check
- Automated: axe-core in unit tests (jest-axe, axe-core 4.10+)
- E2E: @axe-core/playwright with `wcag2a`, `wcag2aa`, `wcag22aa` tags
- E2E: bounding-box assertions for target size (SC 2.5.8)
- CI: run a11y checks on every PR
