/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.monitoring.rest.app.resource;

import fish.payara.monitoring.rest.app.MBeanServerDelegate;
import fish.payara.monitoring.rest.app.handler.MBeanReadHandler;
import fish.payara.monitoring.rest.app.handler.MBeanAttributeReadHandler;
import static fish.payara.monitoring.rest.app.resource.PathProcessor.getSplitPath;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 *
 * @author Fraser Savage
 */
@Path("read")
@RequestScoped
public class MBeanReadResource {
    
    @Inject
    private MBeanServerDelegate mDelegate;
   
    /**
     * Returns the {@link String} form of the {@link JSONObject} resource 
     * from the ResourceHandler.
     * 
     * @param path The {@link String} representation of the URL path the resource has received.
     * @return The {@link String} representation of the MBeanRead/MBeanAttributeRead {@link JSONObject}.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{path : .+}")
    public String getReadResource(@PathParam("path") String path) {
        String[] segments = getSplitPath(path);
        switch (segments.length) {
            case 2:
                return getMBeanAttributeRead(segments[0], segments[1]);
            case 1:
                return getMBeanRead(segments[0]);
            default:
                return "invalid url";
                // @TODO - FANG-1: What path functionality is there to implement?
        }
    }

    private String getMBeanRead(String mbeanname) {
        return new MBeanReadHandler(mDelegate, mbeanname).getResource()
                .toString();
    }

    private String getMBeanAttributeRead(String mbeanname, String attributename) {
        return new MBeanAttributeReadHandler(mDelegate, mbeanname, 
                attributename).getResource().toString();
    }
}
