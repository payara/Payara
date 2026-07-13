/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.remote.ejb.tracing.web;

import fish.payara.samples.remote.ejb.tracing.EjbRemote;
import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

/**
 * JAX-RS resource that exercises the IIOP path from within the server, allowing
 * {@code @RunAsClient} tests to trigger IIOP calls and observe the resulting spans
 * via the {@link SpanReportResource}.
 */
@Path("/iiop-tracing")
public class IiopTracingResource {

    private static final String IIOP_HOST = "localhost";
    private static final int IIOP_PORT = 3700;

    /**
     * Makes a normal IIOP call to {@code EjbRemote.nonAnnotatedMethod()} and returns
     * {@code "ok"}.  The resulting spans can be retrieved via
     * {@code GET /span-report}.
     */
    @GET
    @Path("/call")
    @Produces(MediaType.TEXT_PLAIN)
    public String call() throws NamingException {
            EjbRemote ejb = lookupEjb();
        ejb.nonAnnotatedMethod();
        return "ok";
    }

    /**
     * Makes an IIOP call that causes the EJB to throw a RuntimeException (wrapped
     * by the container as EJBException), then returns {@code "ok"} after the expected
     * exception is caught.
     */
    @GET
    @Path("/call-error")
    @Produces(MediaType.TEXT_PLAIN)
    public String callError() throws NamingException {
        EjbRemote ejb = lookupEjb();
        try {
            ejb.throwsException();
        } catch (EJBException expected) {
            // Expected — the container wraps RuntimeExceptions
        }
        return "ok";
    }

    private EjbRemote lookupEjb() throws NamingException {
        Properties props = new Properties();
        props.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.enterprise.naming.SerialInitContextFactory");
        props.setProperty("org.omg.CORBA.ORBInitialHost", IIOP_HOST);
        props.setProperty("org.omg.CORBA.ORBInitialPort", String.valueOf(IIOP_PORT));
        InitialContext ctx = new InitialContext(props);
        return (EjbRemote) ctx.lookup("java:global/remote-ejb-tracing/Ejb");
    }
}
