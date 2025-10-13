/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import fish.payara.microprofile.jwtauth.jwt.JsonWebTokenImpl;
import fish.payara.microprofile.jwtauth.jwt.JwtTokenParser;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.jwt.config.Names;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import static jakarta.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;
import static java.lang.Thread.currentThread;
import static java.util.logging.Level.INFO;
import static org.eclipse.microprofile.jwt.config.Names.ISSUER;

/**
 * Identity store capable of asserting that a signed JWT token is valid
 * according to the MP-JWT 1.1 spec.
 *
 * @author Arjan Tijms
 */
public class SignedJWTIdentityStore implements IdentityStore {

    private static final Logger LOGGER = Logger.getLogger(SignedJWTIdentityStore.class.getName());

    private final String acceptedIssuer;
    private final Optional<List<String>> allowedAudience;
    private final Optional<Boolean> enabledNamespace;
    private final Optional<String> customNamespace;
    private final Optional<Boolean> disableTypeVerification;

    private final Config config;
    private final JwtPublicKeyStore publicKeyStore;
    private final JwtPrivateKeyStore privateKeyStore;

    private final boolean isEncryptionRequired;

    private Map<String, Optional<String>> optionalConfigProperty;

    public SignedJWTIdentityStore() {
        config = ConfigProvider.getConfig();

        Optional<Properties> properties = readVendorProperties();
        acceptedIssuer = readVendorIssuer(properties)
                .orElseGet(() -> config.getOptionalValue(ISSUER, String.class)
                .orElseThrow(() -> new IllegalStateException("No issuer found")));
        Optional<String> allowedAudienceOpt = readAudience(properties);
        if (!allowedAudienceOpt.isPresent()) {
            allowedAudienceOpt = config.getOptionalValue(Names.AUDIENCES, String.class);
        }
        allowedAudience = allowedAudienceOpt.map(str -> Arrays.asList(str.split(",")));

        enabledNamespace = readEnabledNamespace(properties);
        customNamespace = readCustomNamespace(properties);
        disableTypeVerification = readDisableTypeVerification(properties);
        Optional<String> publicKeyLocation = readConfigOptional(Names.VERIFIER_PUBLIC_KEY_LOCATION, properties, config); //mp.jwt.verifyAndParseEncryptedJWT.publickey.location
        Optional<String> publicKey = readConfigOptional(Names.VERIFIER_PUBLIC_KEY, properties, config); //mp.jwt.verifyAndParseEncryptedJWT.publickey
        Optional<String> decryptKeyLocation = readConfigOptional(Names.DECRYPTOR_KEY_LOCATION, properties, config); //mp.jwt.decrypt.key.location
        publicKeyStore = new JwtPublicKeyStore(readPublicKeyCacheTTL(properties), publicKeyLocation);
        privateKeyStore = new JwtPrivateKeyStore(readPublicKeyCacheTTL(properties), decryptKeyLocation);

        // Signing is required by default, it doesn't parse if not signed
        isEncryptionRequired = decryptKeyLocation.isPresent();
        setOptionalConfigProperty(properties);
    }

    public CredentialValidationResult validate(SignedJWTCredential signedJWTCredential) {
        final JwtTokenParser jwtTokenParser = new JwtTokenParser(enabledNamespace, customNamespace, disableTypeVerification);
        try {
            JsonWebTokenImpl jsonWebToken = jwtTokenParser.parse(signedJWTCredential.getSignedJWT(),
                    isEncryptionRequired, publicKeyStore, acceptedIssuer, privateKeyStore, optionalConfigProperty);

            // verifyAndParseEncryptedJWT audience
            final Set<String> recipientsOfThisJWT = jsonWebToken.getAudience();
            // find if any recipient is in the allowed audience
            Boolean recipientInAudience = allowedAudience
                    .map(recipient -> recipient.stream().anyMatch(a -> recipientsOfThisJWT != null && recipientsOfThisJWT.contains(a)))
                    .orElse(true);
            if (!recipientInAudience) {
                throw new Exception("The intended audience " + recipientsOfThisJWT + " is not a part of allowed audience.");
            }

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

    public static Optional<Properties> readVendorProperties() {
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
    
    private Optional<String> readAudience(Optional<Properties> properties) {
        return properties.isPresent() ? Optional.ofNullable(properties.get().getProperty(Names.AUDIENCES)) : Optional.empty();
    }

    /**
     * Read configuration from Vendor or server or return default value.
     */
    public static String readConfig(String key, Optional<Properties> properties, Config config, String defaultValue) {
        Optional<String> valueOpt = readConfigOptional(key, properties, config);
        return valueOpt.orElse(defaultValue);
    }

    public static Optional<String> readConfigOptional(String key, Optional<Properties> properties, Config config) {
        Optional<String> valueOpt = properties.map(props -> props.getProperty(key));
        if (!valueOpt.isPresent()) {
            valueOpt = config.getOptionalValue(key, String.class);
        }
        return valueOpt;
    }

    private void setOptionalConfigProperty(Optional<Properties> properties) {
        optionalConfigProperty = new HashMap<>();
        optionalConfigProperty.put(Names.TOKEN_AGE, readConfigOptional(Names.TOKEN_AGE, properties, config)); // mp.jwt.verify.token.age
        optionalConfigProperty.put(Names.CLOCK_SKEW, readConfigOptional(Names.CLOCK_SKEW, properties, config)); // mp.jwt.verify.clock.skew
        optionalConfigProperty.put(Names.DECRYPTOR_KEY_ALGORITHM, readConfigOptional(Names.DECRYPTOR_KEY_ALGORITHM, properties, config)); //mp.jwt.decrypt.key.algorithm
    }
}
