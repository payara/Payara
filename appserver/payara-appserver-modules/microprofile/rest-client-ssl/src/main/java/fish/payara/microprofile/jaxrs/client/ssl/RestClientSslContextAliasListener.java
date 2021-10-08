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

import com.sun.enterprise.security.ssl.SSLUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;
import org.glassfish.hk2.api.ServiceLocator;
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
import java.util.Optional;
import java.util.logging.Logger;

/**
 * This class implements RestClientBuilder to evaluate the alias property to add the sslContext for the MicroProfile
 * rest client
 */
public class RestClientSslContextAliasListener implements RestClientListener {

    private static final Logger logger = Logger.getLogger(RestClientSslContextAliasListener.class.getName());

    @Override
    public void onNewClient(Class<?> serviceInterface, RestClientBuilder restClientBuilder) {
        logger.info("Starting the evaluation of the alias property to set the sslContext");

        Object objectProperty = restClientBuilder.getConfiguration()
                .getProperty(PayaraConstants.PAYARA_REST_CLIENT_CERTIFICATE_ALIAS);

        if (objectProperty != null && objectProperty instanceof String) {
            String alias = (String) objectProperty;
            logger.info("The alias is available from the RestClientBuilder configuration");
            SSLContext customSSLContext = buildSSlContext(alias);
            if (customSSLContext != null) {
                restClientBuilder.sslContext(customSSLContext);
            }
        } else {
            Config config = ConfigProvider.getConfig();
            String alias = config.getValue("certificate.alias", String.class);
            if (alias != null) {
                logger.info("The alias is available from the MP Config");
                SSLContext customSSLContext = buildSSlContext(alias);
                if (customSSLContext != null) {
                    restClientBuilder.sslContext(customSSLContext);
                }
            }
        }
    }

    /**
     * This method evaluate the alias on the global keystore and return the corresponding SSLContext based on the alias
     * if not available the SSLContest should be the default that was set during the payara server configuration
     * @param alias name of the certificate
     * @return the SSLContext with the corresponding certificate and alias name
     */
    protected SSLContext buildSSlContext(String alias) {
        try {
            logger.info("Building the SSLContext for the alias");
            ServiceLocator habitat = Globals.getDefaultHabitat();
            SSLUtils sslUtils = habitat.getService(SSLUtils.class);
            KeyManager[] managers = sslUtils.getKeyManagers();
            Optional<X509KeyManager> optionalKeyManager = null;
            X509KeyManager keyManager = null;
            optionalKeyManager = Arrays.stream(managers).filter(m -> (m instanceof X509KeyManager))
                    .map(m -> ((X509KeyManager)m)).findFirst();

            KeyStore[] keyStores = sslUtils.getKeyStores();

            if(optionalKeyManager.isPresent()) {
                keyManager = optionalKeyManager.get();
            }

            for (KeyStore ks : keyStores) {
                if (ks.containsAlias(alias) && keyManager != null) {
                    X509KeyManager customKeyManager = new SingleCertificateKeyManager(alias, keyManager);
                    SSLContext customSSLContext = SSLContext.getInstance("TLS");
                    customSSLContext.init(new KeyManager[]{customKeyManager}, null, null);
                    return customSSLContext;
                }
            }
            return null;
        } catch (IOException e) {
            logger.severe("An IOException was thrown with the following message"+e.getMessage());
        } catch (KeyStoreException e) {
            logger.severe("A KeyStoreException was thrown with the following message"+e.getMessage());
        } catch (Exception e) {
            logger.severe("An Exception was thrown with the following message"+e.getMessage());
        }
        return null;
    }

    /**
     * This class is a custom implementation of rht X509KeyManager to set the custom certificate based on the
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
