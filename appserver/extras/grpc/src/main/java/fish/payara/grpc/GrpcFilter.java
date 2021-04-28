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
package fish.payara.grpc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.grpc.BindableService;
import io.grpc.servlet.ServletAdapter;
import io.grpc.servlet.ServletAdapterBuilder;

public class GrpcFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    private final CreationalContext<BindableService> cdiContext;

    private final Map<Bean<BindableService>, BindableService> services;

    private ServletAdapter adapter;

    protected GrpcFilter(CreationalContext<BindableService> cdiContext, Bean<BindableService>... serviceBeans) {
        services = new HashMap<>();
        for (Bean<BindableService> serviceBean : serviceBeans) {
            services.put(serviceBean, null);
        }
        this.cdiContext = cdiContext;
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        final String method = req.getMethod();

        // Ignore non-gRPC requests
        if (method == null || !ServletAdapter.isGrpc(req)) {
            chain.doFilter(req, res);
            return;
        }

        switch (method.toUpperCase()) {
        case "GET":
            adapter.doGet(req, res);
            break;
        case "POST":
            adapter.doPost(req, res);
            break;
        default:
            throw new UnsupportedOperationException("Unsupported HTTP method: " + method);
        }
    }

    @Override
    public void init() throws ServletException {

        // For each service
        final Iterator<Entry<Bean<BindableService>, BindableService>> iterator = services.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<Bean<BindableService>, BindableService> entry = iterator.next();
            if (entry.getValue() == null) {

                // Create the service
                final BindableService service = entry.getKey().create(cdiContext);

                entry.setValue(service);
            }
        }

        if (adapter == null) {
            ServletAdapterBuilder builder = new ServletAdapterBuilder();

            // Register each service
            for (BindableService service : services.values()) {
                builder = builder.addService(service);
            }

            this.adapter = builder.buildServletAdapter();
        }

        super.init();
    }

    @Override
    public void destroy() {

        // For each service
        final Iterator<Entry<Bean<BindableService>, BindableService>> iterator = services.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<Bean<BindableService>, BindableService> entry = iterator.next();
            if (entry.getValue() != null) {

                // Destroy the service
                entry.getKey().destroy(entry.getValue(), cdiContext);

                iterator.remove();
            }
        }

        if (adapter != null) {
            adapter.destroy();
        }

        super.destroy();
    }

}
