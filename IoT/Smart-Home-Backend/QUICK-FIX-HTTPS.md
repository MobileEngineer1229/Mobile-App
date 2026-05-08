# Quick Fix for HTTPS SSL Errors

## The Problem
You're accessing `https://172.86.88.76:3003/api-docs/` but the server is running HTTP on port 3003, causing SSL protocol errors.

## Solution 1: Use HTTP (Quickest - No Setup Required)

Simply use **HTTP** instead of HTTPS:

```
http://172.86.88.76:3003/api-docs
```

This will work immediately without any configuration.

---

## Solution 2: Set Up HTTPS Properly

### Step 1: Install OpenSSL

Download and install OpenSSL from:
**https://slproweb.com/products/Win32OpenSSL.html**

Choose the "Win64 OpenSSL v3.x.x Light" version (smaller download).

### Step 2: Generate Certificate

After installing OpenSSL, run:

```powershell
.\scripts\generate-self-signed-cert.ps1
```

Or manually:

```powershell
# Create certs directory
New-Item -ItemType Directory -Path certs -Force

# Generate private key
openssl genrsa -out certs/server.key 2048

# Generate certificate
openssl req -new -x509 -key certs/server.key -out certs/server.crt -days 365 -subj "/CN=172.86.88.76/O=Smart Home Backend/C=US"
```

### Step 3: Update .env File

Add these lines to your `.env` file:

```env
ENABLE_HTTPS=true
HTTPS_PORT=3003
SSL_CERT_PATH=certs/server.crt
SSL_KEY_PATH=certs/server.key
```

### Step 4: Restart Server

```powershell
.\start-server.ps1
```

### Step 5: Access HTTPS

Now you can access:
```
https://172.86.88.76:3003/api-docs
```

**Note:** Browsers will show a security warning for self-signed certificates. Click "Advanced" → "Proceed to 172.86.88.76 (unsafe)" to continue.

---

## Why This Happens

- Your browser is trying to load resources with HTTPS protocol
- But the server is responding with HTTP protocol
- This causes SSL/TLS handshake failures (ERR_SSL_PROTOCOL_ERROR)

The fix is to either:
1. Use HTTP (simplest)
2. Configure the server to serve HTTPS (requires SSL certificate)

---

## Recommendation

For **development/testing**: Use HTTP - it's simpler and works immediately.

For **production**: Set up HTTPS with a proper certificate from Let's Encrypt or a commercial CA.
