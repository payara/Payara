/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.rest.management.extension;

import static java.lang.String.format;
import static java.util.logging.Level.WARNING;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Logger;

import javax.json.JsonObject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;

import fish.payara.samples.ServerOperations;
import fish.payara.samples.rest.management.util.RestManagementClientBuilder;

/**
 * An Arquillian resource that will create an instance on the target server
 * using the REST management interface if it is reachable. A new instance will
 * be created for each test class.
 */
public class TemporaryInstance {

    private static final Logger LOGGER = Logger.getLogger(TemporaryInstance.class.getName());

    private boolean initialized;
    private boolean created;

    private String name;

    private WebTarget target;

    protected void initialise(URL arquillianURL) {
        initialized = true;

        URI adminBaseUri;
        try {
            adminBaseUri = ServerOperations.toAdminPort(arquillianURL).toURI();
        } catch (Exception ex) {
            LOGGER.log(WARNING, "Unable to find admin base URL. Should this profile have an admin console?", ex);
            return;
        }

        this.target = RestManagementClientBuilder.newClient(adminBaseUri);

        name = "test-instance-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Get the default node name
        String nodeName = target.path("nodes/node").request()
                .get(JsonObject.class)
                .getJsonObject("extraProperties")
                .getJsonObject("childResources")
                .keySet().iterator().next();

        Form form = new Form();
        form.param("node", nodeName);
        form.param("name", name);

        JsonObject response = target.path("create-instance")
                .request()
                .post(entity(form, APPLICATION_FORM_URLENCODED), JsonObject.class);
        
        if (!"SUCCESS".equals(response.getString("exit_code"))) {
            LOGGER.warning(format("Failed to create instance %s. Response from endpoint: %s.", name, response));
        } else {
            created = true;
        }
    }

    protected void destroy() {
        Form form = new Form();
        form.param("instance_name", name);

        JsonObject response = target.path(format("servers/server/%s/delete-instance", name))
                .request()
                .post(entity(form, APPLICATION_FORM_URLENCODED), JsonObject.class);

        if (!"SUCCESS".equals(response.getString("exit_code"))) {
            LOGGER.warning(format("Failed to delete instance %s. Response from endpoint: %s.", name, response));
        }
    }

    /**
     * @return if this object has had an initialisation attempt, successful or
     *         otherwise.
     */
    protected boolean isInitialized() {
        return initialized;
    }

    /**
     * @return true if the instance has been created on the server, or false otherwise.
     */
    public boolean isCreated() {
        return created;
    }

    /**
     * @return the name of the instance created, or null if it hasn't been created.
     */
    public String getName() {
        return name;
    }

}