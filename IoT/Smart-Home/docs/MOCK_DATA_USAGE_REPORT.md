# Mock Data Usage Report

This document lists all areas of the app that are still using mock data and have not been integrated with the API yet.

## ✅ **FULLY INTEGRATED WITH API** (Using API, mock data only as fallback)

### **Home & Room Management**

1. **HomeManagementActivity** - ✅ API Integrated
   - Loads homes from API (`GET /homes`)
   - Falls back to mock data only on error or when not logged in

2. **RoomManagementActivity** - ✅ API Integrated
   - Loads rooms from API (`GET /rooms?homeId=X`)
   - Creates rooms via API (`POST /rooms`)
   - Falls back to mock data only on error or when not logged in

3. **AccountActivity** - ✅ API Integrated
   - Loads primary home from API (`GET /homes/primary`)
   - Falls back to first home if no primary home exists

4. **CreateHomeActivity** - ✅ API Integrated
   - Creates home via API (`POST /homes`)
   - Creates rooms sequentially after home creation

5. **HomeDetailActivity** - ✅ API Integrated
   - Loads home details from API (`GET /homes/{id}`)
   - Loads room count from API (`GET /rooms?homeId=X`)
   - Loads device count from API (`GET /devices?homeId=X`)
   - Updates home name via API (`PUT /homes/{id}`)
   - Deletes home via API (`DELETE /homes/{id}`)
   - Falls back to mock data only on error or when not logged in

6. **JoinHomeActivity** - ✅ API Integrated
   - Joins home via API (`POST /homes/join`)
   - Handles invitation code validation
   - Falls back to mock data only for demo users

### **Member Management**

7. **AddMemberActivity** - ✅ API Integrated
   - Sends home invitation via API (`POST /homes/{id}/invitations`)
   - Includes email and role in invitation
   - Falls back to mock data only for demo users

8. **MemberDetailActivity** - ✅ API Integrated
   - Removes member from home via API (`DELETE /homes/{id}/members/{memberId}`)
   - Falls back to mock data only for demo users

### **Smart Scenes Management**

9. **SmartSceneActivity** - ✅ API Integrated
   - Loads scenes from API (`GET /scenes?type=X&homeId=Y`)
   - Executes scenes via API (`POST /scenes/{id}/execute`)
   - Updates scene enabled state via API (`PUT /scenes/{id}`)
   - Falls back to mock data only for demo users

10. **ManageScenesActivity** - ✅ API Integrated
    - Loads scenes from API (`GET /scenes?type=X&homeId=Y`)
    - Saves scene order via API (`PUT /scenes/order`)
    - Deletes scenes via API (`DELETE /scenes/{id}`)
    - Falls back to mock data only for demo users

11. **SelectSceneActivity** - ✅ API Integrated
    - Loads scenes from API (`GET /scenes?type=X&homeId=Y`)
    - Falls back to mock data only for demo users

12. **SceneLogsActivity** - ✅ API Integrated
    - Loads scene execution logs from API (`GET /scenes/{id}/logs`)
    - Supports pagination (page, limit)
    - Falls back to mock data only for demo users

13. **SceneBuilderActivity** - ✅ API Integrated
    - Creates scenes via API (`POST /scenes`)
    - Converts conditions and tasks to API format
    - Includes homeId from primary home
    - Falls back to mock data only for demo users

### **Device Control**

14. **DeviceControlDetailActivity** - ✅ API Integrated
    - Toggles device power via API (`POST /devices/{id}/control/power`)
    - Reverts switch state on API failure
    - Falls back to mock data only for demo users

15. **LampControlFragment** - ✅ API Integrated
    - Updates brightness via API (`POST /devices/{id}/control/lamp`)
    - Updates temperature via API (white mode)
    - Updates color via API (color mode)
    - Updates saturation via API
    - Prevents duplicate API calls with flags
    - Falls back to mock data only for demo users

16. **SpeakerControlFragment** - ✅ API Integrated
    - Controls volume via API (`POST /devices/{id}/control/speaker`)
    - Controls playback via API (play, pause, previous, next)
    - Seeks to position via API (action="seek")
    - Loads device state from API (`GET /devices/{id}/control/state`)
    - Falls back to mock data only for demo users

17. **CameraControlFragment** - ✅ API Integrated
    - Executes camera commands via API (`POST /devices/{id}/control/camera`)
    - Toggles audio via API (action="toggle_audio")
    - Moves camera via API (action="move")
    - Loads camera stream from API (`GET /devices/{id}/control/camera/stream`)
    - Falls back to mock data only for demo users

18. **MainActivity** - ✅ API Integrated
    - Loads homes, rooms, and devices from API
    - Toggles device power via API (`POST /devices/{id}/control/power`)
    - Updates UI immediately, then syncs with API
    - Reverts device state on API failure
    - Uses mock data only for demo users (`demo@smartify.com`)

### **Other Activities**

19. **NotificationsActivity** - ✅ API Integrated
    - Loads notifications from API
    - Uses mock data only for demo users

