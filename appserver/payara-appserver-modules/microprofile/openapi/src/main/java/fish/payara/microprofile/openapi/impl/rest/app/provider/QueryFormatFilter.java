/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.impl.rest.app.provider;

import static fish.payara.microprofile.openapi.rest.app.OpenApiApplication.APPLICATION_YAML;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import jakarta.ws.rs.ext.Provider;

/**
 * A filter that attempts to change the <code>Accept</code> header if the
 * <code>format</code> query parameter is provided.
 */
@Provider
@PreMatching
public class QueryFormatFilter implements ContainerRequestFilter {

    /**
     * A map of recognised media types that can be specified in a
     * <code>format</code> query parameter.
     */
    private static final Map<String, String> mappings;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("yaml", APPLICATION_YAML);
        map.put("json", APPLICATION_JSON);
        mappings = Collections.unmodifiableMap(map);
    }

    /**
     * Filters incoming requests to change the <code>Accept</code> header based on
     * the <code>format</code> query parameter.
     */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String format = request.getUriInfo().getQueryParameters().getFirst("format");
        if (format != null) {
            format = format.toLowerCase();
            if (mappings.containsKey(format)) {
                request.getHeaders().putSingle(ACCEPT, mappings.get(format));
            }
        }
    }

}