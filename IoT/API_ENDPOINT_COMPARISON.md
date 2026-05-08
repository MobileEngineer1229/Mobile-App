# API Endpoint Comparison Report
## Mobile vs Backend API Endpoints

This document compares all API endpoints between the Android mobile app and the Express.js backend to identify mismatches in request parameters, response types, and missing endpoints.

---

## ❌ CRITICAL ISSUES

### 1. Device ID Type Mismatch
**Issue**: Mobile uses `String` for device IDs, but backend expects `integer`

**Mobile (ApiService.java)**:
```java
@GET("devices/{id}")
Call<ApiResponse<Device>> getDeviceById(@Path("id") String deviceId);

@PUT("devices/{id}")
Call<ApiResponse<Device>> updateDevice(@Path("id") String deviceId, @Body Map<String, Object> deviceData);
```

**Backend (device-routes.ts)**:
```typescript
router.get('/:id', authenticate, deviceController.getDeviceById);  // Expects integer
router.put('/:id', authenticate, validate(updateDeviceValidator), deviceController.updateDevice);  // Expects integer
```

**Fix Required**: Change mobile to use `int` instead of `String`:
```java
@GET("devices/{id}")
Call<ApiResponse<Device>> getDeviceById(@Path("id") int deviceId);

@PUT("devices/{id}")
Call<ApiResponse<Device>> updateDevice(@Path("id") int deviceId, @Body Map<String, Object> deviceData);
```

---

### 2. Scene Order Endpoint Mismatch
**Issue**: Mobile uses `PUT` but backend uses `POST`

**Mobile (ApiService.java)**:
```java
@PUT("scenes/order")
Call<ApiResponse<Void>> updateSceneOrder(@Body Map<String, Object> orderData);
```

**Backend (sceneRoutes.ts)**:
```typescript
router.post('/reorder', sceneController.reorderScenes);  // POST, not PUT
```

**Fix Required**: Change mobile to use `POST`:
```java
@POST("scenes/reorder")  // Note: endpoint is also "reorder" not "order"
Call<ApiResponse<Void>> updateSceneOrder(@Body Map<String, Object> orderData);
```

---

## ⚠️ POTENTIAL ISSUES

### 3. Missing Query Parameters in GET /devices
**Mobile** supports: `roomId`, `homeId`, `status`, `type`, `page`, `limit`
**Backend** supports: `page`, `limit`, `type`, `status`, `search`, `roomId`, `homeId`

**Status**: ✅ **OK** - Mobile has all required params, backend has additional `search` param (optional)

### 4. Missing Query Parameters in GET /notifications
**Mobile** supports: `page`, `limit`, `type`, `category`, `isRead`
**Backend** supports: `page`, `limit`, `type`, `category`, `isRead`

**Status**: ✅ **OK** - All parameters match

### 5. Missing Query Parameters in GET /scenes
**Mobile** supports: `type`, `homeId`
**Backend** supports: `type` only

**Issue**: Mobile sends `homeId` but backend doesn't support it
**Status**: ⚠️ **WARNING** - Backend may ignore `homeId` parameter

### 6. Chatbot History Query Parameters
**Mobile** supports: `page`, `limit`
**Backend** supports: `page`, `limit`, `startDate`, `endDate`

**Status**: ✅ **OK** - Mobile has required params, backend has additional optional params

---

## ✅ VERIFIED MATCHING ENDPOINTS

### Authentication
- ✅ `POST /users/login` - Matches
- ✅ `POST /users/signup` - Matches
- ✅ `POST /users/logout` - Matches
- ✅ `GET /users/profile` - Matches
- ✅ `PUT /users/profile` - Matches

### Password Reset
- ✅ `POST /users/forgot-password` - Matches
- ✅ `POST /users/verify-otp` - Matches
- ✅ `POST /users/reset-password` - Matches
- ✅ `POST /users/change-password` - Matches

