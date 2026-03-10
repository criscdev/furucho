# Roberta Furucho - Bonecas Artesanais 🧸

Portfolio and order platform for Roberta Furucho's handmade dolls (*bonecas artesanais*).

## 🌸 About

This platform showcases Roberta Furucho's handmade doll creations and provides an easy way for customers to request custom orders via WhatsApp.

**Live Demo:** Coming soon at robertafurucho.me

## ✨ Features

### Frontend

- 🎨 **Pastel Design System** - Rose, lavender, mint, and cream colors
- ♿ **Fully Accessible** - WCAG 2.2 AA compliant
- 📱 **Responsive** - Mobile-first design
- 💬 **WhatsApp Integration** - One-click order submission
- 🖼️ **Gallery** - Showcase handmade creations
- 🔍 **SEO Optimized** - Open Graph meta tags

### Backend (Java)

- ☕ **Spring Boot 3.4** - Modern Java 17 backend
- 🗃️ **JPA/Hibernate** - H2 (dev) / PostgreSQL (prod)
- ✅ **Validation** - Jakarta Bean Validation
- 🚦 **Rate Limiting** - Bucket4j protection
- 🔐 **Authentication** - JWT + Spring Security
- 📝 **REST API** - Order management endpoints

### DevOps

- 🔄 **CI/CD** - GitHub Actions workflows
- 🐳 **Docker** - Containerized deployment
- ☁️ **Cloud Ready** - Azure + DigitalOcean configs

## 🚀 Getting Started

### Prerequisites

- Node.js 20+
- Java 17+ (for backend)
- Maven 3.9+ (for backend)

### Frontend Development

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

Application available at `http://localhost:5173`

### Backend Development

```bash
cd backend

# Run with Maven wrapper
./mvnw spring-boot:run
```

API available at `http://localhost:8080`

### Running Tests

```bash
# Frontend tests
npm test

# Backend tests
cd backend && ./mvnw test
```

## 📁 Project Structure

```text
furucho/
├── app/                    # React Router app routes
│   ├── app.css            # Global styles + design tokens
│   ├── root.tsx           # Root layout
│   ├── routes/            # Route components
│   └── welcome/           # Landing page
├── src/
│   ├── component/         # Reusable components
│   │   ├── Header/        # Site header
│   │   ├── OrderForm/     # Order request form
│   │   └── Gallery/       # Image gallery
│   └── test/              # Test utilities & factories
├── backend/               # Java Spring Boot API
│   ├── src/main/java/     # Application code
│   └── src/test/java/     # Tests
├── docs/                  # Documentation
│   ├── ACCESSIBILITY.md   # A11y guidelines
│   ├── CODING_STANDARDS.md
│   ├── DEPLOYMENT.md      # Deployment guide
│   └── ...
└── .github/workflows/     # CI/CD pipelines
```

## 🎨 Design System

| Token               | Color     | Usage            |
| ------------------- | --------- | ---------------- |
| `--color-rose`      | `#F4B8C5` | Primary accent   |
| `--color-lavender`  | `#D8D0E8` | Secondary accent |
| `--color-mint`      | `#B8E0C8` | Success states   |
| `--color-cream`     | `#FFFAF5` | Backgrounds      |

## 📚 Documentation

- [Security Guidelines](docs/SECURITY.md)
- [Accessibility Guidelines](docs/ACCESSIBILITY.md)
- [Coding Standards](docs/CODING_STANDARDS.md)
- [Component Development](docs/COMPONENT_DEVELOPMENT.md)
- [Testing Strategy](docs/TESTING_STRATEGY.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
- [Dev Journal](docs/DEV_JOURNAL.md)

## 🛠️ Tech Stack

### Frontend Technologies

- React 19 + React Router 7
- TypeScript 5
- TailwindCSS 4
- Vite 6
- Vitest + React Testing Library

### Backend Technologies

- Java 17
- Spring Boot 3.4
- Spring Data JPA
- H2 / PostgreSQL
- Bucket4j (rate limiting)

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

## 👩‍🎨 Credits

Handmade dolls by **Roberta Furucho**  
📸 Instagram: [@robertafurucho1](https://instagram.com/robertafurucho1)

---

Feito com 💜 no Brasil 🇧🇷
