package fish.payara.appserver.rest.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
/**
 *
 * @author Matt Gill
 */
public class RestEndpointTest {

    @Test
    public void parseAnnotations() throws NoSuchMethodException {
        RestEndpointModel endpoint1 = RestEndpointModel.generateFromMethod(new TestClass().getClass().getDeclaredMethod("returnNothing", null));
        assertEquals(endpoint1.getPath(), "/rootpath");
        assertEquals(endpoint1.getRequestMethod(), HttpMethod.GET);
        
        RestEndpointModel endpoint2 = RestEndpointModel.generateFromMethod(new TestClass().getClass().getDeclaredMethod("returnNothingAgain", null));
        assertEquals(endpoint2.getPath(), "/rootpath");
        assertEquals(endpoint2.getRequestMethod(), HttpMethod.GET);
        
        RestEndpointModel endpoint3 = RestEndpointModel.generateFromMethod(new TestClass().getClass().getDeclaredMethod("returnAnother", null));
        assertEquals(endpoint3.getPath(), "/rootpath/another/{id}");
        assertEquals(endpoint3.getRequestMethod(), HttpMethod.POST);
        
        RestEndpointModel endpoint4 = RestEndpointModel.generateFromMethod(new TestClass().getClass().getDeclaredMethod("notAnEndpoint", null));
        assertEquals(endpoint4, null);
    }

    @Path("/rootpath")
    class TestClass {

        @Path("/")
        @GET
        public Object returnNothing() {
            return null;
        }

        @GET
        public Object returnNothingAgain() {
            return null;
        }

        @Path("/another/{id}")
        @POST
        public String returnAnother() {
            return "Another";
        }
        
        public String notAnEndpoint() {
            return "Null";
        }
    }
}
