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
package fish.payara.microprofile.jaxrs.client.ssl;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import java.io.*;
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

import static fish.payara.microprofile.jaxrs.client.ssl.PayaraConstants.*;

/**
 * This class implements RestClientListener to evaluate the alias property and set a custom sslContext
 * for the MicroProfile rest client
 */
public class RestClientSslContextAliasListener implements RestClientListener {

    private static final Logger logger = Logger.getLogger(RestClientSslContextAliasListener.class.getName());

    @Override
    public void onNewClient(Class<?> serviceInterface, RestClientBuilder restClientBuilder) {
        logger.log(Level.INFO,"Evaluating state of the RestClientBuilder after calling build method");

        Object objectProperty = restClientBuilder.getConfiguration()
                .getProperty(PAYARA_REST_CLIENT_CERTIFICATE_ALIAS);

        if (objectProperty != null && objectProperty instanceof String) {
            String alias = (String) objectProperty;
            logger.log(Level.INFO,"The alias is available from the RestClientBuilder configuration");
            SSLContext customSSLContext = buildSSlContext(alias);
            if (customSSLContext != null) {
                restClientBuilder.sslContext(customSSLContext);
            }
        } else {
            Config config = ConfigProvider.getConfig();
            try {
                String alias = config.getValue(PAYARA_MP_CONFIG_CLIENT_CERTIFICATE_ALIAS,
                        String.class);
                if (alias != null) {
                    logger.log(Level.INFO,"The alias is available from the MP Config");
                    SSLContext customSSLContext = buildSSlContext(alias);
                    if (customSSLContext != null) {
                        restClientBuilder.sslContext(customSSLContext);
                    }
                }
            } catch(NoSuchElementException e) {
                logger.log(Level.SEVERE, String.format("The MP config property %s was not set",
                        PAYARA_MP_CONFIG_CLIENT_CERTIFICATE_ALIAS));
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
        logger.log(Level.INFO, "Building the SSLContext for the alias");
        String configPath = System.getProperty(PAYARA_BASEDIR_PROPERTY_NAME);
        logger.log(Level.INFO, "Basedir value:"+configPath);
        if (configPath != null) {
            File file = new File(configPath.concat(PAYARA_KEYSTORE_NAME));
            logger.log(Level.INFO, file.getPath());
            try (InputStream is = new FileInputStream(file)) {
                String password = System.getProperty(PAYARA_KEYSTORE_PASSWORD_PROPERTY_NAME);
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                if (password != null) {
                    ks.load(is, password.toCharArray());
                    if (ks.containsAlias(alias)) {
                        logger.log(Level.INFO, "The alias was found to configure the custom SSLContext");
                        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        kmf.init(ks, password.toCharArray());
                        Optional<X509KeyManager> optionalKeyManager = null;
                        X509KeyManager keyManager = null;
                        optionalKeyManager = Arrays.stream(kmf.getKeyManagers()).filter(m -> (m instanceof X509KeyManager))
                                .map(m -> ((X509KeyManager) m)).findFirst();

                        if (optionalKeyManager.isPresent()) {
                            keyManager = optionalKeyManager.get();
                        }

                        if (keyManager != null) {
                            logger.log(Level.INFO, "Creating custom sslContext for alias "+alias);
                            X509KeyManager customKeyManager = new SingleCertificateKeyManager(alias, keyManager);
                            SSLContext customSSLContext = SSLContext.getInstance("TLS");
                            customSSLContext.init(new KeyManager[]{customKeyManager}, null, null);
                            return customSSLContext;
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                logger.log(Level.SEVERE, "While configuring custom SSLContext a FileNotFoundException was thrown with the following message" +
                        e.getMessage());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "While configuring custom SSLContext an IOException was thrown with the following message" +
                        e.getMessage());
            } catch (KeyStoreException e) {
                logger.log(Level.SEVERE, "While configuring custom SSLContext a KeyStoreException was thrown with the following message" +
                        e.getMessage());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "While configuring custom SSLContext An Exception was thrown with the following message" +
                        e.getMessage());
            }
        }
        return null;
    }

    /**
     * This class is a custom implementation of X509KeyManager to set the custom certificate based on the
     * alias property
     */
    private class SingleCertificateKeyManager implements X509KeyManager {

        private String alias;
        private X509KeyManager keyManager;

        SingleCertificateKeyManager(String alias, X509KeyManager keyManager) {
            this.alias = alias;
            this.keyManager = keyManager;
        }

        @Override
        public String[] getClientAliases(String s, Principal[] principals) {
            return new String[]{alias};
        }

        @Override
        public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
            return null;
        }

        @Override
        public String[] getServerAliases(String s, Principal[] principals) {
            return new String[]{alias};
        }

        @Override
        public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
            return null;
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
