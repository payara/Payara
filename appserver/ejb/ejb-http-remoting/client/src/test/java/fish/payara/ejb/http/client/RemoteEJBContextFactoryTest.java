/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.http.client;

import static fish.payara.ejb.http.client.RemoteEJBContextFactory.JAXRS_CLIENT_CONNECT_TIMEOUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.After;
import org.junit.Test;

/**
 * Tests the {@link System} property fallback for the {@link RemoteEJBContextFactory}.
 * 
 * @author Jan Bernitt
 * @since 5.192
 */
public class RemoteEJBContextFactoryTest {

    private static final String FISH_PAYARA_EJB_HTTP_CLIENT_PROVIDER_URL = Context.PROVIDER_URL;
    private static final String FISH_PAYARA_EJB_HTTP_CLIENT_SSLCONTEXT = RemoteEJBContextFactory.JAXRS_CLIENT_SSL_CONTEXT;

    @After
    public void clean() {
        System.clearProperty(FISH_PAYARA_EJB_HTTP_CLIENT_PROVIDER_URL);
        System.clearProperty(FISH_PAYARA_EJB_HTTP_CLIENT_SSLCONTEXT);
        System.clearProperty(JAXRS_CLIENT_CONNECT_TIMEOUT);
    }

    @Test
    public void whenPayaraSpecificEnvironmentVariableIsSetThePrefixIsEnlarged() throws NamingException {
        System.setProperty(FISH_PAYARA_EJB_HTTP_CLIENT_PROVIDER_URL, "http://custom.url");
        Context context = new RemoteEJBContextFactory().getInitialContext(new Hashtable<>());
        assertEquals("http://custom.url", context.getEnvironment().get(Context.PROVIDER_URL));
    }

    @Test
    public void whenNamingEnvironmentVariableIsSetThePrefixIsReplaced() throws NamingException {
        System.setProperty(FISH_PAYARA_EJB_HTTP_CLIENT_SSLCONTEXT, "custom-SSL-context");
        Context context = new RemoteEJBContextFactory().getInitialContext(new Hashtable<>());
        assertEquals("custom-SSL-context", context.getEnvironment().get(RemoteEJBContextFactory.JAXRS_CLIENT_SSL_CONTEXT));
    }

    @Test
    public void whenEnvironmentVariableIsAlreadySetItIsNotReplacedWithSystemProperty() throws NamingException {
        System.setProperty(FISH_PAYARA_EJB_HTTP_CLIENT_PROVIDER_URL, "http://custom.url");
        System.setProperty(FISH_PAYARA_EJB_HTTP_CLIENT_SSLCONTEXT, "custom-SSL-context");
        Hashtable<String, Object> environment = new Hashtable<>();
        environment.put(Context.PROVIDER_URL, "http://existing.url");
        environment.put(RemoteEJBContextFactory.JAXRS_CLIENT_SSL_CONTEXT, "existing-SSL-context");
        Context context = new RemoteEJBContextFactory().getInitialContext(environment);
        assertEquals("http://existing.url", context.getEnvironment().get(Context.PROVIDER_URL));
        assertEquals("existing-SSL-context", context.getEnvironment().get(RemoteEJBContextFactory.JAXRS_CLIENT_SSL_CONTEXT));
    }

    @Test
    public void systemPropertiesWorkWithJNDI() throws NamingException {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, RemoteEJBContextFactory.FACTORY_CLASS);
        System.setProperty(Context.PROVIDER_URL, "http://localhost:8080");
        System.setProperty(JAXRS_CLIENT_CONNECT_TIMEOUT, "90");
        ExposingInitialContext context = new ExposingInitialContext();
        assertTrue("RemoteEJBContext was created", context.getDefaultInitCtx() instanceof RemoteEJBContext);
        assertEquals("90", context.getEnvironment().get(JAXRS_CLIENT_CONNECT_TIMEOUT));
    }

    static class ExposingInitialContext extends InitialContext {

        public ExposingInitialContext() throws NamingException {
        }

        @Override
        protected Context getDefaultInitCtx() throws NamingException {
            return super.getDefaultInitCtx();
        }
    }
}
