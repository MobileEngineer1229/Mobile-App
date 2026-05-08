# API Endpoints List

**Base URL**: `/api/v1`  
**Authentication**: Most endpoints require JWT Bearer token in `Authorization` header  
**Swagger UI**: Available at `/api-docs` when server is running

---

## Authentication & Users (`/api/v1/users`)

### Authentication
- **POST /users/signup** — Register a new user
  - Body: `{ email, password, firstName, lastName, phone? }`
- **POST /users/login** — User login
  - Body: `{ email, password }`
- **POST /users/logout** — Logout user (requires auth)

### Profile Management
- **GET /users/profile** — Get current user profile (requires auth)
- **PUT /users/profile** — Update user profile (requires auth)
  - Body: `{ firstName?, lastName?, phone? }`

### Password Management
- **POST /users/forgot-password** — Request password reset (send OTP to email)
  - Body: `{ email }`
- **POST /users/verify-otp** — Verify OTP code
  - Body: `{ email, otpCode }`
- **POST /users/reset-password** — Reset password with OTP
  - Body: `{ email, otpCode, newPassword }`
- **POST /users/change-password** — Change password (requires auth)
  - Body: `{ currentPassword, newPassword }`

### Account Management
- **POST /users/deactivate** — Deactivate account (requires auth)
- **POST /users/delete** — Delete account (requires auth)

### User Settings
- **GET /users/settings** — Get user settings (requires auth)
- **PUT /users/settings** — Update user settings (requires auth)
  - Body: `{ notifications?, theme?, language? }`

### Notification Preferences
- **GET /users/notifications/preferences** — Get notification preferences (requires auth)
- **PUT /users/notifications/preferences** — Bulk update notification preferences (requires auth)
- **PUT /users/notifications/preferences/:type** — Update single notification preference (requires auth)

### Security Settings
- **GET /users/security/settings** — Get security settings (requires auth)
- **PUT /users/security/settings** — Bulk update security settings (requires auth)
  - Body: `{ settings: [{ type, enabled, metadata? }] }`
- **PUT /users/security/settings/:type** — Update security setting (requires auth)
  - Types: `biometric_id`, `face_id`, `sms_authenticator`, `google_authenticator`

### Additional Settings
- **GET /users/additional-settings** — Get additional settings (requires auth)
- **PUT /users/additional-settings** — Update additional settings (requires auth)

### App Appearance
- **GET /users/app-appearance** — Get app appearance settings (requires auth)
- **PUT /users/app-appearance** — Update app appearance settings (requires auth)

### Data Analytics
- **GET /users/data-analytics** — Get data analytics settings (requires auth)
- **POST /users/data-analytics/download** — Download user data (requires auth)

### Linked Accounts
- **GET /users/linked-accounts** — Get all linked accounts (requires auth)
- **POST /users/linked-accounts/:provider/link** — Link an account (requires auth)
- **POST /users/linked-accounts/:id/unlink** — Unlink an account (requires auth)

---

## Devices (`/api/v1/devices`)

### Device Management
- **GET /devices** — Get all devices for the authenticated user (requires auth)
  - Query params: `page?`, `limit?`, `type?`, `status?`, `search?`, `roomId?`
- **GET /devices/:id** — Get device by ID (requires auth)
- **POST /devices** — Create a new device (requires auth)
  - Body: `{ name, type, macAddress, roomId?, ... }`
- **PUT /devices/:id** — Update device (requires auth)
- **DELETE /devices/:id** — Delete device (requires auth)
- **GET /devices/category/:category** — Get devices by category (requires auth)
  - Categories: `lightning`, `cameras`, `electrical`
  - Query params: `roomId?`

### Device Discovery
- **GET /devices/discover** — Discover nearby devices on the network (requires auth)
  - Returns list of discoverable IoT devices

### Device Types
- **GET /devices/types** — Get device type templates for manual device addition
  - Query params: `category?` (Popular, Lightning, Camera, Electronics)
  - No authentication required

