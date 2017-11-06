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
package fish.payara.appserver.rest.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.HEAD;
import javax.ws.rs.DELETE;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Matt Gill
 */
public class RestEndpointTest {

    @Test
    public void testClass1() throws NoSuchMethodException {
        RestEndpointModel endpoint1 = RestEndpointModel.generateFromMethod(new TestClass().getClass().getDeclaredMethod("returnNothing"));
        assertEquals(endpoint1.getPath(), "/rootpath");
        assertEquals(endpoint1.getRequestMethod(), HttpMethod.GET);

        RestEndpointModel endpoint2 = RestEndpointModel.generateFromMethod(new TestClass().getClass().getDeclaredMethod("returnNothingAgain"));
        assertEquals(endpoint2.getPath(), "/rootpath");
        assertEquals(endpoint2.getRequestMethod(), HttpMethod.GET);

        RestEndpointModel endpoint3 = RestEndpointModel.generateFromMethod(new TestClass().getClass().getDeclaredMethod("returnAnother"));
        assertEquals(endpoint3.getPath(), "/rootpath/another/{id}");
        assertEquals(endpoint3.getRequestMethod(), HttpMethod.POST);

        RestEndpointModel endpoint4 = RestEndpointModel.generateFromMethod(new TestClass().getClass().getDeclaredMethod("deleteAnother"));
        assertEquals(endpoint4.getPath(), "/rootpath/another/{id}");
        assertEquals(endpoint4.getRequestMethod(), HttpMethod.DELETE);

        RestEndpointModel endpoint5 = RestEndpointModel.generateFromMethod(new TestClass().getClass().getDeclaredMethod("notAnEndpoint"));
        assertEquals(endpoint5, null);
    }

    @Test
    public void testClass2() throws NoSuchMethodException {
        RestEndpointModel endpoint1 = RestEndpointModel.generateFromMethod(new TestClass2().getClass().getDeclaredMethod("returnNothing"));
        assertEquals(endpoint1.getPath(), "/another");
        assertEquals(endpoint1.getRequestMethod(), HttpMethod.GET);

        RestEndpointModel endpoint2 = RestEndpointModel.generateFromMethod(new TestClass2().getClass().getDeclaredMethod("returnNothingAgain"));
        assertEquals(endpoint2.getPath(), "/another");
        assertEquals(endpoint2.getRequestMethod(), HttpMethod.GET);

        RestEndpointModel endpoint3 = RestEndpointModel.generateFromMethod(new TestClass2().getClass().getDeclaredMethod("returnAnother"));
        assertEquals(endpoint3.getPath(), "/another/{testid}");
        assertEquals(endpoint3.getRequestMethod(), HttpMethod.POST);
    }

    @Test
    public void testClass3() throws NoSuchMethodException {
        RestEndpointModel endpoint1 = RestEndpointModel.generateFromMethod(new TestClass3().getClass().getDeclaredMethod("returnNothing"));
        assertEquals(endpoint1.getPath(), "/");
        assertEquals(endpoint1.getRequestMethod(), HttpMethod.HEAD);
    }

    /**
     * Default test class.
     */
    @Path("/rootpath")
    class TestClass {

        /**
         * Test the default case of having a root response.
         *
         * @return nothing
         */
        @Path("/")
        @GET
        public Object returnNothing() {
            return null;
        }

        /**
         * Test the case of having no path annotation.
         *
         * @return nothing
         */
        @GET
        public Object returnNothingAgain() {
            return null;
        }

        /**
         * Test the case of having an endpoint on the end.
         *
         * @return nothing
         */
        @Path("/another/{id}")
        @POST
        public String returnAnother() {
            return null;
        }

        /**
         * Test the case of having the same endpoint with a different request method
         *
         * @return nothing
         */
        @Path("/another/{id}")
        @DELETE
        public String deleteAnother() {
            return null;
        }

        /**
         * Test the case of a method which isn't an endpoint.
         *
         * @return nothing
         */
        public String notAnEndpoint() {
            return null;
        }
    }

    /**
     * Test class for objects with no leading or trailing slashes.
     */
    @Path("another")
    class TestClass2 {

        /**
         * Test the case of having a trailing slash.
         *
         * @return nothing
         */
        @Path("/")
        @GET
        public Object returnNothing() {
            return null;
        }

        /**
         * Test the case of having the default endpoint with no slashes at all.
         *
         * @return nothing
         */
        @GET
        public Object returnNothingAgain() {
            return null;
        }

        /**
         * Test the case of having a parameter, but still no slashes
         *
         * @return nothing
         */
        @Path("{testid}")
        @POST
        public String returnAnother() {
            return null;
        }
    }

    /**
     * Test class for objects at the jersey context root
     */
    @Path("/")
    class TestClass3 {

        @HEAD
        public Object returnNothing() {
            return null;
        }
    }
}