### Devices
- ✅ `GET /devices` - Matches (query params verified)
- ✅ `POST /devices` - Matches
- ✅ `DELETE /devices/{id}` - Matches (but see Issue #1 for type)
- ✅ `GET /devices/discover` - Matches
- ✅ `GET /devices/types` - Matches
- ✅ `GET /devices/category/{category}` - Matches

### Device Control
- ✅ `POST /devices/{id}/control/power` - Matches
- ✅ `POST /devices/{id}/control/lamp` - Matches
- ✅ `POST /devices/{id}/control/camera` - Matches
- ✅ `GET /devices/{id}/control/camera/stream` - Matches
- ✅ `POST /devices/{id}/control/speaker` - Matches
- ✅ `POST /devices/{id}/control/ac` - Matches
- ✅ `GET /devices/{id}/control/state` - Matches
- ✅ `POST /devices/{id}/command` - Matches

### Rooms
- ✅ `GET /rooms` - Matches (supports `homeId` query param)
- ✅ `GET /rooms/{id}` - Matches
- ✅ `POST /rooms` - Matches
- ✅ `PUT /rooms/{id}` - Matches
- ✅ `DELETE /rooms/{id}` - Matches

### Homes
- ✅ `GET /homes` - Matches
- ✅ `GET /homes/primary` - Matches
- ✅ `GET /homes/{id}` - Matches
- ✅ `POST /homes` - Matches
- ✅ `PUT /homes/{id}` - Matches
- ✅ `DELETE /homes/{id}` - Matches
- ✅ `POST /homes/join` - Matches
- ✅ `POST /homes/{id}/invitations` - Matches
- ✅ `DELETE /homes/{id}/members/{memberId}` - Matches

### Notifications
- ✅ `GET /notifications` - Matches
- ✅ `GET /notifications/stats` - Matches
- ✅ `GET /notifications/{id}` - Matches
- ✅ `PUT /notifications/{id}/read` - Matches
- ✅ `PUT /notifications/read-all` - Matches
- ✅ `DELETE /notifications/{id}` - Matches

### Scenes
- ✅ `GET /scenes` - Matches (but see Issue #5 for query params)
- ✅ `GET /scenes/{id}` - Matches
- ✅ `POST /scenes` - Matches
- ✅ `PUT /scenes/{id}` - Matches
- ✅ `DELETE /scenes/{id}` - Matches
- ✅ `POST /scenes/{id}/execute` - Matches
- ✅ `GET /scenes/{id}/logs` - Matches

### Chatbot
- ✅ `POST /chatbot/message` - Matches
- ✅ `GET /chatbot/history` - Matches
- ✅ `DELETE /chatbot/history` - Matches

### User Settings
- ✅ `GET /users/settings` - Matches
- ✅ `PUT /users/settings` - Matches
- ✅ `GET /users/notifications/preferences` - Matches
- ✅ `PUT /users/notifications/preferences` - Matches
- ✅ `PUT /users/notifications/preferences/{type}` - Matches
- ✅ `GET /users/security/settings` - Matches
- ✅ `PUT /users/security/settings` - Matches
- ✅ `PUT /users/security/settings/{type}` - Matches
- ✅ `GET /users/additional-settings` - Matches
- ✅ `PUT /users/additional-settings` - Matches
- ✅ `GET /users/data-analytics` - Matches
- ✅ `POST /users/data-analytics/download` - Matches
- ✅ `GET /users/app-appearance` - Matches
- ✅ `PUT /users/app-appearance` - Matches

### Reports
- ✅ `GET /reports/monthly-summary` - Matches
- ✅ `GET /reports/statistics` - Matches
- ✅ `GET /reports/devices` - Matches
- ✅ `GET /reports/devices/{type}/details` - Matches

### Voice Assistants
- ✅ `GET /voice-assistants` - Matches
- ✅ `POST /voice-assistants/{id}/link` - Matches
- ✅ `POST /voice-assistants/{id}/unlink` - Matches

### Linked Accounts
- ✅ `GET /users/linked-accounts` - Matches
- ✅ `POST /users/linked-accounts/{provider}/link` - Matches
- ✅ `POST /users/linked-accounts/{id}/unlink` - Matches

### App Version
- ✅ `POST /app/version/check` - Matches

---

## 📋 MISSING ENDPOINTS IN MOBILE

### Backend has, Mobile doesn't:
1. `GET /devices/home/{homeId}` - Get devices by home ID (backend has this)
2. `PATCH /scenes/{id}/toggle` - Toggle scene enabled/disabled (backend has this)
3. `GET /scenes/logs` - Get all scene logs (backend has this)
4. `GET /app/version/{platform}` - Get active version for platform
5. `GET /app/versions/{platform}` - Get all versions for platform
6. `GET /app/versions/{id}` - Get version by ID
7. `POST /app/versions` - Create version (admin)
8. `PUT /app/versions/{id}` - Update version (admin)
9. `DELETE /app/versions/{id}` - Delete version (admin)
10. `GET /homes/{homeId}/invitations` - Get home invitations
11. `GET /homes/invitations/{code}` - Get invitation by code
12. `POST /homes/invitations/{code}/accept` - Accept invitation by code
13. `GET /homes/{homeId}/members` - Get home members
14. `GET /homes/{homeId}/members/{memberId}` - Get member by ID
15. `POST /homes/{homeId}/members` - Add member
16. `PUT /homes/{homeId}/members/{memberId}` - Update member
17. `POST /devices/{deviceId}/energy` - Submit energy consumption
18. `POST /devices/{deviceId}/energy/batch` - Submit batch energy consumption
19. `POST /devices/webhook/energy` - Webhook for energy data

---

## 📋 MISSING ENDPOINTS IN BACKEND

### Mobile has, Backend doesn't:
None found - All mobile endpoints exist in backend (with the exception of the mismatches noted above)

---

## 🔧 RECOMMENDED FIXES

### Priority 1 (Critical - Breaks Functionality)
1. **Fix Device ID Type**: Change `String` to `int` in mobile for:
   - `getDeviceById`
   - `updateDevice`

### Priority 2 (Important - Wrong HTTP Method/Path)
2. **Fix Scene Order Endpoint**: 
   - Change from `PUT scenes/order` to `POST scenes/reorder` in mobile

### Priority 3 (Optional - Feature Enhancement)
3. **Add Missing Endpoints to Mobile** (if needed):
   - Scene toggle endpoint
   - Home member management endpoints
   - Energy consumption endpoints
   - App version management endpoints

4. **Backend Enhancement** (if needed):
   - Add `homeId` query parameter support to `GET /scenes`

---

## 📝 NOTES

- Most endpoints are correctly aligned between mobile and backend
- Response types appear to match (all use `ApiResponse<T>` wrapper)
- Authentication requirements match (all protected endpoints require auth)
- Query parameters are mostly aligned, with backend having some additional optional params

---

**Generated**: $(date)
**Last Updated**: Review both codebases before making changes
