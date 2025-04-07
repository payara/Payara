#!/bin/bash

set -euo pipefail

echo "🔍 Detecting Java trust store path..."

# Locate Java trust store
if [ -f "${JAVA_HOME}/jre/lib/security/cacerts" ]; then
    JAVA_TRUSTSTORE="${JAVA_HOME}/jre/lib/security/cacerts"
elif [ -f "${JAVA_HOME}/lib/security/cacerts" ]; then
    JAVA_TRUSTSTORE="${JAVA_HOME}/lib/security/cacerts"
else
    echo "❌ ERROR: Unable to find Java trust store. Exiting..."
    exit 1
fi

echo "📌 Java Trust Store Path: $JAVA_TRUSTSTORE"

# Download Mozilla CA bundle
CA_CERTS_PATH="/tmp/ca-certificates.crt"
echo "🌐 Downloading Mozilla CA certificates..."
curl -fsSL -o "$CA_CERTS_PATH" https://curl.se/ca/cacert.pem

if [ ! -s "$CA_CERTS_PATH" ]; then
    echo "❌ Failed to download CA certificates. Exiting..."
    exit 1
fi
echo "✅ Downloaded Mozilla CA certificates."

# Import into Java truststore
echo "🔐 Updating Java trust store with Mozilla certs..."
awk 'split_after == 1 {n++;split_after=0} /-----END CERTIFICATE-----/ {split_after=1}
     {print > "/tmp/cert" n ".pem"}' "$CA_CERTS_PATH"

for cert in /tmp/cert*.pem; do
    alias=$(basename "$cert" .pem)
    keytool -importcert -noprompt -trustcacerts \
        -keystore "$JAVA_TRUSTSTORE" \
        -storepass "changeit" \
        -file "$cert" \
        -alias "moz-${alias}" || true
done

echo "✅ Java trust store updated with Mozilla certs."

# Convert to PKCS12
echo "🛠️ Generating Payara truststore (cacerts.p12)..."
keytool -importkeystore \
    -srckeystore "$JAVA_TRUSTSTORE" \
    -srcstorepass "changeit" \
    -destkeystore "cacerts.p12" \
    -deststorepass "changeit" \
    -deststoretype pkcs12 \
    -noprompt

# Replace in Payara
echo "📁 Replacing trust store in Payara..."
PAYARA_P12_PATHS=(
    "nucleus/admin/template/src/main/resources/config/cacerts.p12"
    "nucleus/security/core/src/main/resources/config/cacerts.p12"
)

for path in "${PAYARA_P12_PATHS[@]}"; do
    cp -f "cacerts.p12" "$path" && echo "✅ Replaced: $path"
done

echo "🔎 Verifying updated trust store..."
keytool -list -keystore "cacerts.p12" -storepass "changeit" | head -n 10

echo "🏁 All done."
