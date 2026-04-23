#!/usr/bin/env bash
set -e

PROTO_DIR="$(dirname "$0")/../shared/proto"
OUT_DIR="$(dirname "$0")/../shared/proto-gen"

mkdir -p "$OUT_DIR"

npx pbjs \
  -t static-module \
  -w es6 \
  --es6 \
  -o "$OUT_DIR/proto.js" \
  "$PROTO_DIR"/*.proto

npx pbts \
  -o "$OUT_DIR/proto.d.ts" \
  "$OUT_DIR/proto.js"

echo "Proto files generated in $OUT_DIR"
