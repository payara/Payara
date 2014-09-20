/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.nucleus.admin.rest;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.admin.rest.client.utils.MarshallingUtils;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 * @author Mitesh Meswani
 */
public class NoCLICommandResourceCreationTest extends RestTestBase {
    private static final String URL_SERVER_PROPERTY = "/domain/servers/server/server/property";

    @Test
    public void testPropertyCreation() {
        final String propertyKey  = "propertyName" + generateRandomString();
        String propertyValue = generateRandomString();

        //Create a property
        Map<String, String> param = new HashMap<String, String>();
        param.put("name", propertyKey);
        param.put("value",propertyValue);
        Response response = getClient().target(getAddress(URL_SERVER_PROPERTY))
                .request(RESPONSE_TYPE)
                .post(Entity.entity(MarshallingUtils.getXmlForProperties(param), MediaType.APPLICATION_XML), Response.class);
        assertTrue(isSuccess(response));

        //Verify the property got created
        String propertyURL = URL_SERVER_PROPERTY + "/" + propertyKey;
        response = get (propertyURL);
        assertTrue(isSuccess(response));
        Map<String, String> entity = getEntityValues(response);
        assertTrue(entity.get("name").equals(propertyKey));
        assertTrue(entity.get("value").equals(propertyValue));

        // Verify property update
        propertyValue = generateRandomString();
        param.put("value", propertyValue);
        response = getClient().target(getAddress(URL_SERVER_PROPERTY))
                .request(RESPONSE_TYPE)
                .put(Entity.entity(MarshallingUtils.getXmlForProperties(param), MediaType.APPLICATION_XML), Response.class);
        assertTrue(isSuccess(response));
        response = get (propertyURL);
        assertTrue(isSuccess(response));
        entity = getEntityValues(response);
        assertTrue(entity.get("name").equals(propertyKey));
        assertTrue(entity.get("value").equals(propertyValue));

        //Clean up to leave domain.xml good for next run
        response = delete(propertyURL, new HashMap<String, String>());
        assertTrue(isSuccess(response));
    }

}
