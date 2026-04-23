# Hotel Service Management App

## Overview
A hotel operations and guest service management app with two sides: a guest-facing mobile app and a staff/admin dashboard to manage room service, housekeeping, reservations, and guest requests.

---

## Features

### Guest-Facing App
- **Room Booking** — Search available rooms by date, room type, and price; book and pay online
- **Check-in / Check-out** — Digital check-in before arrival, QR code room key
- **Room Service Orders** — Browse menu and place food/beverage orders from the room
- **Service Requests** — Request housekeeping, extra towels, maintenance, wake-up calls
- **Concierge Chat** — Live chat or chatbot with hotel concierge
- **Bill Viewing** — View itemized bill and pay on checkout
- **Feedback & Rating** — Rate stay and services per category
- **Amenity Booking** — Reserve spa, gym, restaurant, or pool time slots

### Staff / Admin Dashboard
- **Reservation Management** — View, edit, cancel reservations; check-in/out guests manually
- **Room Status Board** — Real-time status: available, occupied, dirty, cleaning, out of order
- **Task Assignment** — Assign housekeeping or maintenance tasks to staff with priority
- **Service Request Queue** — View and respond to incoming guest requests
- **Order Management** — Track room service orders from placed → preparing → delivered
- **Staff Scheduling** — Assign shifts and manage staff availability
- **Reports & Analytics** — Occupancy rate, revenue, service response time, guest satisfaction
- **Inventory Management** — Track minibar stock, linen counts, amenity supplies

---

## Application Logic

### Room Availability Logic
- Each room has: room number, type, floor, status (available/occupied/maintenance)
- When booking: check no overlapping confirmed reservations for the date range
- Block rooms under maintenance from appearing in guest search
- Auto-release un-paid reservations after a hold window (e.g., 30 minutes)

### Room Service Order Logic
- Guest places order → notification pushed to kitchen display system
- Order states: `placed → accepted → preparing → out for delivery → delivered`
- Estimated delivery time shown to guest with countdown
- Staff marks order delivered; charge auto-added to room bill

### Housekeeping Task Logic
- Checkout triggers "dirty" status on room
- Supervisor assigns cleaner; cleaner sees task on mobile app
- Cleaner marks: in progress → completed → ready for inspection
- Inspector confirms → room status changes to "available"

### Digital Key Logic
- Generate time-limited JWT or QR code upon check-in confirmation
- Key valid from check-in date to check-out date
- Transmitted to door lock hardware via BLE or NFC
- Revoke key on checkout or early cancellation

---

## Challenges

| Challenge | Description |
|-----------|-------------|
| Real-Time Room Status | Syncing room status across housekeeping, front desk, and guest app instantly |
| Hardware Integration | Integrating digital key with BLE/NFC door lock hardware (proprietary APIs) |
| Double Booking Prevention | Race conditions when multiple bookings hit the same room simultaneously |
| Multi-Role Access Control | Different permissions for guest, staff, housekeeping, supervisor, admin |
| Offline Staff App | Housekeeping staff may have poor connectivity on upper floors |
| PMS Integration | May need to integrate with existing Property Management Systems (PMS) |
| Payment Handling | Splitting bills, applying discounts, handling partial payments |
| Multilingual Guest App | International guests need localized language and currency |

---

## Recommended Tech Stack
- **Guest Mobile App**: Flutter / React Native
- **Admin Dashboard**: Next.js / React
- **Backend**: Node.js + PostgreSQL
- **Real-Time**: Socket.io for live order and room status updates
- **Payment**: Stripe / local payment gateway
- **Push Notifications**: Firebase Cloud Messaging
- **Auth**: JWT with role-based access control (RBAC)
- **BLE/NFC Keys**: Integration with SALTO, ASSA ABLOY, or similar SDK
