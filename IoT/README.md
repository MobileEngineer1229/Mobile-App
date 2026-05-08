# Smart Home IoT Application

A comprehensive IoT smart home management system with Android mobile app and Express.js backend.

## 📱 Project Overview

This project consists of two main components:

1. **Android Mobile App** (`Smart-Home/`) - Native Android application for controlling and managing IoT devices
2. **Express.js Backend** (`Smart-Home-Backend/`) - RESTful API backend with PostgreSQL database

## 🏗️ Architecture

### Backend
- **Framework**: Express.js 4.18.2
- **Language**: TypeScript 5.3.3
- **Database**: PostgreSQL
- **Authentication**: JWT (JSON Web Tokens)
- **API Documentation**: Swagger/OpenAPI
- **Architecture Pattern**: Layered (Controllers → Services → Repositories → Database)

### Mobile App
- **Language**: Java 8
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 33 (Android 13)
- **Networking**: Retrofit 2.9.0
- **UI**: Material Design Components
- **Architecture**: Activity-based

## 🚀 Quick Start

### Prerequisites
- Node.js 18.18.0+
- PostgreSQL 12+
- Android Studio (for mobile app)
- Java JDK 8+

### Backend Setup

1. Navigate to backend directory:
   ```bash
   cd Smart-Home-Backend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Create `.env` file:
   ```env
   NODE_ENV=development
   PORT=3003
   DB_HOST=localhost
   DB_PORT=5432
   DB_USER=postgres
   DB_PASSWORD=your_password
   DB_NAME=smart_home_db
   JWT_SECRET=your-super-secret-jwt-key-change-in-production-min-32-chars
   JWT_EXPIRES_IN=7d
   API_VERSION=v1
   AUTO_MIGRATE=true
   ```

4. Setup database:
   ```bash
   createdb -U postgres smart_home_db
   psql -U postgres -d smart_home_db -f database/schema.sql
   ```

5. Start development server:
   ```bash
   npm run dev
   ```

6. Access Swagger UI:
   ```
   http://localhost:3003/api-docs
   ```

### Mobile App Setup

1. Open `Smart-Home/` in Android Studio
2. Sync Gradle files
3. Update API base URL in `ApiClient.java` if needed:
   - Emulator: `http://10.0.2.2:3003`
   - Physical device: `http://<your-local-ip>:3003`
4. Run on emulator or physical device

## 📚 Documentation

