#!/bin/bash

set -euo pipefail

echo "🔍 Detecting Java trust store path..."

# Set your custom Java trust store (already updated one)
JAVA_TRUSTSTORE="/Users/abdulrahim/cacerts"

echo "📌 Using custom trust store: $JAVA_TRUSTSTORE"

# Generate Payara-compatible PKCS12 trust store
echo "🛠️ Generating Payara truststore (cacerts.p12) from Java truststore..."
keytool -importkeystore \
    -srckeystore "$JAVA_TRUSTSTORE" \
    -srcstorepass "changeit" \
    -destkeystore "cacerts.p12" \
    -deststorepass "changeit" \
    -deststoretype pkcs12 \
    -noprompt

echo "📁 Replacing trust stores in Payara codebase..."

PAYARA_P12_PATHS=(
    "nucleus/admin/template/src/main/resources/config/cacerts.p12"
    "nucleus/security/core/src/main/resources/config/cacerts.p12"
)

for path in "${PAYARA_P12_PATHS[@]}"; do
    if [ -f "$path" ]; then
        cp -f "cacerts.p12" "$path"
        echo "✅ Replaced: $path"
    else
        echo "⚠️ Skipped: $path not found"
    fi
done

echo "🔎 Verifying generated truststore..."
keytool -list -keystore "cacerts.p12" -storepass "changeit" | head -n 10

echo "🏁 Trust store replacement complete."
