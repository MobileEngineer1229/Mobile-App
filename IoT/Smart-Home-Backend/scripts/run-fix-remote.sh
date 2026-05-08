#!/bin/bash
# Script to fix demo user passwords on remote database
# 
# Usage:
#   chmod +x scripts/run-fix-remote.sh
#   DB_HOST=172.86.88.76 DB_USER=postgres DB_PASSWORD=your_password DB_NAME=smart_home_db ./scripts/run-fix-remote.sh

set -e

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-213515}
DB_NAME=${DB_NAME:-smart_home_db}

echo "🔗 Connecting to remote database..."
echo "   Host: $DB_HOST"
echo "   Port: $DB_PORT"
echo "   Database: $DB_NAME"
echo ""

# Export for Node.js script
export DB_HOST DB_PORT DB_USER DB_PASSWORD DB_NAME

# Run the fix script
node scripts/fix-remote-demo-passwords.js
