# Mock Data Setup for Mobile App

This document describes the mock data system used in the mobile app for testing without backend API access.

## Overview

The mobile app includes a `MockDataProvider` utility class that provides mock data when the demo user is logged in. This allows testing the complete UI workflow without requiring backend API access.

## Demo User Credentials

- **Email**: `demo@smartify.com`
- **Password**: `demo123456`

When logged in with these credentials, the app automatically uses mock data instead of making API calls.

## Mock Data Provided

### 1. Homes (2 homes)

#### Primary Home: "My Home"
- **ID**: 1
- **Address**: 701 7th Ave, New York, 10036, USA
- **Coordinates**: 40.7128, -74.0060
- **Country**: United States
- **Is Primary**: Yes

#### Secondary Home: "Vacation Home"
- **ID**: 2
- **Address**: 1600 Amphitheatre Parkway, Mountain View, CA 94043, USA
- **Coordinates**: 37.4220, -122.0841
- **Country**: United States
- **Is Primary**: No

### 2. Rooms (6 rooms total)

#### For Primary Home (ID: 1)
- Living Room (ID: 1)
- Bedroom (ID: 2)
- Kitchen (ID: 3)
- Bathroom (ID: 4)
- Office (ID: 5)

#### For Vacation Home (ID: 2)
- Guest Room (ID: 6)

### 3. Notifications (6 notifications)

#### Smart Home Category (4 notifications)

**Today - Unread:**
1. **Account Security Alert** (10:30 AM)
   - Type: security
   - Message: "We've noticed some unusual activity on your account. Please review your security settings."
   - Icon: security

2. **Device Connected** (2:15 PM)
   - Type: info
   - Message: "New device \"Living Room Sensor\" has been successfully connected to your smart home."
   - Icon: device

3. **Automation Activated** (4:45 PM)
   - Type: feature
   - Message: "Your \"Morning Routine\" automation has been activated successfully."
   - Icon: star

**Yesterday - Unread:**
4. **Device Maintenance** (9:00 AM)
   - Type: alert
   - Message: "Time for scheduled maintenance on \"Kitchen Thermostat\". Please check device status."
   - Icon: device

#### General Category (2 notifications)

**Today - Read:**
5. **Energy Usage Report** (8:00 AM)
   - Type: info
   - Message: "Your energy consumption this month is 15% lower than last month. Great job!"
   - Icon: energy
   - Status: Read

**Yesterday - Read:**
6. **Bill Reminder** (12:30 PM)
   - Type: reminder
   - Message: "Your monthly electricity bill is due in 3 days. Amount: $125.50"
   - Icon: calendar
   - Status: Read

### 4. Chatbot History (4 messages)

Sample conversation about automation and device control:

1. **User** (2 hours ago): "Hello! How can I set up automation?"
2. **Assistant** (2 hours ago): "Hello! I'd be happy to help you set up automation. Automation allows you to create rules that trigger actions based on conditions. For example, you can set your lights to turn on automatically when you arrive home. Would you like me to guide you through creating your first automation?"
3. **User** (2 hours ago): "How do I control my devices?"
4. **Assistant** (2 hours ago): "You can control your devices in several ways:\n\n1. **Manual Control**: Open the device from your home screen and use the controls\n2. **Voice Commands**: Use voice assistants like Google Assistant or Alexa\n3. **Automation**: Set up rules to control devices automatically\n4. **Schedules**: Create time-based schedules for your devices\n\nWhich method would you like to learn more about?"

### 5. Chatbot Responses

The chatbot provides contextual responses based on user input:

- **Greetings** (hello, hi, hey): "Hello! I'm Bobo, your smart home assistant. How can I help you today?"
- **Automation queries**: Information about automation features
- **Device control queries**: Methods for controlling devices
- **Energy queries**: Energy consumption monitoring
- **Help/Support**: List of available assistance topics
- **Setup queries**: Step-by-step setup guidance
- **Default**: Generic helpful response

## How It Works

### Detection
The app checks if the current user is the demo user using:
```java
MockDataProvider.isDemoUser(authManager)
```

This checks if the email matches `demo@smartify.com`.

### Usage in Activities

#### MainActivity
- Uses `getMockHomes()` for location selector
- Uses `getMockRooms(homeId)` for room filtering

#### NotificationsActivity
- Uses `getMockNotifications(category)` for notification list
- Supports filtering by "general" or "smart_home" category

#### ChatbotActivity
- Uses `getMockChatHistory()` for conversation history
- Uses `getMockBotResponse(message)` for bot responses

## Implementation Details

### MockDataProvider Class
Location: `app/src/main/java/com/smarthome/iot/utils/MockDataProvider.java`

### Key Methods
- `isDemoUser(AuthManager)` - Check if current user is demo user
- `getMockHomes()` - Returns list of 2 homes
- `getMockRooms(Integer homeId)` - Returns rooms filtered by homeId
- `getMockNotifications(String category)` - Returns notifications filtered by category
- `getMockChatHistory()` - Returns sample chat history
- `getMockBotResponse(String userMessage)` - Returns contextual bot response

## Testing Workflow

1. **Login** with demo credentials:
   - Email: `demo@smartify.com`
   - Password: `demo123456`

2. **Test Features**:
   - Location selector shows 2 homes
   - Room filtering works for each home
   - Notifications show 6 notifications (4 unread)
   - Chatbot shows conversation history
   - Bot responds to various queries

3. **Switch Homes**:
   - Select different home from location selector
   - Rooms update based on selected home
   - Devices filter by room

## Adding New Mock Data

To add new mock data:

1. Open `MockDataProvider.java`
2. Find the appropriate method (e.g., `getMockNotifications`)
3. Add new mock data object
4. Set appropriate properties (timestamps, categories, etc.)
5. Test in the app

## Notes

- Mock data is only used when logged in as demo user
- For other users, the app attempts API calls
- If API calls fail, the app may fall back to empty states
- Mock data timestamps are relative to current time
- All mock data uses realistic values matching backend structure

## Future Enhancements

- Add mock devices
- Add mock energy consumption data
- Add mock automation rules
- Add more chatbot conversation examples
