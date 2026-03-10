# Error Journal

A log of mistakes and learnings to prevent repeating them.

---

## 2026-01-27: GitHub Actions Environment Validation

**What went wrong:**
Used `environment: production` as a simple string value in GitHub Actions workflows. The GitHub Actions schema expects environment to be either a string that matches a pre-defined GitHub Environment in the repository settings, OR an object with `name` and optionally `url` properties.

**The error:**

```text
Value 'production' is not valid
```

**How I fixed it:**
Changed from simple string to object syntax:

```yaml
environment:
  name: production
```

**Lesson learned:**
When using GitHub Environments, always check that the environment is created in GitHub repository Settings → Environments. Use the object syntax `{name, url}` for proper schema validation. Remember that secrets referenced with `${{ secrets.* }}` must be configured in repository settings.

---

## 2026-01-27: Missing @NonNull Annotation on Override Methods

**What went wrong:**
When overriding Spring's `WebMvcConfigurer.addCorsMappings()` method, I didn't include the `@NonNull` annotation that the parent interface specifies.

**The error:**

```text
Missing non-null annotation: inherited method from WebMvcConfigurer specifies this parameter as @NonNull
```

**How I fixed it:**
Added import `import org.springframework.lang.NonNull;` and added annotation to parameter: `public void addCorsMappings(@NonNull CorsRegistry registry)`

**Lesson learned:**
When overriding methods from Spring interfaces, check if the parent method has null-safety annotations. Always propagate `@NonNull` / `@Nullable` annotations to overriding methods. Use `org.springframework.lang.NonNull` (not javax or jakarta) for Spring projects.

---

## 2026-01-27: Null-Safety Warnings with Spring Data JPA and MockMvc

**What went wrong:**
Eclipse's null-analysis flagged warnings for `orderRepository.findById(id)` where the `id` parameter needs unchecked conversion, and `MediaType.APPLICATION_JSON` needs unchecked conversion. These are false positives from strict null-analysis interacting with libraries that don't have complete null annotations.

**The errors:**

```text
Null type safety: The expression of type 'Long' needs unchecked conversion to conform to '@NonNull Long'
Null type safety: The expression of type 'MediaType' needs unchecked conversion to conform to '@NonNull MediaType'
```

**How I fixed it:**
Added `@SuppressWarnings("null")` at the class level for `OrderService.java` and `OrderControllerTest.java`.

**Lesson learned:**
Not all library code is fully null-annotated. Use `@SuppressWarnings("null")` judiciously when you know the values aren't actually null. Consider project-wide null-analysis configuration if too many false positives occur. Alternative: wrap with `Objects.requireNonNull()` for explicit null checks.

---

## 2026-01-27: Premature Deployment Job Configuration

**What went wrong:**
Added deployment jobs to GitHub Actions workflows before the infrastructure was set up. This caused schema validation errors for undefined secrets and environments.

**The error:**

```text
Value 'production' is not valid
Context access might be invalid: AZURE_CREDENTIALS
```

**How I fixed it:**
Commented out the deploy jobs with clear documentation of prerequisites. The jobs can be uncommented once secrets are configured.

**Lesson learned:**
Don't add deployment workflows until the actual infrastructure is ready. Either keep deployment jobs commented with clear prerequisites, use workflow dispatch for manual deployment triggers, or set up infrastructure first then add CI/CD.

---

## 2026-01-27: Import Paths in Nested Test Directories

**What went wrong:**
Created integration tests in `src/test/integration/` but used wrong relative import paths. Forgot that moving deeper into a directory requires additional `../` in the path.

**The error:**

```text
Error: Failed to load url ../component/OrderForm/OrderForm (resolved id: ../component/OrderForm/OrderForm)
```

**How I fixed it:**
Changed imports from `../component/...` to `../../component/...` since the test file is 2 levels deep from `src/`.

**Lesson learned:**
When creating files in nested directories, count the depth from the source root. `src/test/integration/` is 2 levels deep, so imports to `src/component/` need `../../component/`.

---

## 2026-01-27: Non-Unique Element Queries in Integration Tests

**What went wrong:**
Used `getByRole('link', { name: /instagram/i })` in a full-page test that renders both Header and Gallery - but both components have Instagram links, causing a "multiple elements found" error.

**The error:**

```text
TestingLibraryElementError: Found multiple elements with the role "link" and name /instagram/i
```

