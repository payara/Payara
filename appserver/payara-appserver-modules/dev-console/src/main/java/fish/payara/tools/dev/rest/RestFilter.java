/*
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.tools.dev.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.Context;
import java.io.IOException;
import java.io.OutputStream;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
public class RestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_TIME = "start-time";
    private static final String REQUEST_SIZE = "request-size";

    @Context
    private ResourceInfo resourceInfo;

    @Inject
    private RestMetricsRegistry registry;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty(START_TIME, System.currentTimeMillis());

        // Calculate request size if entity present
        int reqSize = 0;
        if (requestContext.hasEntity()) {
            // Warning: consumes entity stream if you read fully
            // Simple approximation:
            requestContext.getEntityStream().mark(Integer.MAX_VALUE);
            reqSize = requestContext.getEntityStream().available(); // works for some streams
            requestContext.getEntityStream().reset();
        }
        requestContext.setProperty(REQUEST_SIZE, reqSize);
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) throws IOException {

        Object start = requestContext.getProperty(START_TIME);
        if (start == null) {
            return;
        }

        long duration = System.currentTimeMillis() - (long) start;

        int reqSize = requestContext.getProperty(REQUEST_SIZE) != null
                ? (int) requestContext.getProperty(REQUEST_SIZE)
                : 0;

        int respSize = 0;

        if (responseContext.hasEntity()) {
            OutputStream original = responseContext.getEntityStream();
            CountingOutputStream cos = new CountingOutputStream(original);
            responseContext.setEntityStream(cos);
            responseContext.setEntityStream(cos); // wrap entity stream

            respSize = cos.getCount();
        }

        String classMethod = resourceInfo.getResourceClass().getName() + "#"
                + resourceInfo.getResourceMethod().getName();

        int status = responseContext.getStatus();
        registry.addRecord(classMethod, duration, status, reqSize, respSize);
    }

    private static class CountingOutputStream extends OutputStream {

        private final OutputStream out;
        private int count = 0;

        public CountingOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            count += len;
        }

        public int getCount() {
            return count;
        }
    }
}