20. **DeviceCategoryListActivity** - ✅ API Integrated (Acceptable)
    - Uses API when logged in and not demo user
    - Falls back to mock data when not logged in or demo user
    - ✅ This is acceptable behavior (fallback for offline/demo mode)

---

## ❌ **STILL USING MOCK DATA** (Not integrated with API)

### 1. **Control Device Activity**

#### **ControlDeviceActivity** (`app/src/main/java/com/smarthome/iot/ui/ControlDeviceActivity.java`)
- **Status**: ❌ Using mock data only
- **Line**: 219 - `MockDataProvider.getMockDevicesForScene()`
- **Missing API Calls**:
  - `GET /devices?category=X&roomId=Y` - Load devices for scene control
- **Impact**: Cannot control real devices in scene builder

---

### 2. **Nearby Devices**

#### **NearbyDevicesFragment** (`app/src/main/java/com/smarthome/iot/ui/fragments/NearbyDevicesFragment.java`)
- **Status**: ❌ Using mock data only
- **Line**: 110 - Comment says "Mock device data matching the UI positions"
- **Missing API Calls**:
  - `GET /devices/nearby` - Discover nearby devices
  - `POST /devices/discover` - Start device discovery
- **Impact**: Cannot discover real nearby devices

---

### 3. **Onboarding**

#### **OnboardingStep4Activity** (`app/src/main/java/com/smarthome/iot/ui/OnboardingStep4Activity.java`)
- **Status**: ⚠️ Partially integrated
- **Line**: 187 - Comment says "For now, use a mock location"
- **Missing API Calls**:
  - Location selection might need to be saved to home creation
- **Impact**: Location might not be properly saved during onboarding

---

### 6. **Control Device Activity**

#### **ControlDeviceActivity** (`app/src/main/java/com/smarthome/iot/ui/ControlDeviceActivity.java`)
- **Status**: ❌ Using mock data only
- **Line**: 219 - `MockDataProvider.getMockDevicesForScene()`
- **Missing API Calls**:
  - `GET /devices?category=X&roomId=Y` - Load devices for scene control
- **Impact**: Cannot control real devices in scene builder

---

### 7. **Nearby Devices**

#### **NearbyDevicesFragment** (`app/src/main/java/com/smarthome/iot/ui/fragments/NearbyDevicesFragment.java`)
- **Status**: ❌ Using mock data only
- **Line**: 110 - Comment says "Mock device data matching the UI positions"
- **Missing API Calls**:
  - `GET /devices/nearby` - Discover nearby devices
  - `POST /devices/discover` - Start device discovery
- **Impact**: Cannot discover real nearby devices

---

### 8. **Onboarding**

#### **OnboardingStep4Activity** (`app/src/main/java/com/smarthome/iot/ui/OnboardingStep4Activity.java`)
- **Status**: ⚠️ Partially integrated
- **Line**: 187 - Comment says "For now, use a mock location"
- **Missing API Calls**:
  - Location selection might need to be saved to home creation
- **Impact**: Location might not be properly saved during onboarding

---

## 📊 **Summary Statistics**

- **Fully Integrated**: 20 activities/fragments ✅
- **Partially Integrated**: 1 activity (OnboardingStep4Activity - location)
- **Not Integrated**: 2 activities/fragments
  - ControlDeviceActivity (scene device selection)
  - NearbyDevicesFragment (device discovery)
- **Total Areas Needing Integration**: 3 areas remaining
- **Integration Progress**: ~87% complete (20/23 areas)

---

## 🔧 **Remaining Integration Priority**

### **Medium Priority** (Important Features)
1. **ControlDeviceActivity** - Device selection for scenes
   - Used in scene builder to select devices
   - Currently uses mock data
   - **Impact**: Cannot select real devices when building scenes

### **Low Priority** (Nice to Have)
2. **NearbyDevicesFragment** - Device discovery
   - Advanced feature for discovering nearby devices
   - Can use manual device entry as alternative
   - **Impact**: Cannot discover devices automatically

3. **OnboardingStep4Activity** - Location selection
   - Location might not be properly saved during onboarding
   - May already work if location is included in home creation
   - **Impact**: Minor - location can be set later

---

## 📝 **Notes**

- **Demo User Support**: Mock data is intentionally used for demo users (`demo@smartify.com`) - this is expected behavior
- **Fallback Strategy**: Many activities correctly use mock data as fallback when API fails - this is good practice
- **API Endpoints**: Check `ApiService.java` to see which endpoints are already defined but not used

---

**Last Updated**: December 2024 - After major API integration work
**Next Steps**: 
- ✅ **COMPLETED**: Smart Scenes Management (all 5 activities)
- ✅ **COMPLETED**: Device Control (all 5 activities/fragments)
- ✅ **COMPLETED**: Home Management (all 3 activities)
- ✅ **COMPLETED**: Member Management (all 2 activities)
- 🔄 **REMAINING**: ControlDeviceActivity, NearbyDevicesFragment, OnboardingStep4Activity (location)

**Integration Status**: ~87% complete - Most core functionality is now API-integrated!
