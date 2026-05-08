# Postman Collection Setup Guide

This guide will help you set up and use the Postman collection for testing the Smart Home Backend API.

## Files Included

1. **Smart-Home-Backend.postman_collection.json** - Complete API collection with all endpoints
2. **Smart-Home-Backend.postman_environment.json** - Environment variables for easy configuration

## Quick Setup

### Step 1: Import Collection and Environment

1. Open Postman
2. Click **Import** button (top left)
3. Import both files:
   - `Smart-Home-Backend.postman_collection.json`
   - `Smart-Home-Backend.postman_environment.json`
4. Select the environment: **Smart Home Backend - Development** (top right dropdown)

### Step 2: Configure Environment Variables

The environment file includes:
- `base_url`: `http://172.86.88.76:3003` (or `http://localhost:3003` for local)
- `api_version`: `v1`
- `token`: (auto-populated after login)
- `user_id`: (auto-populated after login)

**To change the base URL:**
1. Click the environment dropdown (top right)
2. Click the eye icon to view/edit variables
3. Update `base_url` if needed

### Step 3: Start Testing

1. **First, authenticate:**
   - Go to **Authentication** folder
   - Run **Login** or **Signup** request
   - The JWT token will be automatically saved to the `token` variable

2. **Then test other endpoints:**
   - All authenticated endpoints will automatically use the saved token
   - Browse through folders organized by resource type

## Collection Structure

The collection is organized into the following folders:

### 🔐 Authentication
- Signup, Login, Profile management
- Password reset (Forgot Password, Verify OTP, Reset Password)
- **Start here!** Login or signup first to get your token.

### 🏠 Homes
- Create, read, update, delete homes
- Get primary home
- Manage home locations and addresses

### 🚪 Rooms
- Create, read, update, delete rooms
- Organize devices by rooms
- Filter rooms by home

### 📱 Devices
- Full CRUD operations for devices
- Device discovery
- Get device types/templates
- Filter by category, type, status, room

### 🎮 Device Control
- Control device power
- Control smart lamps (brightness, color)
- Control CCTV cameras
- Control speakers
- Control air conditioners
- Get device state
- Execute custom commands

### 🎬 Scenes
- Create automation scenes
- Execute scenes manually
- Toggle scene enabled/disabled
- Get scene logs

### 🔔 Notifications
- Get all notifications with filters
- Mark as read
- Delete notifications
- Get notification statistics

### 📊 Reports
- Monthly energy consumption summary
- Device consumption reports
- Overall statistics

### 🤖 Chatbot
- Send messages to AI assistant
- Get chat history
- Clear history

### ❤️ Health Check
- Verify server is running (no auth required)

## Features

### ✅ Automatic Token Management
- Login/Signup requests automatically save the JWT token
- All authenticated requests use the saved token
- No need to manually copy/paste tokens!

### ✅ Demo Data Included
- All requests include realistic demo data
- Ready to test immediately
- Easy to modify for your needs

### ✅ Detailed Descriptions
- Each endpoint includes:
  - Purpose and description
  - Required parameters
  - Demo data examples
  - Response information

### ✅ Organized Structure
- Endpoints grouped by resource type
- Easy to find what you need
- Logical folder hierarchy

## Testing Workflow

### Recommended Testing Order:

1. **Health Check** - Verify server is running
2. **Authentication → Login** - Get your token
3. **Homes → Create Home** - Create a home
4. **Rooms → Create Room** - Create a room
5. **Devices → Create Device** - Add devices
6. **Device Control → Control Power** - Test device control
7. **Scenes → Create Scene** - Create automation
8. **Notifications → Get All Notifications** - Check notifications

## Demo Data Examples

### User Signup/Login
```json
{
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
}
```

### Create Home
```json
{
    "name": "My Smart Home",
    "address": "123 Main Street, City, State 12345",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "country": "United States",
    "isPrimary": true
}
```

### Create Device
```json
{
    "name": "Smart Light Bulb",
    "type": "actuator",
    "macAddress": "AA:BB:CC:DD:EE:FF",
    "manufacturer": "Philips",
    "model": "Hue White",
    "roomId": 1,
    "homeId": 1
}
```

### Control Lamp
```json
{
    "power": true,
    "brightness": 80,
    "color": "#FF5733"
}
```

## Troubleshooting

### Token Not Working?
1. Make sure you've run **Login** or **Signup** first
2. Check the environment variable `token` is set
3. Verify the token hasn't expired (JWT tokens expire after 7 days by default)

### 401 Unauthorized?
- Token might be expired - login again
- Make sure the Authorization header is set correctly
- Verify the token variable is populated

### 404 Not Found?
- Check the `base_url` is correct
- Verify the server is running
- Check the API version (`v1`) is correct

### Connection Refused?
- Verify the server is running on the correct port (3003)
- Check firewall settings
- Try `http://localhost:3003` if testing locally

## Environment Variables

You can create multiple environments for different setups:

### Development (Local)
```
base_url: http://localhost:3003
api_version: v1
```

### Development (Network)
```
base_url: http://172.86.88.76:3003
api_version: v1
```

### Production (when ready)
```
base_url: https://api.yourdomain.com
api_version: v1
```

## Tips

1. **Use Collection Runner** - Run multiple requests in sequence
2. **Save Responses** - Save example responses for documentation
3. **Create Tests** - Add automated tests to verify responses
4. **Use Variables** - Store IDs (home_id, device_id, etc.) for reuse
5. **Export Collection** - Share with your team

## Next Steps

1. Import the collection and environment
2. Run Health Check to verify connection
3. Login to get your token
4. Start exploring the API!

Happy Testing! 🚀
