#!/bin/bash

set -euo pipefail

echo "🔍 Detecting Java trust store path..."

# Detect Java trust store path
if [ -f "${JAVA_HOME}/jre/lib/security/cacerts" ]; then
    JAVA_TRUSTSTORE="${JAVA_HOME}/jre/lib/security/cacerts"
elif [ -f "${JAVA_HOME}/lib/security/cacerts" ]; then
    JAVA_TRUSTSTORE="${JAVA_HOME}/lib/security/cacerts"
else
    echo "❌ ERROR: Unable to find Java trust store in ${JAVA_HOME}"
    exit 1
fi

echo "📌 Truststore detected at: $JAVA_TRUSTSTORE"

TRUSTSTORE_PASSWORD="changeit"
TEMP_STORE="updated-cacerts.jks"
DAYS_THRESHOLD=90

echo "🧹 Cleaning certificates expiring within ${DAYS_THRESHOLD} days..."

# Export all aliases
keytool -list -v -keystore "$JAVA_TRUSTSTORE" -storepass "$TRUSTSTORE_PASSWORD" | \
awk -v threshold=$(date -v+${DAYS_THRESHOLD}d +%s) '
    /Alias name:/ { alias=$NF }
    /Valid until:/ {
        gsub(",", "", $0);
        cmd = "date -j -f \"%b %e %T %Y %Z\" \"" $NF " 00:00:00 " $(NF-1) " UTC\" +%s"
        cmd | getline exp
        close(cmd)
        if (exp < threshold) {
            print alias
        }
    }
' > expired_aliases.txt

# Create a copy of original store to work on
cp "$JAVA_TRUSTSTORE" "$TEMP_STORE"

while IFS= read -r alias; do
    echo "⏳ Removing expiring cert: $alias"
    keytool -delete -alias "$alias" -keystore "$TEMP_STORE" -storepass "$TRUSTSTORE_PASSWORD" || true
done < expired_aliases.txt

echo "✅ Removed $(wc -l < expired_aliases.txt) expired certs"

echo "🌐 Downloading latest Mozilla CA bundle..."
CA_CERTS_PATH="/tmp/ca-certificates.crt"
curl -fsSL -o "$CA_CERTS_PATH" https://curl.se/ca/cacert.pem

if [ ! -s "$CA_CERTS_PATH" ]; then
    echo "❌ Failed to download CA certs."
    exit 1
fi

echo "➕ Importing non-duplicate Mozilla certs..."
csplit -z -f mozilla-cert- "$CA_CERTS_PATH" '/-----BEGIN CERTIFICATE-----/' '{*}' > /dev/null 2>&1

for cert in mozilla-cert-*; do
    fingerprint=$(openssl x509 -in "$cert" -noout -fingerprint -sha1 | cut -d'=' -f2 | tr -d ':')

    exists=$(keytool -list -keystore "$TEMP_STORE" -storepass "$TRUSTSTORE_PASSWORD" | grep -i "$fingerprint" || true)

    if [ -z "$exists" ]; then
        alias="mozilla-${fingerprint:0:12}"
        echo "🔐 Importing cert: $alias"
        keytool -importcert -noprompt -keystore "$TEMP_STORE" \
            -storepass "$TRUSTSTORE_PASSWORD" -file "$cert" -alias "$alias"
    else
        echo "⚠️ Skipped duplicate cert with fingerprint $fingerprint"
    fi
done

echo "🛠️ Replacing original truststore with cleaned one..."
mv "$TEMP_STORE" "$JAVA_TRUSTSTORE"

echo "✅ Java truststore successfully updated."
rm -f mozilla-cert-* expired_aliases.txt "$CA_CERTS_PATH"
