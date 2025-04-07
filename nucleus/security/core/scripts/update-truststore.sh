#!/bin/bash

set -euo pipefail

echo "🔍 Detecting Java trust store path..."

if [ -f "${JAVA_HOME}/jre/lib/security/cacerts" ]; then
    JAVA_TRUSTSTORE="${JAVA_HOME}/jre/lib/security/cacerts"
elif [ -f "${JAVA_HOME}/lib/security/cacerts" ]; then
    JAVA_TRUSTSTORE="${JAVA_HOME}/lib/security/cacerts"
else
    echo "❌ ERROR: Java trust store not found."
    exit 1
fi

echo "📌 Java Trust Store: $JAVA_TRUSTSTORE"

# Copy truststore to a working file
WORKING_TRUSTSTORE="working-cacerts.jks"
cp "$JAVA_TRUSTSTORE" "$WORKING_TRUSTSTORE"

echo "🧹 Removing certificates expiring within 90 days..."
keytool -list -v -keystore "$WORKING_TRUSTSTORE" -storepass changeit | awk '
    BEGIN { RS="\n\n"; }
    /Valid from:/ {
        match($0, /until: ([^,\n]+)/, exp);
        cmd = "date -j -f \"%b %e, %Y\" \"" exp[1] "\" +%s 2>/dev/null";
        cmd | getline exp_epoch;
        close(cmd);
        now=systime();
        if (exp_epoch < now + (90*86400)) {
            match($0, /Alias name: ([^\n]+)/, alias);
            print alias[1];
        }
    }' | while read -r alias; do
        echo "🗑️ Removing soon-to-expire cert: $alias"
        keytool -delete -alias "$alias" -keystore "$WORKING_TRUSTSTORE" -storepass changeit || true
    done

echo "🌐 Downloading Mozilla CA bundle..."
curl -fsSL -o /tmp/cacert.pem https://curl.se/ca/cacert.pem

echo "➕ Importing Mozilla certs (avoiding duplicates)..."
csplit -z -f cert- /tmp/cacert.pem '/-BEGIN CERTIFICATE-/' '{*}' >/dev/null 2>&1

for cert in cert-*; do
    fingerprint=$(openssl x509 -noout -fingerprint -in "$cert" | sed 's/.*=//;s/://g')
    alias="moz-$fingerprint"

    if keytool -list -keystore "$WORKING_TRUSTSTORE" -storepass changeit -alias "$alias" >/dev/null 2>&1; then
        echo "🔁 Skipping existing cert: $alias"
    else
        echo "➕ Importing: $alias"
        keytool -import -noprompt -trustcacerts -keystore "$WORKING_TRUSTSTORE" -storepass changeit -alias "$alias" -file "$cert"
    fi
done

rm cert-* /tmp/cacert.pem

echo "🔐 Converting to PKCS12 format (cacerts.p12)..."
keytool -importkeystore \
    -srckeystore "$WORKING_TRUSTSTORE" \
    -srcstorepass changeit \
    -destkeystore "cacerts.p12" \
    -deststorepass changeit \
    -deststoretype pkcs12 \
    -noprompt

echo "📁 Copying truststore to Payara codebase paths..."

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
keytool -list -keystore "cacerts.p12" -storepass changeit | head -n 10

echo "🏁 Trust store update and replacement complete."
