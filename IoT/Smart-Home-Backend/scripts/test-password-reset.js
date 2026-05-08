/**
 * Test script for password reset endpoints
 * Usage: node scripts/test-password-reset.js [email]
 */

const http = require('http');

const BASE_URL = 'http://localhost:3003/api/v1';
const TEST_EMAIL = process.argv[2] || 'tymoshenkovitalii84@gmail.com';

// Colors for console output
const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
};

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

function makeRequest(method, path, data = null) {
  return new Promise((resolve, reject) => {
    // Remove leading slash if present, then construct URL properly
    const cleanPath = path.startsWith('/') ? path.substring(1) : path;
    const url = new URL(cleanPath, BASE_URL);
    const options = {
      method,
      headers: {
        'Content-Type': 'application/json',
      },
    };

    const req = http.request(url, options, (res) => {
      let body = '';
      res.on('data', (chunk) => {
        body += chunk;
      });
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          resolve({ status: res.statusCode, data: json });
        } catch (e) {
          resolve({ status: res.statusCode, data: body });
        }
      });
    });

    req.on('error', (error) => {
      reject(error);
    });

    if (data) {
      req.write(JSON.stringify(data));
    }

    req.end();
  });
}

async function testForgotPassword() {
  log('\n════════════════════════════════════════', 'cyan');
  log('Test 1: Forgot Password (Send OTP)', 'cyan');
  log('════════════════════════════════════════', 'cyan');

  try {
    const response = await makeRequest('POST', '/users/forgot-password', {
      email: TEST_EMAIL,
    });

    if (response.status === 200 && response.data.success) {
      log('✅ PASS: Forgot password request successful', 'green');
      log(`   Message: ${response.data.data.message}`, 'green');
      log(`   OTP Sent: ${response.data.data.otpSent}`, 'green');
      log(`   Expires In: ${response.data.data.expiresInMinutes} minutes`, 'green');
      return { success: true, data: response.data.data };
    } else {
      log('❌ FAIL: Forgot password request failed', 'red');
      log(`   Status: ${response.status}`, 'red');
      log(`   Response: ${JSON.stringify(response.data, null, 2)}`, 'red');
      return { success: false };
    }
  } catch (error) {
    log(`❌ ERROR: ${error.message}`, 'red');
    return { success: false, error };
  }
}

async function testVerifyOTP(otpCode) {
  log('\n════════════════════════════════════════', 'cyan');
  log('Test 2: Verify OTP', 'cyan');
  log('════════════════════════════════════════', 'cyan');

  if (!otpCode) {
    log('⚠️  SKIP: No OTP code provided (check backend logs for OTP)', 'yellow');
    log('   To test manually, get OTP from backend logs and run:', 'yellow');
    log(`   node scripts/test-password-reset.js ${TEST_EMAIL} <OTP_CODE>`, 'yellow');
    return { success: false, skipped: true };
  }

  try {
    const response = await makeRequest('POST', '/users/verify-otp', {
      email: TEST_EMAIL,
      otpCode: otpCode,
    });

    if (response.status === 200 && response.data.success && response.data.data.valid) {
      log('✅ PASS: OTP verification successful', 'green');
      log(`   Message: ${response.data.data.message}`, 'green');
      return { success: true, data: response.data.data };
    } else {
      log('❌ FAIL: OTP verification failed', 'red');
      log(`   Status: ${response.status}`, 'red');
      log(`   Response: ${JSON.stringify(response.data, null, 2)}`, 'red');
      return { success: false };
    }
  } catch (error) {
    log(`❌ ERROR: ${error.message}`, 'red');
    return { success: false, error };
  }
}

async function testResetPassword(otpCode, newPassword = 'NewPassword123') {
  log('\n════════════════════════════════════════', 'cyan');
  log('Test 3: Reset Password', 'cyan');
  log('════════════════════════════════════════', 'cyan');

  if (!otpCode) {
    log('⚠️  SKIP: No OTP code provided', 'yellow');
    return { success: false, skipped: true };
  }

  try {
    const response = await makeRequest('POST', '/users/reset-password', {
      email: TEST_EMAIL,
      otpCode: otpCode,
      newPassword: newPassword,
    });

    if (response.status === 200 && response.data.success && response.data.data.success) {
      log('✅ PASS: Password reset successful', 'green');
      log(`   Message: ${response.data.data.message}`, 'green');
      return { success: true, data: response.data.data };
    } else {
      log('❌ FAIL: Password reset failed', 'red');
      log(`   Status: ${response.status}`, 'red');
      log(`   Response: ${JSON.stringify(response.data, null, 2)}`, 'red');
      return { success: false };
    }
  } catch (error) {
    log(`❌ ERROR: ${error.message}`, 'red');
    return { success: false, error };
  }
}

async function testInvalidOTP() {
  log('\n════════════════════════════════════════', 'cyan');
  log('Test 4: Verify Invalid OTP (Error Handling)', 'cyan');
  log('════════════════════════════════════════', 'cyan');

  try {
    const response = await makeRequest('POST', '/users/verify-otp', {
      email: TEST_EMAIL,
      otpCode: '000000',
    });

    if (response.status === 400 || (response.data && !response.data.success)) {
      log('✅ PASS: Invalid OTP correctly rejected', 'green');
      return { success: true };
    } else {
      log('❌ FAIL: Invalid OTP was accepted (should be rejected)', 'red');
      log(`   Status: ${response.status}`, 'red');
      return { success: false };
    }
  } catch (error) {
    log(`❌ ERROR: ${error.message}`, 'red');
    return { success: false, error };
  }
}

