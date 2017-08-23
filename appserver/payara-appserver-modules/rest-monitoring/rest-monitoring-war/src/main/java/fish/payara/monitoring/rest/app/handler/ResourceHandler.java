/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.monitoring.rest.app.RestMonitoringAppResponseToken;
import fish.payara.monitoring.rest.app.MBeanServerDelegate;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author Fraser Savage
 */
public abstract class ResourceHandler {

    protected final MBeanServerDelegate delegate;
    private Response.Status status;

    /**
     * Constructs the {@link ResourceHandler}.
     * 
     * @param delegate The {@link MBeanServerDelegate} to get information from.
     */
    public ResourceHandler(MBeanServerDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns a {@link JSONObject} containing the response to the request given
     * to the implementing class. 
     * The response is made up of a request object and a value object. 
     * In some cases the value object is removed in the case of an exception and
     * replaced with an error object.
     * 
     * @return The {@link JSONObject} containing the response to the request.
     */
    public JSONObject getResource() {
        JSONObject resourceResponse = new JSONObject();
        try {
            JSONObject requestObject = getRequestObject();
            resourceResponse.put(RestMonitoringAppResponseToken.getRequestKey(),
                    requestObject);

            Object valueObject = getValueObject();
            resourceResponse.put(RestMonitoringAppResponseToken.getValueKey(), 
                    valueObject);
            
            setStatus(Response.Status.OK);
            
            if (errorThrown(status)) {
                JSONObject traceObject = (JSONObject) resourceResponse
                        .get(RestMonitoringAppResponseToken.getValueKey());
               
                resourceResponse.put(RestMonitoringAppResponseToken.getStacktraceKey(),
                        traceObject.get(RestMonitoringAppResponseToken.getStacktraceKey()));
                resourceResponse.put(RestMonitoringAppResponseToken.getErrorTypeKey(),
                        traceObject.get(RestMonitoringAppResponseToken.getErrorTypeKey()));
                resourceResponse.put(RestMonitoringAppResponseToken.getErrorKey(),
                        traceObject.get(RestMonitoringAppResponseToken.getErrorKey()));
               
                resourceResponse.remove(RestMonitoringAppResponseToken.getValueKey());
            } else {
                Long millis = System.currentTimeMillis();
                resourceResponse.put(RestMonitoringAppResponseToken.getTimestampKey(),
                        millis);
            }

            int statusCode = status.getStatusCode();
            resourceResponse.put(RestMonitoringAppResponseToken.getHttpStatusKey(),
                    statusCode);
        } catch (JSONException ex) {
            // @TODO - FANG-6: Properly handle any JSONException caught in the ResourceHandler class.
            // Is this the best way to handle it? Return the response built so far and log the issue.
            // Don't exactly want to return a JSON error in the response.
            //
            // Options:
            //  1. Place each put in it's own try-catch block so that what can be 
            // added to the object is added to the object before returning it - ie.
            // just carry on.
            //  2. Leave it as it currently is.
            //  3. Don't return the response object but instead a string denoting
            // internal error or something?
            Logger.getLogger(ResourceHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return resourceResponse;
    }

    /**
     * Returns a {@link JSONObject} that contains values relating to the request. 
     * 
     * @return A {@link JSONObject} containing the value(s) relating to the request..
     * @throws JSONException {@inheritDoc}
     */
    abstract JSONObject getRequestObject() throws JSONException;

    /**
     * Returns an {@link Object} that contains values relating to the target of 
     * the request. 
     * Typically this is a primitive type or a JSONObject.
     * 
     * @return An {@link Object} containing the value(s) relating to the request target.
     * @throws JSONException {@inheritDoc}
     */
    abstract Object getValueObject() throws JSONException; 
  
    // If the response status isn't set then will set it
    protected void setStatus(Response.Status status) {
        if (this.status == null) {
            this.status = status;
        }
    }

    // Gets the JSONObject containing information about the exception passed 
    protected JSONObject getTraceObject(Exception exception) throws JSONException {
        JSONObject traceObject = new JSONObject();

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);

        traceObject.put(RestMonitoringAppResponseToken.getStacktraceKey(), 
                stringWriter.toString());

        traceObject.put(RestMonitoringAppResponseToken.getErrorTypeKey(), 
                exception.getClass().getCanonicalName());

        traceObject.put(RestMonitoringAppResponseToken.getErrorKey(), 
                exception.getClass().getCanonicalName() + " : " 
                        + exception.getMessage());
        
        return traceObject;
    }

    // Returns true if one of the checked response codes is passed to it
    private boolean errorThrown(Response.Status status) {
        switch (status) {
            case NOT_FOUND:
                return true;                
            case INTERNAL_SERVER_ERROR:
                return true;
            default:
                return false;
        }
    }
}
