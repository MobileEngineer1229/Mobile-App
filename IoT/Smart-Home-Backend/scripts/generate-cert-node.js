// Generate self-signed certificate using Node.js
// This doesn't require OpenSSL to be installed

const fs = require('fs');
const { execSync } = require('child_process');
const path = require('path');

const certDir = path.join(__dirname, '..', 'certs');
const certFile = path.join(certDir, 'server.crt');
const keyFile = path.join(certDir, 'server.key');

// Create certs directory
if (!fs.existsSync(certDir)) {
  fs.mkdirSync(certDir, { recursive: true });
}

console.log('Generating self-signed certificate using Node.js...');

try {
  // Try to use openssl if available
  const openssl = execSync('where openssl', { encoding: 'utf8', stdio: 'pipe' }).trim();
  
  console.log('Using OpenSSL:', openssl);
  console.log('Generating private key...');
  execSync(`openssl genrsa -out "${keyFile}" 2048`, { stdio: 'inherit' });
  
  console.log('Generating certificate...');
  execSync(`openssl req -new -x509 -key "${keyFile}" -out "${certFile}" -days 365 -subj "/CN=172.86.88.76/O=Smart Home Backend/C=US"`, { stdio: 'inherit' });
  
  console.log('\n✓ Certificate generated successfully!');
  console.log(`  Certificate: ${certFile}`);
  console.log(`  Private Key: ${keyFile}`);
} catch (error) {
  console.error('\nOpenSSL not found. Please install OpenSSL or use HTTP.');
  console.error('\nQuick fix: Use HTTP instead of HTTPS:');
  console.error('  http://172.86.88.76:3003/api-docs');
  process.exit(1);
}