### Backend Documentation
- [Backend README](Smart-Home-Backend/README.md) - Setup, API endpoints, and development guide
- [Swagger UI](http://localhost:3003/api-docs) - Interactive API documentation
- [Logging Guide](Smart-Home-Backend/docs/LOGGING.md) - Logging configuration and usage

### Mobile App Documentation
- [Mobile App README](Smart-Home/MOCK_DATA_SETUP.md) - Setup, build instructions, and mock data guide

## 🔑 Demo User

For testing the mobile app without backend:

- **Email**: `demo@smartify.com`
- **Password**: `demo123456`

The app automatically uses mock data when logged in with this account.

## 🛠️ Development

### Adding New Features

1. **Backend**:
   - Add route in `src/routes/`
   - Add controller in `src/controllers/`
   - Add service in `src/services/`
   - Add repository in `src/repositories/`
   - Add validation in `src/validators/`
   - Document in Swagger
   - Create migration if needed

2. **Mobile App**:
   - Add API interface method in `network/ApiService.java`
   - Create/update models
   - Update UI components
   - Add mock data if needed in `utils/MockDataProvider.java`

### Database Migrations

Migrations run automatically on server start (if `AUTO_MIGRATE=true`). To add a new migration:

1. Create SQL file in `database/` directory
2. Add to `migrations` array in `src/utils/migrations.ts`
3. Migration will run automatically on next server start

## 📁 Project Structure

```
IoT/
├── Smart-Home/                    # Android Mobile App
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/              # Java source code
│   │   │   └── res/               # Android resources
│   │   └── build.gradle
│   └── build.gradle
│
└── Smart-Home-Backend/            # Express.js Backend
    ├── src/
    │   ├── config/                # Configuration
    │   ├── controllers/           # Request handlers
    │   ├── services/              # Business logic
    │   ├── repositories/          # Data access
    │   ├── models/                # Data models
    │   ├── routes/                # Route definitions
    │   ├── middleware/            # Custom middleware
    │   ├── validators/            # Input validation
    │   ├── utils/                 # Utilities
    │   └── swagger/               # API documentation
    ├── database/                  # Database schema & migrations
    ├── scripts/                   # Utility scripts
    └── package.json
```

## 🔐 Security

- JWT token authentication
- Password hashing with bcryptjs
- Input validation and sanitization
- SQL injection prevention (parameterized queries)
- CORS configuration
- Secure token storage (consider Android Keystore for production)

## 🧪 Testing

### Backend
- Unit tests for services
- Integration tests for API endpoints
- Test error scenarios

### Mobile App
- Unit tests for business logic
- Integration tests for API calls
- UI tests for critical flows

## 📝 API Endpoints

### Authentication
- `POST /api/v1/users/signup` - Register new user
- `POST /api/v1/users/login` - Login user
- `GET /api/v1/users/profile` - Get user profile
- `PUT /api/v1/users/profile` - Update user profile

### Devices
- `GET /api/v1/devices` - Get all devices
- `GET /api/v1/devices/:id` - Get device by ID
- `POST /api/v1/devices` - Create device
- `PUT /api/v1/devices/:id` - Update device
- `DELETE /api/v1/devices/:id` - Delete device

### Rooms
- `GET /api/v1/rooms` - Get all rooms (filter by homeId)
- `GET /api/v1/rooms/:id` - Get room by ID
- `POST /api/v1/rooms` - Create room
- `PUT /api/v1/rooms/:id` - Update room
- `DELETE /api/v1/rooms/:id` - Delete room

### Homes
- `GET /api/v1/homes` - Get all homes for user
- `GET /api/v1/homes/:id` - Get home by ID
- `POST /api/v1/homes` - Create home
- `PUT /api/v1/homes/:id` - Update home
- `DELETE /api/v1/homes/:id` - Delete home

### Notifications
- `GET /api/v1/notifications` - Get notifications (filter by category)
- `GET /api/v1/notifications/stats` - Get notification statistics
- `PUT /api/v1/notifications/:id/read` - Mark as read
- `DELETE /api/v1/notifications/:id` - Delete notification

### Chatbot
- `POST /api/v1/chatbot/message` - Send message
- `GET /api/v1/chatbot/history` - Get chat history
- `DELETE /api/v1/chatbot/history` - Clear chat history

For complete API documentation, see [Swagger UI](http://localhost:3003/api-docs).

## 🎨 UI Design

- **Theme**: Dark mode
- **Design System**: Material Design
- **Colors**: 
  - Primary: `#5B42F3` (Purple-blue)
  - Background: `#1F222A` (Dark)
  - Cards: `#262A35` (Dark grey)

## 📄 License

ISC

## 🤝 Contributing

1. Follow the code standards in `.cursorrules`
2. Update documentation when adding features
3. Test thoroughly before submitting
4. Keep API contracts in sync between frontend and backend

## 📞 Support

For issues or questions, please refer to the documentation in each subdirectory or check the Swagger UI for API details.

                     ┌──────────────────────────────┐
                     │          Mobile App           │
                     │  - BLE scan                   │
                     │  - Control via MQTT           │
                     │  - UI for tasks               │
                     └──────────────▲───────────────┘
                                    │ REST
                                    │
                     ┌──────────────┴───────────────┐
                     │         Express.js API        │
                     │  - Save tasks                 │
                     │  - Save devices               │
                     │  - Automation engine          │
                     │  - MQTT client (optional)     │
                     └──────────────▲───────────────┘
                                    │ MQTT
                                    │
                     ┌──────────────┴───────────────┐
                     │            EMQX               │
                     │  - Broker                     │
                     │  - Routing                    │
                     │  - ACL                        │
                     └──────────────▲───────────────┘
                                    │ MQTT
                                    │
                     ┌──────────────┴───────────────┐
                     │        IoT Device / GW        │
                     │  - Telemetry → EMQX           │
                     │  - Commands ← EMQX            │
                     └───────────────────────────────┘