### Device Control
- **POST /devices/:id/control/power** — Control device power (requires auth)
- **POST /devices/:id/control/lamp** — Control smart lamp (requires auth)
- **POST /devices/:id/control/camera** — Control CCTV camera (requires auth)
- **GET /devices/:id/control/camera/stream** — Get camera live stream URL (requires auth)
- **POST /devices/:id/control/speaker** — Control stereo speaker (requires auth)
- **POST /devices/:id/control/ac** — Control air conditioner (requires auth)
- **GET /devices/:id/control/state** — Get device current state (requires auth)
- **POST /devices/:id/command** — Execute device command (unified command interface) (requires auth)

### Device Data (Energy Consumption)
- **POST /devices/:deviceId/energy** — Submit energy consumption data from IoT device (requires auth)
  - Body: `{ consumptionKwh, date, costUsd? }`
- **POST /devices/:deviceId/energy/batch** — Submit multiple energy consumption records (requires auth)
  - Body: `{ records: [{ consumptionKwh, date, costUsd? }] }`
- **POST /devices/webhook/energy** — Webhook endpoint for device energy data (device authentication)
  - Body: `{ deviceToken, consumptionKwh, date, costUsd? }`
  - No user authentication required (uses device token)

---

## Rooms (`/api/v1/rooms`)

- **GET /rooms** — Get all rooms for the authenticated user (requires auth)
  - Query params: `homeId?`
- **GET /rooms/:id** — Get room by ID (requires auth)
- **POST /rooms** — Create a new room (requires auth)
  - Body: `{ name, homeId? }`
- **PUT /rooms/:id** — Update room (requires auth)
  - Body: `{ name? }`
- **DELETE /rooms/:id** — Delete room (requires auth)

---

## Homes (`/api/v1/homes`)

### Home Management
- **GET /homes** — Get all homes for the authenticated user (requires auth)
- **GET /homes/primary** — Get primary home for the authenticated user (requires auth)
- **POST /homes** — Create a new home (requires auth)
  - Body: `{ name, address?, latitude?, longitude?, country?, isPrimary? }`
- **GET /homes/:id** — Get home by ID (requires auth)
- **PUT /homes/:id** — Update home (requires auth)
  - Body: `{ name?, address?, latitude?, longitude?, country?, isPrimary? }`
- **DELETE /homes/:id** — Delete home (requires auth)

### Home Invitations
- **POST /homes/:homeId/invitations** — Create invitation for a home (requires auth)
  - Body: `{ expiresAt?, maxUses? }`
- **GET /homes/invitations/:code** — Get invitation by code (no auth required)
- **POST /homes/invitations/:code/accept** — Accept invitation and join home (requires auth)
- **GET /homes/:homeId/invitations** — Get all invitations for a home (requires auth)
- **POST /homes/invitations/:code/deactivate** — Deactivate invitation (requires auth)

### Home Members
- **GET /homes/:homeId/members** — Get all members of a home (requires auth)
- **GET /homes/:homeId/members/:memberId** — Get member by ID (requires auth)
- **POST /homes/:homeId/members** — Add member to home (requires auth)
  - Body: `{ email, userId?, role? }`
  - Roles: `owner`, `admin`, `member`
- **PUT /homes/:homeId/members/:memberId** — Update member role (requires auth)
  - Body: `{ role }`
- **DELETE /homes/:homeId/members/:memberId** — Remove member from home (requires auth)

---

## Scenes (`/api/v1/scenes`)

- **GET /scenes** — Get all scenes for the authenticated user (requires auth)
  - Query params: `type?` (automation, tap_to_run)
- **GET /scenes/:id** — Get a specific scene by ID (requires auth)
- **POST /scenes** — Create a new scene (requires auth)
  - Body: `{ name, type, conditionLogic?, icon?, color?, homeId?, conditions?, tasks? }`
- **PUT /scenes/:id** — Update a scene (requires auth)
  - Body: `{ name?, conditionLogic?, icon?, color?, isEnabled?, conditions?, tasks? }`
- **DELETE /scenes/:id** — Delete a scene (requires auth)
- **PATCH /scenes/:id/toggle** — Toggle scene enabled/disabled status (requires auth)
- **POST /scenes/:id/execute** — Execute/run a scene (requires auth)
- **GET /scenes/logs** — Get scene execution logs (requires auth)
  - Query params: `limit?`, `offset?`, `sceneId?`
- **POST /scenes/reorder** — Reorder scenes (requires auth)
  - Body: `{ sceneIds: number[], type? }`

---

## Reports (`/api/v1/reports`)

