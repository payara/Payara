# Client Certificate Functional Tests

This directory contains functional tests for validating Client Certificate Authentication in Payara Server, specifically verifying behavior with the Payara JAX-RS Extension and server-side validation settings.

## Modules

### 1. Client Certificate Validation Enabled (`client-certificate-validation-enabled`)
*   **Purpose**: Validates standard client certificate authentication flows.
*   **Scenarios**:
    *   **Valid Certificate**: Verifies that a valid client certificate correctly authenticates the user and maps to the expected principal.
    *   **Expired Certificate**: Verifies that an expired certificate is **rejected** by the server and logs the expected error message (`Certificate Validation Failed via API`).
*   **Configuration**: 
    *   Sets `client-auth=want` on the HTTP listener.
    *   Sets `configs.config.server-config.security-service.auth-realm.certificate.property.assign-groups=myRole`

### 2. Client Certificate Validation Disabled (`client-certificate-validation-disabled`)
*   **Purpose**: Verifies the specific Payara configuration property `certificate-validation` which allows bypassing certificate expiration checks.
*   **Scenarios**:
    *   **Expired Certificate (Allowed)**: Configuring `certificate-validation=false` at the `auth-realm` level allows an expired certificate to be accepted. The test asserts that a request with an expired certificate returns `200 OK` instead of `401 Unauthorized`.
*   **Configuration**: 
    *   Sets `client-auth=want` on the HTTP listener.
    *   Sets `configs.config.server-config.security-service.auth-realm.certificate.property.certificate-validation=false`.
    *   Sets `configs.config.server-config.security-service.auth-realm.certificate.property.assign-groups=myRole`

---

## Prerequisites

*   **JDK**: Java 21 or higher.
*   **Payara Server**: A valid installation of Payara Server 7.

## How to Run

These tests are designed to be run against a running or remote Payara Server instance using the `payara-server-remote` Maven profile.

The following command template is recommended for running the tests, ensuring all necessary system properties and build flags are set.

### Running Client Certificate Validation Enabled Tests
This test expects standard certificate validation to be **active** (default).

```bash
mvn -V -B -ff install --strict-checksums \
    -Ppayara-server-remote \
    -Djavax.net.ssl.trustStore=/path/to/your/jdk/lib/security/cacerts \
    -Djavax.xml.accessExternalSchema=all \
    -Dpayara.home=/path/to/payara7 \
    -f appserver/tests/functional/client-certificate/client-certificate-validation-enabled
```

### Running Client Certificate Validation Disabled Tests
This test **modifies the server configuration** to disable certificate expiration checks.

```bash
mvn -V -B -ff install --strict-checksums \
    -Ppayara-server-remote \
    -Djavax.net.ssl.trustStore=/path/to/your/jdk/lib/security/cacerts \
    -Djavax.xml.accessExternalSchema=all \
    -Dpayara.home=/path/to/payara7 \
    -f appserver/tests/functional/client-certificate/client-certificate-validation-disabled
```
> **Tip**: Once you have executed a module's tests as above at least once, you can add `-DskipConfig=true` to skip the re-configuration and restart of the Payara instance. This can be useful when debugging.

> **Note**: Update `/path/to/your/jdk/lib/security/cacerts` and `/path/to/payara7` with your actual system paths. The `javax.net.ssl.trustStore` property ensures the test runner trusts the server's certificate if using a custom or self-signed CA.

