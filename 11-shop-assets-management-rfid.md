# Shop Assets Management App (RFID)

## Overview
A mobile app for tracking and managing physical assets in a shop, warehouse, or retail environment using RFID tags — enabling fast inventory counts, location tracking, check-in/out, and maintenance scheduling.

---

## Features

### Core Features
- **Asset Registry** — Database of all assets: name, category, serial number, RFID tag ID, location, status
- **RFID Scanning** — Scan RFID tags via mobile RFID reader or NFC to identify and log assets
- **Check-In / Check-Out** — Log when an asset is taken out or returned; track who has it
- **Location Tracking** — Assign assets to zones/rooms; update location on scan
- **Inventory Count** — Bulk scan a room to generate a count report and flag missing items
- **Asset Status** — Available, in use, under maintenance, retired, lost
- **Maintenance Schedule** — Log service history; set maintenance reminders for equipment

### Additional Features
- **Asset Search** — Search by name, category, tag ID, location, or assignee
- **Audit Trail** — Full history of every scan, movement, and status change per asset
- **Reports** — Asset utilization, missing items, overdue checkouts, maintenance due
- **Low Stock Alerts** — Alert when category count drops below minimum threshold
- **Asset Photos** — Attach photos to asset records
- **QR Code Fallback** — Use QR code scanning where RFID is not available
- **Multi-Location** — Manage assets across multiple branches or warehouses
- **User Roles** — Admin, manager, staff with different permissions

---

## Application Logic

### RFID Scan Logic
- Mobile connects to RFID reader via Bluetooth or USB OTG
- Reader returns list of detected EPC (Electronic Product Code) tag IDs in range
- App looks up each tag ID in asset DB and updates their "last seen" location and timestamp
- Unknown tag IDs flagged for registration

### Check-Out / Check-In Logic
- Staff scans asset tag → system prompts: "Check Out to [staff name]?" → confirm
- Record: asset_id, staff_id, timestamp, expected_return_date
- On check-in scan: match to open checkout record, calculate days out, close record
- Alert if asset not returned by expected return date

### Inventory Count Logic
- User starts a "count session" for a zone
- Walk around and scan all assets in the zone (bulk read)
- System compares scanned set against expected assets for that zone
- Report: present, missing, unexpected (assets from other zones)

### Maintenance Logic
- Each asset has maintenance intervals (e.g., every 6 months or every 500 hours of use)
- System calculates next due date from last maintenance date + interval
- Push notification when maintenance is approaching (e.g., 7 days before due)
- Maintenance log entry created on each service event

---

## Challenges

| Challenge | Description |
|-----------|-------------|
| RFID Hardware Compatibility | Different RFID readers have different SDKs and connection methods |
| Multi-Tag Read Disambiguation | Bulk reading picks up tags from adjacent rooms; filtering false reads |
| NFC vs RFID Distinction | NFC (HF 13.56MHz) has short range; UHF RFID is needed for bulk scanning — hardware requirements differ |
| Offline Operation | Warehouse/shop environments may have poor Wi-Fi; offline-first is essential |
| Data Sync Conflicts | Multiple staff scanning simultaneously can create conflicting location records |
| Tag Placement | Metal surfaces and liquids interfere with RFID signals; staff must understand placement rules |
| Asset Lifecycle Management | Retiring, replacing, or merging asset records while preserving history |
| Large Asset Count Performance | Bulk scan of 500+ tags must process and match quickly without freezing the app |

---

## Recommended Tech Stack
- **Mobile**: Android Native (Java) — min SDK API 24 (Android 7.0+)
- **Backend**: Node.js + Express.js + PostgreSQL
- **HTTP Client**: Retrofit 2 + OkHttp
- **RFID Reader**: Zebra, Chainway, or Impinj UHF RFID reader via Android Bluetooth API or USB OTG (Android USB Host API)
- **NFC Fallback**: Android NFC API (`NfcAdapter`) — HF tag reading for short-range scanning
- **QR Code Fallback**: Google ML Kit Barcode Scanning API (Android) or ZXing Android library
- **Local DB / Offline**: Room Database + WorkManager — offline-first asset DB; sync queue when back online
- **Auth**: JWT + RBAC (admin / manager / staff)
- **PDF Reports**: iText 7 for Android (on-device) + PDFKit server-side (Node.js) for scheduled reports
- **Push Notifications**: Firebase Cloud Messaging (FCM) — overdue checkout and maintenance alerts
- **Image Loading**: Glide — asset photos
- **Architecture**: MVVM + LiveData + ViewModel (Android Jetpack)