**How I fixed it:**
Two approaches:
1. Test components in isolation when checking specific behavior
2. Use more specific queries like `getByRole('link', { name: /instagram de roberta furucho/i })` which matches the aria-label

**Lesson learned:**
Integration tests that render multiple components may have duplicate interactive elements. Use specific aria-labels or test components separately for focused behavior tests.

---

## Template for Future Entries

```markdown
## YYYY-MM-DD: Brief Title

**What went wrong:**
Description of the mistake.

**The error:**
Error message or symptoms in a code block.

**How I fixed it:**
Steps taken to resolve.

**Lesson learned:**
Key takeaways to prevent recurrence.
```

---

## 2026-03-03: JPA Optional Return Type with ORDER BY Returns Multiple Rows

**What went wrong:**
`ConversationRepository.findActiveByWaId()` and `findLastCompletedByWaId()` used JPQL queries with `ORDER BY` returning `Optional<ConversationState>`. Spring Data JPA internally calls `getSingleResult()` for `Optional<T>` return types. When multiple rows match (returning customers with multiple completed conversations, or race condition creating duplicate active rows), it throws `IncorrectResultSizeDataAccessException`.

**The error:**

```text
org.springframework.dao.IncorrectResultSizeDataAccessException:
  Query did not return a unique result: 3 results were returned
```

**How I fixed it:**
Changed both queries from JPQL to native SQL with `LIMIT 1`:

```java
@Query(value = "SELECT * FROM conversation_states WHERE wa_id = :waId " +
       "AND current_step = 'COMPLETED' ORDER BY completed_at DESC LIMIT 1",
       nativeQuery = true)
Optional<ConversationState> findLastCompletedByWaId(@Param("waId") String waId);
```

**Lesson learned:**
Never use `Optional<T>` as a return type for JPA queries with `ORDER BY` that can match multiple rows. Either use native query with `LIMIT 1`, return `List<T>` and take first, or use Spring Data's `findFirstBy...OrderBy...` derived query method naming.

---

## 2026-03-03: WebFlux subscribe() Without Error Consumer

**What went wrong:**
`WhatsAppClient.sendMessage()` used `.subscribe()` with no arguments after a Reactor chain. The `.doOnError()` operator is a side-effect — it logs but doesn't consume the error. When the HTTP call fails, the error propagates to `subscribe()` which has no error consumer, causing `reactor.core.Exceptions$ErrorCallbackNotImplemented` on the async reactor thread.

**The error:**

```text
reactor.core.Exceptions$ErrorCallbackNotImplemented:
  org.springframework.web.reactive.function.client.WebClientResponseException$InternalServerError
```

**How I fixed it:**
Added error consumer lambda to subscribe:

```java
.subscribe(
    resp -> {},
    err -> log.error("WhatsApp API call failed: {}", err.getMessage())
);
```

**Lesson learned:**
Always provide an error consumer when calling `.subscribe()` on a Mono/Flux. The `doOnError()` operator is for side-effects only — it does NOT consume the error signal. Without an error consumer, errors crash the reactor thread silently. Consider using `.onErrorResume()` for more sophisticated error handling.

---

## 2026-03-03: Mockito Strictness.LENIENT Hides False-Positive Tests

**What went wrong:**
`ConversationServiceTest` used `@MockitoSettings(strictness = Strictness.LENIENT)` which suppressed all warnings about unnecessary stubbings. This hid several problems: dead `stubSave()` calls, duplicate when/thenReturn setups, and the `confirmCreatesOrder` test using `any()` matcher — which meant the test would pass even if all order fields were mapped incorrectly.

**The error:**

No visible error — that's the problem. Tests passed with false confidence.

**How I fixed it:**
Removed `Strictness.LENIENT`, then fixed each strict-stubbing violation exposed:
- Removed dead `stateAt()` call in `sendsGreeting`
- Removed duplicate `stubSave()` calls
- Replaced `any(CreateOrderRequest.class)` with `ArgumentCaptor` to verify actual field values

**Lesson learned:**
Never use `Strictness.LENIENT` at the class level as a convenience shortcut. It defeats Mockito's built-in safety net against stale test code. If a specific test needs LENIENT, annotate just that test method with `@MockitoSettings`. Use `ArgumentCaptor` instead of `any()` when the argument values matter — which is almost always the case for data-mapping tests.
