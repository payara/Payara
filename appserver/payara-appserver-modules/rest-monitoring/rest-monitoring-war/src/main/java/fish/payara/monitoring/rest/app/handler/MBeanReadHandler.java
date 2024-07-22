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
package fish.payara.monitoring.rest.app.handler;

import fish.payara.monitoring.rest.app.RestMonitoringAppResponseToken;
import fish.payara.monitoring.rest.app.MBeanServerDelegate;
import java.math.BigDecimal;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import jakarta.ws.rs.core.Response;

/**
 * @since 4.1.2.173
 * @author Fraser Savage
 */
public class MBeanReadHandler extends ReadHandler {
  
    private final String mbeanname;

    /**
     * Creates an instance of MBeanReadHandler, which handles MBean
     * read requests.
     * 
     * @param delegate The {@link MBeanServerDelegate} to get information from.
     * @param mbeanname The {@link ObjectName} of the MBean to get information from.
     */
    public MBeanReadHandler(@Singleton MBeanServerDelegate delegate,
            String mbeanname) {
        super(delegate);
        this.mbeanname = mbeanname;
    }

    @Override
    public JsonObject getRequestObject() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        try {
            
            objectBuilder.add(RestMonitoringAppResponseToken.getMbeanNameKey(), mbeanname);
            objectBuilder.add(RestMonitoringAppResponseToken.getRequestTypeKey(), 
                    requesttype);
        } catch (JsonException ex) {
            super.setStatus(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return objectBuilder.build();
    }

    @Override
    public JsonValue getValueObject() throws JsonException {
        try {
            MBeanInfo mbeanInfo = delegate.getMBean(mbeanname);
            return buildAttributes(mbeanInfo);
        } catch (InstanceNotFoundException | IntrospectionException | ReflectionException | MalformedObjectNameException ex) {
            super.setStatus(Response.Status.NOT_FOUND);
            return getTraceObject(ex);
        }
    }

    private JsonObject buildAttributes(MBeanInfo mbean) throws JsonException {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        MBeanAttributeInfo[] attributes = mbean.getAttributes();

        for (MBeanAttributeInfo attribute : attributes) {
            String attributeName = attribute.getName();
            MBeanAttributeReadHandler attributeHandler = 
                    new MBeanAttributeReadHandler(delegate, mbeanname, 
                            attributeName);
            objectBuilder.add(attributeName, 
                    attributeHandler.getValueObject());
        }

        return objectBuilder.build();
    }
}
