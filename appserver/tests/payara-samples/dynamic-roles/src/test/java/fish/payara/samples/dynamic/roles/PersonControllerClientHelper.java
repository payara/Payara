/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.dynamic.roles;

import static fish.payara.samples.dynamic.roles.common.AuthoritiesConstants.DEFAULT_PASSWORD;
import static fish.payara.samples.dynamic.roles.common.AuthoritiesConstants.DEFAULT_USER;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

/**
 *
 * @author Gaurav Gupta
 */
public class PersonControllerClientHelper {

    public static PersonControllerClient getPersonControllerClient(URL deploymentUrl, String username, String password) {
        try {
            RestClientBuilder builder = RestClientBuilder.newBuilder();
            builder.register((ClientRequestFilter) context -> {
                context.getHeaders().add("username", DEFAULT_USER);
                context.getHeaders().add("password", DEFAULT_PASSWORD);
            });
            PersonControllerClient client = builder
                    .baseUrl(new URL(deploymentUrl.toURI().toString() + "resources/"))
                    .build(PersonControllerClient.class);
            return client;
        } catch (URISyntaxException | MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
