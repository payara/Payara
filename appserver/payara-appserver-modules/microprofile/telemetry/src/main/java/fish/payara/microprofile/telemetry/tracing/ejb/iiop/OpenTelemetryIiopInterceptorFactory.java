/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.telemetry.tracing.ejb.iiop;

import jakarta.inject.Singleton;
import org.glassfish.enterprise.iiop.api.IIOPInterceptorFactory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

/**
 * Factory for creating IIOP client and server interceptors that propagate OTel
 * context and create CLIENT/SERVER spans for remote EJB calls.
 *
 * <p>Note: these interceptors are registered in the <em>server</em> ORB only.
 * A plain IIOP naming client (e.g. a {@code @RunAsClient} test JVM) does not
 * have these interceptors registered, so no CLIENT span is created on the
 * client side for externally-initiated calls.</p>
 */
@Service(name = "OpenTelemetryIiopInterceptorFactory")
@Singleton
public class OpenTelemetryIiopInterceptorFactory implements IIOPInterceptorFactory {

    public static final int OPENTELEMETRY_IIOP_ID = 3226428;
    /** Serial version used by {@link OpenTelemetryIiopTextMap} for wire compatibility. */
    public static final long OPENTRACING_IIOP_SERIAL_VERSION_UID = 20200731171822L;

    private ClientRequestInterceptor clientRequestInterceptor;
    private ServerRequestInterceptor serverRequestInterceptor;

    private ServiceLocator serviceLocator;

    @Override
    public synchronized ClientRequestInterceptor createClientRequestInterceptor(ORBInitInfo info, Codec codec) {
        if (clientRequestInterceptor == null && attemptCreation()) {
            clientRequestInterceptor = new OpenTelemetryIiopClientInterceptor(serviceLocator);
        }
        return clientRequestInterceptor;
    }

    @Override
    public synchronized ServerRequestInterceptor createServerRequestInterceptor(ORBInitInfo info, Codec codec) {
        if (serverRequestInterceptor == null && attemptCreation()) {
            serverRequestInterceptor = new OpenTelemetryIiopServerInterceptor(serviceLocator);
        }
        return serverRequestInterceptor;
    }

    private boolean attemptCreation() {
        if (serviceLocator == null) {
            serviceLocator = Globals.getStaticBaseServiceLocator();
        }
        return serviceLocator != null;
    }
}
