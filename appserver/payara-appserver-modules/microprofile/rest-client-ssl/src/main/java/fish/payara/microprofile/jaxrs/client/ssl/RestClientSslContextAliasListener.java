/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.jaxrs.client.ssl;

import com.sun.enterprise.security.ssl.SSLUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;
import org.glassfish.internal.api.Globals;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static fish.payara.security.client.PayaraConstants.MP_CONFIG_CLIENT_CERTIFICATE_ALIAS;
import static fish.payara.security.client.PayaraConstants.REST_CLIENT_CERTIFICATE_ALIAS;

/**
 * This class implements RestClientListener to evaluate the alias property and set a custom sslContext
 * for the MicroProfile rest client
 */
public class RestClientSslContextAliasListener implements RestClientListener {

    private static final Logger logger = Logger.getLogger(RestClientSslContextAliasListener.class.getName());

    @Override
    public void onNewClient(Class<?> serviceInterface, RestClientBuilder restClientBuilder) {
        logger.log(Level.FINE, "Evaluating state of the RestClientBuilder after calling build method");
        Object objectProperty = restClientBuilder.getConfiguration()
                .getProperty(REST_CLIENT_CERTIFICATE_ALIAS);

        if (objectProperty instanceof String) {
            String alias = (String) objectProperty;
            logger.log(Level.INFO,
                    String.format("The alias: %s is available from the RestClientBuilder configuration", alias));
            SSLContext customSSLContext = buildSSlContext(alias);
            if (customSSLContext != null) {
                restClientBuilder.sslContext(customSSLContext);
            } else {
                logger.log(Level.INFO,
                        String.format("Although the alias: %s is configured, it could not be found in an available keystore", alias));
            }
        } else {
            Config config = getConfig();
            try {
                String alias = config.getValue(MP_CONFIG_CLIENT_CERTIFICATE_ALIAS,
                        String.class);
                if (alias != null) {
                    logger.log(Level.INFO, String.format("The alias: %s is available from the MP Config", alias));
                    SSLContext customSSLContext = buildSSlContext(alias);
                    if (customSSLContext != null) {
                        restClientBuilder.sslContext(customSSLContext);
                    } else {
                        logger.log(Level.INFO,
                                String.format("Although the alias: %s is configured, it could not be found in an available keystore", alias));
                    }
                }
            } catch (NoSuchElementException e) {
                logger.log(Level.FINE, String.format("The MP config property %s was not set",
                        MP_CONFIG_CLIENT_CERTIFICATE_ALIAS));
            }
        }
    }

    /**
     * This method evaluate the alias on the global keystore and return the corresponding SSLContext based on the alias
     * if not available the SSLContext should be the default that Jersey implementation set
     *
     * @param alias name of the certificate
     * @return the SSLContext with the corresponding certificate and alias name
     */
    protected SSLContext buildSSlContext(String alias) {
        logger.log(Level.FINE, "Building the SSLContext for the alias");
        try {
            KeyManager[] managers = getKeyManagers();
            Optional<X509KeyManager> optionalKeyManager = null;
            optionalKeyManager = Arrays.stream(managers).filter(m -> (m instanceof X509KeyManager))
                    .map(m -> ((X509KeyManager) m)).findFirst();
            KeyStore[] keyStores = getKeyStores();

            for (KeyStore ks : keyStores) {
                if (ks.containsAlias(alias) && optionalKeyManager.isPresent()) {
                    X509KeyManager customKeyManager = new SingleCertificateKeyManager(alias, optionalKeyManager.get());
                    SSLContext customSSLContext = SSLContext.getInstance("TLS");
                    customSSLContext.init(new KeyManager[]{customKeyManager}, null, null);
                    return customSSLContext;
                }
            }
        } catch (IOException e) {
            logger.severe("An IOException was thrown with the following message" + e.getMessage());
        } catch (KeyStoreException e) {
            logger.severe("A KeyStoreException was thrown with the following message" + e.getMessage());
        } catch (Exception e) {
            logger.severe("An Exception was thrown with the following message" + e.getMessage());
        }
        return null;
    }

    /**
     * Method to get the config
     * @return Config from MicroProfile
     */
    protected Config getConfig() {
        return ConfigProvider.getConfig();
    }

    /**
     * Method used to get KeyManagers
     * @return an array of KeyManager
     * @throws Exception
     */
    protected KeyManager[] getKeyManagers() throws Exception {
        SSLUtils sslUtils = Globals.get(SSLUtils.class);
        return sslUtils.getKeyManagers();
    }

    /**
     * Method used to get KeyStores
     * @return an array of KeyStore
     * @throws IOException
     */
    protected KeyStore[] getKeyStores() throws IOException {
        SSLUtils sslUtils = Globals.get(SSLUtils.class);
        return sslUtils.getKeyStores();
    }

    /**
     * This static class is a custom implementation of X509KeyManager to set the custom certificate based on the
     * alias property
     */
    private static class SingleCertificateKeyManager implements X509KeyManager {

        private String alias;
        private X509KeyManager keyManager;

        SingleCertificateKeyManager(String alias, X509KeyManager keyManager) {
            this.alias = alias;
            this.keyManager = keyManager;
        }

        @Override
        public String[] getClientAliases(String s, Principal[] principals) {
            return keyManager.getClientAliases(s, principals);
        }

        @Override
        public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
            return alias;
        }

        @Override
        public String[] getServerAliases(String s, Principal[] principals) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
            throw new UnsupportedOperationException();
        }

        @Override
        public X509Certificate[] getCertificateChain(String s) {
            return keyManager.getCertificateChain(s);
        }

        @Override
        public PrivateKey getPrivateKey(String s) {
            return keyManager.getPrivateKey(s);
        }
    }
}