- **GET /reports/monthly-summary** — Get monthly usage summary (requires auth)
  - Returns this month's and previous month's energy consumption and cost
- **GET /reports/statistics** — Get statistics for a date range (requires auth)
  - Query params: `dateRange?`, `startDate?`, `endDate?`
  - Date ranges: `today`, `this_week`, `last_month`, `last_3_months`, `last_6_months`, `this_year`, `last_year`, `all_time`, `custom`
- **GET /reports/devices** — Get device consumption summary (requires auth)
  - Query params: `dateRange?`, `startDate?`, `endDate?`, `deviceId?`, `deviceType?`, `roomId?`, `groupBy?` (device, type, room)
- **GET /reports/devices/:type/details** — Get detailed consumption for a device type (requires auth)
  - Query params: `dateRange?`, `startDate?`, `endDate?`

---

## Notifications (`/api/v1/notifications`)

- **GET /notifications** — Get all notifications (requires auth)
  - Query params: `page?`, `limit?`, `type?`, `category?`, `isRead?`
  - Types: `general`, `security`, `system`, `feature`, `reminder`, `alert`, `info`
  - Categories: `general`, `smart_home`
- **GET /notifications/stats** — Get notification statistics (requires auth)
- **PUT /notifications/read-all** — Mark all notifications as read (requires auth)
- **GET /notifications/:id** — Get notification by ID (requires auth)
- **PUT /notifications/:id/read** — Mark notification as read (requires auth)
- **DELETE /notifications/:id** — Delete notification (requires auth)

---

## Chatbot (`/api/v1/chatbot`)

- **POST /chatbot/message** — Send message to chatbot (requires auth)
  - Body: `{ message, conversationId? }`
- **GET /chatbot/history** — Get chat history (requires auth)
  - Query params: `page?`, `limit?`, `startDate?`, `endDate?`
- **DELETE /chatbot/history** — Clear chat history (requires auth)

---

## Voice Assistants (`/api/v1/voice-assistants`)

- **GET /voice-assistants** — Get all voice assistants with linking status (requires auth)
- **POST /voice-assistants/:id/link** — Link a voice assistant (requires auth)
- **POST /voice-assistants/:id/unlink** — Unlink a voice assistant (requires auth)

---

## User Actions (`/api/v1/user-actions`)

- **GET /user-actions** — Get user action logs (requires auth)
  - Query params: `actionType?`, `actionCategory?`, `endpoint?`, `startDate?`, `endDate?`, `page?`, `limit?`
- **GET /user-actions/statistics** — Get user action statistics (requires auth)
  - Query params: `startDate?`, `endDate?`
- **GET /user-actions/:id** — Get user action by ID (requires auth)

---

## App Version Management (`/api/v1/app`)

- **POST /app/version/check** — Check app version and get update status (no auth required)
  - Body: `{ platform: "android" | "ios", versionName: string, versionCode: number }`
  - Returns version check result with update availability and requirements
- **GET /app/version/:platform** — Get active version for platform (no auth required)
  - Returns current active version information
- **GET /app/versions/:platform** — Get all versions for platform (requires auth)
- **GET /app/versions/:id** — Get version by ID (requires auth)
- **POST /app/versions** — Create new version (requires auth)
  - Body: `{ platform, versionName, versionCode, minimumRequiredVersion, updateUrl?, forceUpdate?, releaseNotes?, isActive? }`
- **PUT /app/versions/:id** — Update version (requires auth)
- **DELETE /app/versions/:id** — Delete version (requires auth)

---

## Health Check

- **GET /health** — Health check endpoint (no auth required)
  - Returns server status and environment information

---

## Notes

- **Authentication**: Most endpoints require JWT Bearer token authentication. Include token in `Authorization` header: `Bearer <token>`
- **Base URL**: All endpoints are prefixed with `/api/v1`
- **Swagger Documentation**: Interactive API documentation available at `/api-docs` when server is running
- **Error Responses**: All endpoints follow consistent error response format with `success`, `error`, and `meta` fields
- **Pagination**: List endpoints support `page` and `limit` query parameters
- **Date Formats**: Use ISO 8601 format for dates (e.g., `2024-12-03` or `2024-12-03T10:30:00Z`)

---

*Last updated: Based on route files in `src/routes/` directory*
