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

import java.util.Map;
import javax.ws.rs.core.Response;
import org.glassfish.admin.rest.client.utils.MarshallingUtils;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 *
 * @author jasonlee
 */
public class MetadataTest extends RestTestBase {
    protected static final String URL_CONFIG = "/domain/configs/config.json";
    protected static final String URL_UPTIMECOMMAND = "/domain/uptime.json";
    
    @Test
    public void configParameterTest() {
        Response response = options(URL_CONFIG);
        assertTrue(isSuccess(response));
        // Really dumb test.  Should be good enough for now

        Map extraProperties = MarshallingUtils.buildMapFromDocument(response.readEntity(String.class));
        assertNotNull(extraProperties);

        // Another dumb test to make sure that "name" shows up on the HTML page
        response = getClient().target(getAddress(URL_CONFIG)).request().get(Response.class);
        assertTrue(response.readEntity(String.class).contains("extraProperties"));
    }
    
    @Test
    public void UpTimeMetadaDataTest() {
        Response response = options(URL_UPTIMECOMMAND);
        assertTrue(isSuccess(response));

        Map extraProperties = MarshallingUtils.buildMapFromDocument(response.readEntity(String.class));
        assertNotNull(extraProperties);

        // Another dumb test to make sure that "extraProperties" shows up on the HTML page
        response = getClient().target(getAddress(URL_UPTIMECOMMAND)).request().get(Response.class);
        String resp = response.readEntity(String.class);
        assertTrue(resp.contains("extraProperties"));
        // test to see if we get the milliseconds parameter description which is an
        //optional param metadata for the uptime command
        assertTrue(resp.contains("milliseconds"));
        assertTrue(resp.contains("GET"));
    }
}
