/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.requesttracing.jaxrs.client;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.internal.util.collection.UnsafeValue;

/**
 * Decorator for the default JerseyClientBuilder class to allow us to add our ClientFilter and instrument asynchronous
 * clients.
 *
 * @author David Matejcek
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class TracingJerseyClientBuilder extends JerseyClientBuilder {

    private static final Logger LOG = Logger.getLogger(TracingJerseyClientBuilder.class.getName());

    public static final String EARLY_BUILDER_INIT = //
        "fish.payara.requesttracing.jaxrs.client.decorators.EarlyBuilderInit";

    /**
     * Initialises a new JerseyClientBuilder and sets it as the decorated object.
     */
    public TracingJerseyClientBuilder() {
        super();
        LOG.finest(() -> "Created " + this);
    }

    @Override
    public JerseyClient createClient(final UnsafeValue<SSLContext, IllegalStateException> sslContextProvider,
        final HostnameVerifier verifier) {
        final boolean tracingActive = isRequestTracingActive();
        LOG.finest(() -> "tracingActive: " + tracingActive);
        if (!tracingActive) {
            return super.createClient(sslContextProvider, verifier);
        }

        register(JaxrsClientRequestTracingFilter.class);
        final JerseyClient client = new TracingJerseyClient(getConfiguration(), sslContextProvider, verifier);
        final Object earlyInit = getConfiguration().getProperty(EARLY_BUILDER_INIT);
        if (earlyInit instanceof Boolean && (Boolean) earlyInit) {
            client.preInitialize();
        }
        return client;
    }

    private boolean isRequestTracingActive() {
        try {
            final ServiceLocator locator = Globals.getDefaultBaseServiceLocator();
            if (locator == null) {
                return false;
            }
            final ServiceHandle<RequestTracingService> requestTracing = locator
                .getServiceHandle(RequestTracingService.class);
            if (requestTracing == null || !requestTracing.isActive()) {
                return false;
            }
            final ServiceHandle<OpenTracingService> openTracing = locator.getServiceHandle(OpenTracingService.class);
            return openTracing != null && openTracing.isActive();
        } catch (final RuntimeException e) {
            // means that we likely cannot do request tracing anyway
            LOG.log(Level.CONFIG, "Could not check the status of OpenTracing service!", e);
            return false;
        }
    }
}
