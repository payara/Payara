/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021-2025] Payara Foundation and/or its affiliates. All rights reserved.
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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import jakarta.ws.rs.core.Configuration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;

import static fish.payara.security.client.PayaraConstants.MP_CONFIG_CLIENT_CERTIFICATE_ALIAS;
import static fish.payara.security.client.PayaraConstants.REST_CLIENT_CERTIFICATE_ALIAS;
import static org.mockito.Mockito.*;

public class RestClientSslContextAliasListenerTest {

    @Mock
    private RestClientBuilder restClientBuilder;

    @Mock
    private Configuration configuration;

    @Mock
    private Config config;

    @InjectMocks
    @Spy
    private RestClientSslContextAliasListener restClientSslContextAliasListener =
            new RestClientSslContextAliasListener();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void restClientAliasPropertySslContextTest() throws Exception {
        KeyManager[] managers = getManagers();
        KeyStore[] keyStores = new KeyStore[]{getKeyStore()};

        when(restClientBuilder.getConfiguration()).thenReturn(configuration);
        when(configuration.getProperty(REST_CLIENT_CERTIFICATE_ALIAS)).thenReturn("myKey");
        doReturn(managers).when(restClientSslContextAliasListener).getKeyManagers();
        doReturn(keyStores).when(restClientSslContextAliasListener).getKeyStores();

        restClientSslContextAliasListener.onNewClient(RestClientBuilder.class, restClientBuilder);

        verify(restClientSslContextAliasListener, times(1)).buildSSlContext("myKey");
        verify(restClientSslContextAliasListener, times(1)).getKeyManagers();
        verify(restClientSslContextAliasListener, times(1)).getKeyStores();
        verify(restClientBuilder, times(1)).sslContext(any(SSLContext.class));
    }

    @Test
    public void restClientAliasPropertyFromMPConfigSslContextTest() throws Exception {
        KeyManager[] managers = getManagers();
        KeyStore[] keyStores = new KeyStore[]{getKeyStore()};

        when(restClientBuilder.getConfiguration()).thenReturn(configuration);
        when(configuration.getProperty(REST_CLIENT_CERTIFICATE_ALIAS)).thenReturn(null);
        doReturn(managers).when(restClientSslContextAliasListener).getKeyManagers();
        doReturn(keyStores).when(restClientSslContextAliasListener).getKeyStores();
        doReturn(config).when(restClientSslContextAliasListener).getConfig();
        when(config.getValue(MP_CONFIG_CLIENT_CERTIFICATE_ALIAS, String.class)).thenReturn("myKey");

        restClientSslContextAliasListener.onNewClient(RestClientBuilder.class, restClientBuilder);

        verify(restClientSslContextAliasListener, times(1)).buildSSlContext("myKey");
        verify(restClientSslContextAliasListener, times(1)).getKeyManagers();
        verify(restClientSslContextAliasListener, times(1)).getKeyStores();
        verify(restClientBuilder, times(1)).sslContext(any(SSLContext.class));
    }

    public KeyStore getKeyStore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, URISyntaxException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        URL resource = getClass().getClassLoader().getResource("keystore.p12");
        FileInputStream keyStoreFile = new FileInputStream(new File(resource.toURI()));
        keyStore.load(keyStoreFile, "changeit".toCharArray());
        return keyStore;
    }

    public KeyManager[] getManagers() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, URISyntaxException {
        KeyStore keyStore = getKeyStore();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "changeit".toCharArray());
        return kmf.getKeyManagers();
    }

}