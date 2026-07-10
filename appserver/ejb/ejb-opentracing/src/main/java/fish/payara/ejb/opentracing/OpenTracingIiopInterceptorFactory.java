/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.opentracing;

import fish.payara.opentracing.OpenTracingService;
import org.glassfish.enterprise.iiop.api.IIOPInterceptorFactory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import jakarta.inject.Singleton;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creating IIOP client and server interceptors that propagate OpenTracing SpanContext.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Service(name = "OpenTracingIiopInterceptorFactory")
@Singleton
public class OpenTracingIiopInterceptorFactory implements IIOPInterceptorFactory {

    private static final Logger logger = Logger.getLogger(OpenTracingIiopInterceptorFactory.class.getName());

    public static final int OPENTRACING_IIOP_ID = 3226428;
    public static final long OPENTRACING_IIOP_SERIAL_VERSION_UID = 20200731171822L;


    private ClientRequestInterceptor clientRequestInterceptor;
    private ServerRequestInterceptor serverRequestInterceptor;

    private OpenTracingService openTracingService;
    private ServiceLocator serviceLocator;

    @Override
    public ClientRequestInterceptor createClientRequestInterceptor(ORBInitInfo info, Codec codec) {
        if (clientRequestInterceptor == null) {
            if (attemptCreation()) {
                try {
                    clientRequestInterceptor = new OpenTracingIiopClientInterceptor(openTracingService);
                } catch (NullPointerException nullPointerException) {
                    logger.log(Level.WARNING, "Could not create OpenTracing IIOP Client Interceptor - Remote EJBs will not be traced");
                    return null;
                }
            }
        }

        return clientRequestInterceptor;
    }

    @Override
    public ServerRequestInterceptor createServerRequestInterceptor(ORBInitInfo info, Codec codec) {
        if (serverRequestInterceptor == null) {
            if (attemptCreation()) {
                serverRequestInterceptor = new OpenTracingIiopServerInterceptor(openTracingService);
            }
        }

        return serverRequestInterceptor;
    }

    private boolean attemptCreation() {
        if (serviceLocator == null) {
            serviceLocator = Globals.getStaticBaseServiceLocator();
            if (serviceLocator == null) {
                return false;
            }
        }

        if (openTracingService == null) {
            openTracingService = serviceLocator.getService(OpenTracingService.class);
            if (openTracingService == null) {
                return false;
            }
        }

        return true;
    }

}