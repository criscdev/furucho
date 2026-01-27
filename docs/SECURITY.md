# Security Documentation

## Overview

The Furucho API implements authentication and authorization to protect sensitive admin endpoints while keeping public endpoints accessible.

## Authentication

### JWT-Based Authentication

The API uses **JSON Web Tokens (JWT)** for stateless authentication:

- **Token expiration**: 1 hour (configurable via `jwt.expiration` property)
- **Signing algorithm**: HMAC-SHA256
- **Token location**: `Authorization` header with `Bearer` prefix

### Admin Login

**Endpoint**: `POST /api/auth/login`

**Request**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response** (Success - 200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (Failure - 401):
```json
{
  "error": "Credenciais inválidas",
  "message": "Usuário ou senha incorretos"
}
```

## Authorization

### Public Endpoints

These endpoints are accessible without authentication:

- `POST /api/orders` - Create a new order
- `POST /api/auth/login` - Admin login
- `GET /api/health/**` - Health check endpoints
- `GET /h2-console/**` - H2 database console (development only)

### Protected Endpoints (Admin Only)

These endpoints require authentication and `ROLE_ADMIN`:

- `GET /api/orders` - List all orders
- `GET /api/orders/{id}` - Get order by ID
- `PATCH /api/orders/{id}/status` - Update order status

### Using Protected Endpoints

Include the JWT token in the `Authorization` header:

```bash
curl -H "Authorization: Bearer YOUR_TOKEN_HERE" \
     https://api.example.com/api/orders
```

## Configuration

### Application Properties

```properties
# JWT Configuration
jwt.secret=your-secret-key-min-256-bits
jwt.expiration=3600000

# Admin Credentials (override via environment variables in production)
admin.username=admin
admin.password=admin123
```

### Environment Variables (Production)

For production deployments, override sensitive values using environment variables:

```bash
export JWT_SECRET="your-production-secret-key-min-256-bits"
export ADMIN_USERNAME="your-admin-username"
export ADMIN_PASSWORD="your-secure-password"
```

## Security Implementation Details

### Technologies Used

- **Spring Security 6.2** - Core security framework
- **JWT (jjwt 0.12.5)** - Token generation and validation
- **BCrypt** - Password hashing

### Security Components

1. **JwtUtil** - Utility class for JWT operations (generation, validation, parsing)
2. **JwtAuthenticationFilter** - Intercepts requests to validate JWT tokens
3. **SecurityConfig** - Configures security rules and authentication
4. **AdminUserDetailsService** - Loads admin user credentials
5. **AuthController** - Handles login requests

### Password Security

- Passwords are hashed using BCrypt (work factor 10)
- Plain text passwords are never stored or logged
- Admin password should be changed in production via environment variables

## Testing

### Unit Tests

Security components are excluded from `@WebMvcTest` tests to avoid circular dependencies:

```java
@WebMvcTest(
    controllers = YourController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
    }
)
```

### Integration Tests

Full integration tests verify:
- Login with valid/invalid credentials
- Access to protected endpoints without token (403 Forbidden)
- Access to protected endpoints with valid token (200 OK)

## Security Best Practices

### Implemented

✅ Stateless authentication (JWT)  
✅ Password hashing (BCrypt)  
✅ Role-based access control (RBAC)  
✅ CORS configuration for known origins  
✅ Rate limiting on order creation  
✅ Input validation on all endpoints  

### Recommended for Production

⚠️ Use environment variables for all secrets  
⚠️ Generate a strong JWT secret key (min 256 bits)  
⚠️ Use HTTPS/TLS for all API communication  
⚠️ Implement token refresh mechanism  
⚠️ Add token revocation/blacklist for logout  
⚠️ Set up security monitoring and logging  
⚠️ Implement rate limiting on login endpoint  
⚠️ Add account lockout after failed login attempts  

## Troubleshooting

### 403 Forbidden

- Check if the JWT token is included in the `Authorization` header
- Verify the token hasn't expired
- Ensure the token is prefixed with `Bearer `

### 401 Unauthorized

- Invalid credentials during login
- JWT secret mismatch between token generation and validation
- Malformed JWT token

### Circular Dependency Error

If you encounter circular dependency issues during development, use `@Lazy` annotation:

```java
public AdminUserDetailsService(@Lazy PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
}
```

## Future Enhancements

- [ ] Token refresh mechanism
- [ ] Role-based permissions (beyond admin/user)
- [ ] OAuth2 integration (Google, GitHub)
- [ ] Multi-factor authentication (MFA)
- [ ] IP-based access restrictions
- [ ] Audit logging for admin actions
