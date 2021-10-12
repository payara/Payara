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

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Configuration;
import java.util.logging.Logger;

import static fish.payara.microprofile.jaxrs.client.ssl.PayaraConstants.PAYARA_KEYSTORE_PASSWORD_PROPERTY_NAME;
import static fish.payara.microprofile.jaxrs.client.ssl.PayaraConstants.PAYARA_REST_CLIENT_CERTIFICATE_ALIAS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RestClientSslContextAliasListenerTest {

    private static final Logger logger = Logger.getLogger(RestClientSslContextAliasListenerTest.class.getName());

    @Mock
    private RestClientBuilder restClientBuilder;

    @Mock
    private Configuration configuration;

    @InjectMocks
    @Spy
    private RestClientSslContextAliasListener restClientSslContextAliasListener = new RestClientSslContextAliasListener();

    @Test
    public void restClientBuilderListenerSetAliasPropertyTest() {
        System.setProperty(PAYARA_KEYSTORE_PASSWORD_PROPERTY_NAME, "changeit");

        when(restClientBuilder.getConfiguration()).thenReturn(configuration);
        when(configuration.getProperty(PAYARA_REST_CLIENT_CERTIFICATE_ALIAS)).thenReturn("myKey");

        restClientSslContextAliasListener.onNewClient(RestClientBuilder.class, restClientBuilder);

        verify(restClientSslContextAliasListener, times(1))
                .onNewClient(RestClientBuilder.class, restClientBuilder);
        verify(restClientBuilder, times(1)).sslContext(any(SSLContext.class));
        verify(restClientSslContextAliasListener, times(1))
                .buildSSlContext(anyString());
    }

    @Test
    public void restClientBuilderListenerWithoutAliasPropertyFromRestClientCallTest() {
        when(restClientBuilder.getConfiguration()).thenReturn(configuration);
        when(configuration.getProperty(PAYARA_REST_CLIENT_CERTIFICATE_ALIAS)).thenReturn(null);

        try {
            restClientSslContextAliasListener.onNewClient(RestClientBuilder.class, restClientBuilder);
        } catch (IllegalStateException e) {
            logger.info("MicroProfile Config can't be achieved");
        }

        verify(restClientSslContextAliasListener, times(1))
                .onNewClient(RestClientBuilder.class, restClientBuilder);

    }

}