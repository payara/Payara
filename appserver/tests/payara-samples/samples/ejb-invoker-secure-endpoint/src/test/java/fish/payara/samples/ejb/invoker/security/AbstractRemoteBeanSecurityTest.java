/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */
package fish.payara.samples.ejb.invoker.security;

import fish.payara.ejb.http.client.RemoteEJBContextFactory;
import static fish.payara.ejb.http.client.RemoteEJBContextFactory.JAXRS_CLIENT_KEY_STORE;
import static fish.payara.ejb.http.client.RemoteEJBContextFactory.JAXRS_CLIENT_KEY_STORE_PASSOWRD;
import static fish.payara.ejb.http.client.RemoteEJBContextFactory.JAXRS_CLIENT_PROTOCOL_VERSION;
import static fish.payara.ejb.http.client.RemoteEJBContextFactory.JAXRS_CLIENT_SERIALIZATION;
import static fish.payara.ejb.http.client.RemoteEJBContextFactory.JAXRS_CLIENT_TRUST_STORE;
import static fish.payara.ejb.http.client.RemoteEJBContextFactory.PROVIDER_AUTH_TYPE;
import static fish.payara.ejb.http.client.RemoteEJBContextFactory.PROVIDER_CREDENTIALS;
import static fish.payara.ejb.http.client.RemoteEJBContextFactory.PROVIDER_PRINCIPAL;
import fish.payara.ejb.http.protocol.SerializationType;
import fish.payara.samples.ServerOperations;
import static fish.payara.samples.ServerOperations.getClientTrustStoreURL;
import static fish.payara.samples.ServerOperations.getKeyStore;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import javax.naming.Context;
import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static javax.naming.Context.PROVIDER_URL;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import static javax.ws.rs.core.SecurityContext.BASIC_AUTH;
import static javax.ws.rs.core.SecurityContext.CLIENT_CERT_AUTH;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import static org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author jGauravGupta
 */
public abstract class AbstractRemoteBeanSecurityTest {

    private static final String EJB_INVOKER_PROVIDER_URL = "https://localhost:8181/ejb-invoker";

    @ArquillianResource
    private URL base;

    private static String clientKeyStorePath;

    @Deployment
    public static Archive<?> deployment() {
        clientKeyStorePath = ServerOperations.createClientKeyStore();
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(Bean.class, RemoteBean.class)
                .addAsManifestResource(INSTANCE, "beans.xml");
    }

    private Context getContextWithCredentialsSet(String username, String password) {
        Hashtable<String, Object> environment = new Hashtable<>();
        environment.put(INITIAL_CONTEXT_FACTORY, RemoteEJBContextFactory.class.getName());
        environment.put(PROVIDER_URL, EJB_INVOKER_PROVIDER_URL);
        environment.put(PROVIDER_AUTH_TYPE, BASIC_AUTH);
        environment.put(PROVIDER_PRINCIPAL, username);
        environment.put(PROVIDER_CREDENTIALS, password);
        environment.put(JAXRS_CLIENT_SERIALIZATION, SerializationType.JSON.toString());
        environment.put(JAXRS_CLIENT_PROTOCOL_VERSION, String.valueOf(1));
        createClientCertificateStore(environment);
        try {
            return new InitialContext(environment);
        } catch (NamingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void createClientCertificateStore(Hashtable<String, Object> environment) {
        try {
            URL baseHttps = ServerOperations.toContainerHttps(base);
            if (baseHttps == null) {
                throw new IllegalStateException("No https URL could be created from " + base);
            }

            String type = "jks";
            String password = "changeit";
            environment.put(JAXRS_CLIENT_TRUST_STORE,
                    getKeyStore(getClientTrustStoreURL(baseHttps, clientKeyStorePath), password, type));
            environment.put(JAXRS_CLIENT_KEY_STORE,
                    getKeyStore(new File(clientKeyStorePath).toURI().toURL(), password, type));
            environment.put(JAXRS_CLIENT_KEY_STORE_PASSOWRD,
                    password);

        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    @RunAsClient
    public void callRemoteBeanWithCorrectCredentials() throws NamingException {
        // Obtain the JNDI naming context
        Context ejbRemoteContext = getContextWithCredentialsSet(getUserName(), getPassword());

        RemoteBean beanRemote = (RemoteBean) ejbRemoteContext.lookup("java:global/test/Bean");
        assertNotNull(beanRemote.method());
    }

    @Test
    @RunAsClient
    public void callRemoteBeanWithIncorrectCredentials() throws NamingException {
        // Obtain the JNDI naming context
        Context ejbRemoteContext = getContextWithCredentialsSet(getUserName(), "InvalidPassword");

        try {
            RemoteBean beanRemote = (RemoteBean) ejbRemoteContext.lookup("java:global/test/Bean");
            assertNotNull(beanRemote.method());
            fail("RemoteBean#method must not be accessed for invalid credential");
        } catch (NamingException ex) {
            assertEquals("Invoker is not available at <" + EJB_INVOKER_PROVIDER_URL + ">: Unauthorized", ex.getExplanation());
        }
    }

    protected abstract String getUserName();

    protected abstract String getPassword();

}
