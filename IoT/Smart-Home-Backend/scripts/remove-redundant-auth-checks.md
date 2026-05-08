# Removing Redundant Authentication Checks

## Why Remove These Checks?

1. **The `authenticate` middleware already handles authentication**
   - It verifies the JWT token
   - It sets `req.user` if authentication succeeds
   - It throws `UnauthorizedError` if authentication fails
   - Controllers never execute if authentication fails

2. **Single Responsibility Principle (SRP)**
   - Authentication is middleware's responsibility
   - Controllers should focus on request/response handling

3. **DRY Principle (Don't Repeat Yourself)**
   - 47+ redundant checks across controllers
   - One source of truth: the middleware

4. **TypeScript Safety**
   - Use `req.user!` with non-null assertion
   - The middleware guarantees `req.user` exists when controller runs

## Pattern to Remove

```typescript
// ❌ REMOVE THIS
if (!req.user) {
  sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
  return;
}

// ✅ USE THIS INSTEAD
// Just use req.user!.id directly
// The middleware guarantees req.user exists
```
