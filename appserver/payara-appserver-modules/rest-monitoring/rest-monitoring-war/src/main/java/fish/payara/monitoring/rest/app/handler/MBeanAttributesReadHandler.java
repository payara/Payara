/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.monitoring.rest.app.handler;

import javax.inject.Singleton;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import fish.payara.monitoring.rest.app.MBeanServerDelegate;
import fish.payara.monitoring.rest.app.RestMonitoringAppResponseToken;
import fish.payara.monitoring.rest.app.processor.ProcessorFactory;
import fish.payara.monitoring.rest.app.processor.TypeProcessor;
import java.util.Arrays;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;

/**
 * @author Krassimir Valev
 */
public class MBeanAttributesReadHandler extends ReadHandler {

    private final String mbeanname;
    private final String[] attributenames;

    /**
     * Creates an instance of MBeanAttributeReadHandler, which handles bulk MBean
     * attribute read requests.
     *
     * @param delegate
     *            The {@link MBeanServerDelegate} to get information from.
     * @param mbeanname
     *            The {@link ObjectName} of the MBean to get information from.
     * @param attributename
     *            The name of the MBean attribute to get values for.
     */
    public MBeanAttributesReadHandler(@Singleton final MBeanServerDelegate delegate,
            final String mbeanname, final String[] attributenames) {
        super(delegate);
        this.mbeanname = mbeanname;
        this.attributenames = attributenames;
    }

    @Override
    public JSONObject getRequestObject() {
        JSONObject requestObject = new JSONObject();
        try {
            requestObject.put(RestMonitoringAppResponseToken.getMbeanNameKey(), mbeanname);
            requestObject.put(RestMonitoringAppResponseToken.getMbeanNameKey(), mbeanname);
            requestObject.put(RestMonitoringAppResponseToken.getAttributeNameKey(),
                    new JSONArray(Arrays.asList(attributenames)));
            requestObject.put(RestMonitoringAppResponseToken.getRequestTypeKey(), requesttype);

        } catch (JSONException ex) {
            Logger.getLogger(MBeanAttributesReadHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return requestObject;
    }

    @Override
    public Object getValueObject() throws JSONException {
        try {
            AttributeList attributes = delegate.getMBeanAttributes(mbeanname, attributenames);

            // the javax.management.Attribute type does not inherit from OpenType<T>, so the existing
            // ProcessorFactory and TypeProcessor infrastructure cannot be used
            JSONArray jsonArray = new JSONArray();

            for (Attribute attribute : attributes.asList()) {
                TypeProcessor<?> processor = ProcessorFactory.getTypeProcessor(attribute.getValue());

                JSONObject valueObject = new JSONObject();
                valueObject.put(attribute.getName(), processor.processObject(attribute.getValue()));
                jsonArray.put(valueObject);
            }

            return jsonArray;
        } catch (InstanceNotFoundException | ReflectionException | MalformedObjectNameException ex) {
            super.setStatus(Response.Status.NOT_FOUND);
            return getTraceObject(ex);
        }
    }
}
