#!/bin/bash

set -euo pipefail

TRUSTSTORE_PASSWORD="changeit"

echo "🔍 Detecting Java trust store path..."

# Detect Java trust store path
if [[ -f "${JAVA_HOME}/jre/lib/security/cacerts" ]]; then
    JAVA_TRUSTSTORE="${JAVA_HOME}/jre/lib/security/cacerts"
elif [[ -f "${JAVA_HOME}/lib/security/cacerts" ]]; then
    JAVA_TRUSTSTORE="${JAVA_HOME}/lib/security/cacerts"
else
    echo "❌ Could not locate Java truststore under JAVA_HOME."
    exit 1
fi

echo "📌 Detected Trust Store: $JAVA_TRUSTSTORE"

# Copy truststore to temp p12 file
TEMP_TRUSTSTORE="temp-cacerts.p12"
cp "$JAVA_TRUSTSTORE" "$TEMP_TRUSTSTORE"

# === Remove certificates expiring within 90 days ===
echo "🧹 Removing certificates expiring within 90 days..."

keytool -list -v -keystore "$TEMP_TRUSTSTORE" -storepass "$TRUSTSTORE_PASSWORD" -storetype PKCS12 > certs-info.txt 2>/dev/null

expired_aliases=()
alias=""
while IFS= read -r line; do
    if [[ "$line" == Alias\ name:* ]]; then
        alias=$(echo "$line" | awk '{print $3}')
    fi

    if [[ "$line" == *"Valid from:"* ]]; then
        expiry=$(echo "$line" | sed -n 's/.*until: //p')
        if [[ -n "$expiry" ]]; then
            expiry_ts=$(date -d "$expiry" +%s 2>/dev/null || true)
            now_ts=$(date +%s)
            if [[ "$expiry_ts" =~ ^[0-9]+$ ]] && (( expiry_ts < now_ts + 90*24*3600 )); then
                expired_aliases+=("$alias")
            fi
        fi
    fi
done < certs-info.txt

for alias in "${expired_aliases[@]}"; do
    if [[ -n "$alias" ]]; then
        echo "🗑️ Removing: $alias"
        keytool -delete -alias "$alias" -keystore "$TEMP_TRUSTSTORE" -storepass "$TRUSTSTORE_PASSWORD" -storetype PKCS12 || true
    fi
done

# === Import Mozilla CA Certs (avoiding duplicates) ===
echo "🌐 Downloading Mozilla CA certificates..."
curl -sS -o mozilla.pem https://curl.se/ca/cacert.pem

csplit -s mozilla.pem '/-----BEGIN CERTIFICATE-----/' '{*}' || true

echo "➕ Importing Mozilla certs (no duplicates)..."
for cert in cert-*; do
    # Skip cert-00 which is often invalid
    if [[ "$cert" == "cert-00" ]]; then
        continue
    fi

    if openssl x509 -in "$cert" -noout > /dev/null 2>&1; then
        fingerprint=$(openssl x509 -noout -in "$cert" -fingerprint -sha256 | cut -d'=' -f2 | tr -d ':')
        if [ -n "$fingerprint" ]; then
            exists=$(keytool -list -keystore "$TEMP_TRUSTSTORE" -storepass "$TRUSTSTORE_PASSWORD" -storetype PKCS12 -v | grep -i "$fingerprint" || true)
            if [ -z "$exists" ]; then
                alias="mozilla-$(basename "$cert")"
                keytool -importcert -keystore "$TEMP_TRUSTSTORE" -storepass "$TRUSTSTORE_PASSWORD" -storetype PKCS12 -noprompt -file "$cert" -alias "$alias" || echo "⚠️ Failed to import $cert"
            fi
        fi
    else
        echo "⚠️ Skipping invalid cert: $cert"
    fi
done

# Cleanup
rm -f cert-* mozilla.pem certs-info.txt

# === Replace original truststore ===
cp -f "$TEMP_TRUSTSTORE" "$JAVA_TRUSTSTORE"
echo "✅ Updated truststore at: $JAVA_TRUSTSTORE"

# === Copy to Payara paths (optional, if they exist) ===
echo "📁 Copying to Payara truststore paths (if applicable)..."
PAYARA_PATHS=(
    "../../admin/template/src/main/resources/config/cacerts.p12"
    "src/main/resources/config/cacerts.p12"
)

for path in "${PAYARA_PATHS[@]}"; do
    if [ -f "$path" ]; then
        cp -f "$TEMP_TRUSTSTORE" "$path"
        echo "✅ Copied to: $path"
    else
        echo "⚠️ Skipped: $path not found"
    fi
done

rm temp-cacerts.p12
rm xx*
git status

# === Preview ===
echo "🔎 Final truststore entries:"
keytool -list -keystore "$JAVA_TRUSTSTORE" -storepass "$TRUSTSTORE_PASSWORD" -storetype PKCS12 | head -n 10

echo "🏁 Trust store update complete."