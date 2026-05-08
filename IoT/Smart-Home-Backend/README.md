# Smart Home Backend

Express.js + PostgreSQL + TypeScript backend for IoT smart home system.

## 📋 Table of Contents

- [Quick Setup](#quick-setup)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Database Migrations](#database-migrations)
- [Development](#development)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
- [Logging](#logging)

## Quick Setup

1. **Run setup script:**
   ```powershell
   .\setup.ps1
   ```

2. **Create `.env` file:**
   ```
   NODE_ENV=development
   PORT=3003
   DB_HOST=localhost
   DB_PORT=5432
   DB_USER=postgres
   DB_PASSWORD=213515
   DB_NAME=smart_home_db
   JWT_SECRET=your-super-secret-jwt-key-change-in-production-min-32-chars
   JWT_EXPIRES_IN=7d
   API_VERSION=v1
   AUTO_MIGRATE=true
   ```

3. **Setup database:**
   ```bash
   psql -U postgres -d smart_home_db -f database/schema.sql
   ```
   
   Or create the database first:
   ```bash
   createdb -U postgres smart_home_db
   psql -U postgres -d smart_home_db -f database/schema.sql
   ```
   
   **Note:** The backend will automatically run migrations on startup (if `AUTO_MIGRATE=true`). 
   You only need to run the initial `schema.sql` manually. Subsequent migrations will be applied automatically.

4. **Run project:**
   ```bash
   npm run dev
   ```

## Development

```bash
npm run dev
```

Server runs on `http://localhost:3003`

## API Documentation

### Swagger UI
Interactive API documentation available at:
- **Local**: http://localhost:3003/api-docs
- **Features**:
  - Browse all endpoints
  - Test API calls directly
  - View request/response schemas
  - See authentication requirements

### API Base URL
- **Development**: `http://localhost:3003/api/v1`
- **Production**: Configure via environment variables

## API Endpoints

### Authentication & Users

- `POST /api/v1/users/signup` - Register a new user
- `POST /api/v1/users/login` - Login user
- `GET /api/v1/users/profile` - Get current user profile (requires authentication)
- `PUT /api/v1/users/profile` - Update user profile (requires authentication)

### Devices

- `GET /api/v1/devices` - Get all devices (requires authentication)
  - Query params: `page`, `limit`, `type`, `status`, `search`, `roomId`
- `GET /api/v1/devices/:id` - Get device by ID (requires authentication)
- `POST /api/v1/devices` - Create new device (requires authentication)
- `PUT /api/v1/devices/:id` - Update device (requires authentication)
- `DELETE /api/v1/devices/:id` - Delete device (requires authentication)
- `GET /api/v1/devices/discover` - Discover nearby devices
- `GET /api/v1/devices/types` - Get device type templates

### Rooms

- `GET /api/v1/rooms` - Get all rooms (requires authentication)
  - Query param: `homeId` (optional) - Filter by home
- `GET /api/v1/rooms/:id` - Get room by ID (requires authentication)
- `POST /api/v1/rooms` - Create room (requires authentication)
- `PUT /api/v1/rooms/:id` - Update room (requires authentication)
- `DELETE /api/v1/rooms/:id` - Delete room (requires authentication)

### Homes

- `GET /api/v1/homes` - Get all homes for authenticated user
- `GET /api/v1/homes/primary` - Get primary home
- `GET /api/v1/homes/:id` - Get home by ID
- `POST /api/v1/homes` - Create home
- `PUT /api/v1/homes/:id` - Update home
- `DELETE /api/v1/homes/:id` - Delete home

### Notifications

- `GET /api/v1/notifications` - Get notifications
  - Query param: `category` (optional) - Filter by `general` or `smart_home`
- `GET /api/v1/notifications/stats` - Get notification statistics
- `GET /api/v1/notifications/:id` - Get notification by ID
- `PUT /api/v1/notifications/:id/read` - Mark notification as read
- `PUT /api/v1/notifications/read-all` - Mark all as read
- `DELETE /api/v1/notifications/:id` - Delete notification

### Chatbot

- `POST /api/v1/chatbot/message` - Send message to chatbot
- `GET /api/v1/chatbot/history` - Get chat history
- `DELETE /api/v1/chatbot/history` - Clear chat history

### User Settings

- `GET /api/v1/users/notifications/preferences` - Get notification preferences
- `PUT /api/v1/users/notifications/preferences` - Update notification preferences
- `GET /api/v1/users/security/settings` - Get security settings
- `PUT /api/v1/users/security/settings` - Update security settings
- `POST /api/v1/users/change-password` - Change password

## Authentication

Most endpoints require JWT authentication. Include the token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## Project Structure

```
src/
├── config/          # Configuration files
├── controllers/     # Request handlers
├── services/        # Business logic layer
├── repositories/    # Data access layer
├── models/          # Data models and interfaces
├── middleware/      # Custom middleware
├── routes/          # Route definitions
├── utils/           # Utility functions
├── types/           # TypeScript type definitions
├── validators/      # Input validation schemas
├── swagger/         # Swagger/OpenAPI documentation
└── app.ts           # Application entry point
```

## Technologies

- **Express.js** - Web framework
- **PostgreSQL** - Database
- **TypeScript** - Type safety
- **JWT** - Authentication
- **Swagger/OpenAPI** - API documentation
- **Winston** - Enhanced logging with emoji support
- **Express Validator** - Input validation

## Database Migrations

The backend automatically runs database migrations on startup by default. This ensures your database schema is always up-to-date.

### Automatic Migrations (Default)
- Migrations run automatically when the server starts
- Only pending migrations are executed
- Migration history is tracked in the `migrations` table
- Set `AUTO_MIGRATE=false` in `.env` to disable automatic migrations

### Manual Migrations
If you prefer to run migrations manually:
```bash
# Disable automatic migrations in .env
AUTO_MIGRATE=false

# Run migrations manually
psql -U postgres -d smart_home_db -f database/migration_add_rooms.sql
```

### Adding New Migrations
1. Create a new SQL file in `database/` directory (e.g., `migration_add_feature.sql`)
2. Add the migration to the `migrations` array in `src/utils/migrations.ts`
3. The migration will run automatically on next server start

## Logging

Enhanced logging with emoji support. Logs saved to `logs/` directory.
See [docs/LOGGING.md](docs/LOGGING.md) for details.

## 🔒 Security

- JWT token authentication
- Password hashing with bcryptjs
- Input validation with express-validator
- SQL injection prevention (parameterized queries)
- CORS configuration
- Environment variable management

## 🧪 Testing

### Manual Testing
- Use Swagger UI to test endpoints
- Use Postman or similar tools
- Test with mobile app

### Test Scripts
Utility scripts in `scripts/` directory:
- `create-test-user.js` - Create test user
- `reset-password.js` - Reset user password
- `insert-sample-energy-data.js` - Insert sample data

## 📊 Database Schema

### Main Tables
- `users` - User accounts
- `devices` - IoT devices
- `rooms` - Rooms in homes
- `homes` - User homes
- `notifications` - User notifications
- `chatbot_messages` - Chatbot conversation history
- `user_notification_preferences` - Notification settings
- `user_security_settings` - Security settings
- `user_profile_metadata` - Profile metadata
- `user_additional_settings` - Additional user settings

### Relationships
- Users → Homes (one-to-many)
- Homes → Rooms (one-to-many)
- Rooms → Devices (one-to-many)
- Users → Notifications (one-to-many)
- Users → Chatbot Messages (one-to-many)

## 🐛 Troubleshooting

### Database Connection Issues
- Check PostgreSQL is running
- Verify `.env` configuration
- Check database exists: `psql -U postgres -l`

### Migration Issues
- Check `migrations` table for failed migrations
- Review migration SQL files
- Set `AUTO_MIGRATE=false` to debug

### Port Already in Use
- Change `PORT` in `.env`
- Or kill process using port 3003

## 📝 License

ISC

