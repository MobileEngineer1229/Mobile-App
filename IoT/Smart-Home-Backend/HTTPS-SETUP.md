# HTTPS Setup Guide

This guide explains how to enable HTTPS for your Smart Home Backend API.

## Quick Setup (Self-Signed Certificate for Development)

### Step 1: Generate SSL Certificate

Run the certificate generation script:

```powershell
.\scripts\generate-self-signed-cert.ps1
```

This will create:
- `certs/server.crt` - SSL certificate
- `certs/server.key` - Private key

### Step 2: Update .env File

Add these lines to your `.env` file:

```env
ENABLE_HTTPS=true
HTTPS_PORT=3443
SSL_CERT_PATH=certs/server.crt
SSL_KEY_PATH=certs/server.key
PORT=3003
```

**Note:** 
- `HTTPS_PORT` is optional - if not set, it will use the same port as `PORT`
- Keep `PORT` for HTTP (if you want both HTTP and HTTPS running)

### Step 3: Open Firewall Port

Open the HTTPS port in Windows Firewall:

```powershell
New-NetFirewallRule -DisplayName "Smart Home Backend - HTTPS Port 3443" -Direction Inbound -LocalPort 3443 -Protocol TCP -Action Allow
```

### Step 4: Restart Server

```powershell
.\start-server.ps1
```

### Step 5: Access HTTPS URL

- **HTTPS URL**: `https://172.86.88.76:3443/api-docs`
- **Local HTTPS**: `https://localhost:3443/api-docs`

**Important:** Browsers will show a security warning for self-signed certificates. This is normal for development. Click "Advanced" → "Proceed to 172.86.88.76 (unsafe)" to continue.

---

## Production Setup (Recommended: Reverse Proxy with nginx)

For production, it's recommended to use nginx as a reverse proxy with Let's Encrypt certificates.

### Option 1: nginx Reverse Proxy (Recommended)

1. **Install nginx** on your server
2. **Get Let's Encrypt certificate** using Certbot
3. **Configure nginx** to proxy to your Node.js app

Example nginx configuration:

```nginx
server {
    listen 443 ssl http2;
    server_name 172.86.88.76;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:3003;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name 172.86.88.76;
    return 301 https://$server_name$request_uri;
}
```

### Option 2: Let's Encrypt with Certbot

```bash
# Install certbot
sudo apt-get install certbot python3-certbot-nginx

# Get certificate
sudo certbot --nginx -d your-domain.com

# Auto-renewal
sudo certbot renew --dry-run
```

---

## Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `ENABLE_HTTPS` | Enable HTTPS server | `false` | No |
| `HTTPS_PORT` | HTTPS port number | Same as `PORT` | No |
| `SSL_CERT_PATH` | Path to SSL certificate file | - | Yes (if HTTPS enabled) |
| `SSL_KEY_PATH` | Path to SSL private key file | - | Yes (if HTTPS enabled) |
| `PORT` | HTTP port number | `3003` | No |

---

## Troubleshooting

### Certificate Errors

- **Self-signed certificate warning**: Normal for development. Accept the warning in your browser.
- **Certificate not found**: Check that paths in `.env` are correct and files exist.
- **Permission denied**: Ensure the Node.js process can read the certificate files.

### Port Issues

- **Port already in use**: Change `HTTPS_PORT` to a different port (e.g., 3443, 8443)
- **Firewall blocking**: Ensure the HTTPS port is open in Windows Firewall

### Connection Issues

- **Cannot connect**: Verify the server is running and listening on the correct port
- **Mixed content**: Ensure all API calls use `https://` instead of `http://`

---

## Security Notes

1. **Self-signed certificates** are for development only. Never use in production.
2. **Production** should use certificates from a trusted CA (Let's Encrypt, commercial CA).
3. **Keep private keys secure** - never commit them to version control.
4. **Use strong ciphers** - nginx/Node.js default configurations are usually secure.

---

## Testing HTTPS

Test your HTTPS endpoint:

```powershell
# Test local HTTPS
Invoke-WebRequest -Uri "https://localhost:3443/health" -SkipCertificateCheck

# Test network HTTPS (from another machine)
curl -k https://172.86.88.76:3443/health
```

The `-SkipCertificateCheck` and `-k` flags bypass certificate validation (useful for self-signed certs).
