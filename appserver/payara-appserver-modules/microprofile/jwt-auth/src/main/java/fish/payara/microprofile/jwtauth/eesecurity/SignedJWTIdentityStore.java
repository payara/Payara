/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.jwtauth.eesecurity;

import static java.lang.Thread.currentThread;
import static java.util.logging.Level.INFO;
import static jakarta.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;
import static org.eclipse.microprofile.jwt.config.Names.ISSUER;

import java.io.IOException;
import java.net.URL;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import fish.payara.microprofile.jwtauth.jwt.JsonWebTokenImpl;
import fish.payara.microprofile.jwtauth.jwt.JwtTokenParser;

/**
 * Identity store capable of asserting that a signed JWT token is valid
 * according to the MP-JWT 1.1 spec.
 *
 * @author Arjan Tijms
 */
public class SignedJWTIdentityStore implements IdentityStore {

    private static final Logger LOGGER = Logger.getLogger(SignedJWTIdentityStore.class.getName());

    private final String acceptedIssuer;
    private final Optional<Boolean> enabledNamespace;
    private final Optional<String> customNamespace;
    private final Optional<Boolean> disableTypeVerification;

    private final Config config;
    private final JwtPublicKeyStore publicKeyStore;

    public SignedJWTIdentityStore() {
        config = ConfigProvider.getConfig();

        Optional<Properties> properties = readVendorProperties();
        acceptedIssuer = readVendorIssuer(properties)
                .orElseGet(() -> config.getOptionalValue(ISSUER, String.class)
                .orElseThrow(() -> new IllegalStateException("No issuer found")));

        enabledNamespace = readEnabledNamespace(properties);
        customNamespace = readCustomNamespace(properties);
        disableTypeVerification = readDisableTypeVerification(properties);
        publicKeyStore = new JwtPublicKeyStore(readPublicKeyCacheTTL(properties));
    }

    public CredentialValidationResult validate(SignedJWTCredential signedJWTCredential) {
        final JwtTokenParser jwtTokenParser = new JwtTokenParser(enabledNamespace, customNamespace, disableTypeVerification);
        try {
            jwtTokenParser.parse(signedJWTCredential.getSignedJWT());
            String keyID = jwtTokenParser.getKeyID();

            PublicKey publicKey = publicKeyStore.getPublicKey(keyID);

            JsonWebTokenImpl jsonWebToken
                    = jwtTokenParser.verify(acceptedIssuer, publicKey);

            Set<String> groups = new HashSet<>();
            Collection<String> groupClaims = jsonWebToken.getClaim("groups");
            if (groupClaims != null) {
                groups.addAll(groupClaims);
            }

            return new CredentialValidationResult(jsonWebToken, groups);

        } catch (Exception e) {
            LOGGER.log(INFO, "Exception trying to parse JWT token.", e);
        }

        return INVALID_RESULT;
    }

    private Optional<Properties> readVendorProperties() {
        URL mpJwtResource = currentThread().getContextClassLoader().getResource("/payara-mp-jwt.properties");
        Properties properties = null;
        if (mpJwtResource != null) {
            try {
                properties = new Properties();
                properties.load(mpJwtResource.openStream());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load Vendor properties from resource file", e);
            }
        }
        return Optional.ofNullable(properties);
    }

    private Optional<String> readVendorIssuer(Optional<Properties> properties) {
        return properties.isPresent() ? Optional.ofNullable(properties.get().getProperty("accepted.issuer")) : Optional.empty();
    }

    private Optional<Boolean> readEnabledNamespace(Optional<Properties> properties) {
        return properties.isPresent() ? Optional.ofNullable(Boolean.valueOf(properties.get().getProperty("enable.namespace", "false"))) : Optional.empty();
    }

    private Optional<String> readCustomNamespace(Optional<Properties> properties) {
        return properties.isPresent() ? Optional.ofNullable(properties.get().getProperty("custom.namespace", null)) : Optional.empty();
    }

    private Optional<Boolean> readDisableTypeVerification(Optional<Properties> properties) {
        return properties.isPresent() ? Optional.ofNullable(Boolean.valueOf(properties.get().getProperty("disable.type.verification", "false"))) : Optional.empty();
    }
    
    private Duration readPublicKeyCacheTTL(Optional<Properties> properties) {
        return properties
        		.map(props -> props.getProperty("publicKey.cache.ttl"))
        		.map(Long::valueOf)
        		.map(Duration::ofMillis)
        		.orElseGet( () -> Duration.ofMinutes(5));
    }

    
}