async function testValidationErrors() {
  log('\n════════════════════════════════════════', 'cyan');
  log('Test 5: Validation Errors', 'cyan');
  log('════════════════════════════════════════', 'cyan');

  const tests = [
    {
      name: 'Empty email',
      endpoint: '/users/forgot-password',
      data: { email: '' },
      expectedStatus: 400,
    },
    {
      name: 'Invalid email format',
      endpoint: '/users/forgot-password',
      data: { email: 'invalid-email' },
      expectedStatus: 400,
    },
    {
      name: 'Invalid OTP length',
      endpoint: '/users/verify-otp',
      data: { email: TEST_EMAIL, otpCode: '123' },
      expectedStatus: 400,
    },
    {
      name: 'Short password',
      endpoint: '/users/reset-password',
      data: { email: TEST_EMAIL, otpCode: '123456', newPassword: '123' },
      expectedStatus: 400,
    },
  ];

  let passed = 0;
  let failed = 0;

  for (const test of tests) {
    try {
      const response = await makeRequest('POST', test.endpoint, test.data);
      if (response.status === test.expectedStatus) {
        log(`✅ PASS: ${test.name}`, 'green');
        passed++;
      } else {
        log(`❌ FAIL: ${test.name} (expected ${test.expectedStatus}, got ${response.status})`, 'red');
        failed++;
      }
    } catch (error) {
      log(`❌ ERROR: ${test.name} - ${error.message}`, 'red');
      failed++;
    }
  }

  return { passed, failed, total: tests.length };
}

async function main() {
  log('\n╔════════════════════════════════════════╗', 'blue');
  log('║  Password Reset API Test Suite        ║', 'blue');
  log('╚════════════════════════════════════════╝', 'blue');
  log(`\nTesting with email: ${TEST_EMAIL}`, 'yellow');

  // Check if server is running
  try {
    const healthUrl = new URL('/health', 'http://localhost:3003');
    const healthCheck = await new Promise((resolve, reject) => {
      const req = http.request(healthUrl, { method: 'GET' }, (res) => {
        let body = '';
        res.on('data', (chunk) => { body += chunk; });
        res.on('end', () => {
          try {
            resolve({ status: res.statusCode, data: JSON.parse(body) });
          } catch {
            resolve({ status: res.statusCode, data: body });
          }
        });
      });
      req.on('error', reject);
      req.end();
    });
    
    if (healthCheck.status !== 200) {
      log('\n❌ Backend server is not responding correctly', 'red');
      log('   Please ensure the backend is running: npm run dev', 'yellow');
      process.exit(1);
    }
    log('\n✅ Backend server is running', 'green');
  } catch (error) {
    log('\n❌ Cannot connect to backend server', 'red');
    log('   Please start the backend: cd Smart-Home-Backend && npm run dev', 'yellow');
    process.exit(1);
  }

  const otpCode = process.argv[3]; // Optional OTP code from command line

  // Run tests
  const test1 = await testForgotPassword();
  const test2 = await testVerifyOTP(otpCode);
  const test3 = otpCode ? await testResetPassword(otpCode) : { success: false, skipped: true };
  const test4 = await testInvalidOTP();
  const test5 = await testValidationErrors();

  // Summary
  log('\n╔════════════════════════════════════════╗', 'blue');
  log('║  Test Summary                           ║', 'blue');
  log('╚════════════════════════════════════════╝', 'blue');

  const results = [
    { name: 'Forgot Password', result: test1 },
    { name: 'Verify OTP', result: test2 },
    { name: 'Reset Password', result: test3 },
    { name: 'Invalid OTP Handling', result: test4 },
    { name: 'Validation Errors', result: test5 },
  ];

  let totalPassed = 0;
  let totalFailed = 0;
  let totalSkipped = 0;

  results.forEach((test) => {
    if (test.result.skipped) {
      log(`⚠️  ${test.name}: SKIPPED`, 'yellow');
      totalSkipped++;
    } else if (test.result.success) {
      log(`✅ ${test.name}: PASS`, 'green');
      totalPassed++;
    } else {
      log(`❌ ${test.name}: FAIL`, 'red');
      totalFailed++;
    }
  });

  if (test5.passed !== undefined) {
    log(`\n   Validation Tests: ${test5.passed}/${test5.total} passed`, 'cyan');
  }

  log(`\nTotal: ${totalPassed} passed, ${totalFailed} failed, ${totalSkipped} skipped`, 'cyan');

  if (!otpCode && test1.success) {
    log('\n📝 Note: Check backend logs for the OTP code', 'yellow');
    log(`   Then run: node scripts/test-password-reset.js ${TEST_EMAIL} <OTP_CODE>`, 'yellow');
  }

  process.exit(totalFailed > 0 ? 1 : 0);
}

main().catch((error) => {
  log(`\n❌ Fatal error: ${error.message}`, 'red');
  process.exit(1);
});

