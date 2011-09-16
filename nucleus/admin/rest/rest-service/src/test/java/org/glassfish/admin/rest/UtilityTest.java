/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest;

import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.server.impl.application.WebApplicationContext;
import com.sun.jersey.server.impl.application.WebApplicationImpl;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.WebApplication;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriInfo;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jdlee
 */
public class UtilityTest {

    @Test
    public void parameterResolutionTest() {
        WebApplicationImpl wai = new WebApplicationImpl();
        ContainerRequest r = new TestHttpRequestContext(wai,
                "GET",
                null,
                "/management/domain/one/two/three/four/five/six/seven/eight/nine/ten/endpoint",
                "/management/domain/");
        UriInfo ui = new WebApplicationContext(wai, r, null);
        Map<String, String> commandParams = new HashMap<String, String>() {{
           put("foo", "$parent");
           put("bar", "$grandparent3");
           put("baz", "$grandparent5");
        }};
        
        ResourceUtil.resolveParamValues(commandParams, ui);
        assertEquals("ten", commandParams.get("foo"));
        assertEquals("seven", commandParams.get("bar"));
        assertEquals("five", commandParams.get("baz"));
    }

    private class TestHttpRequestContext extends ContainerRequest {

        public TestHttpRequestContext(
                WebApplication wa,
                String method,
                InputStream entity,
                String completeUri,
                String baseUri) {

            super(wa, method, URI.create(baseUri), URI.create(completeUri), new InBoundHeaders(), entity);
        }
    }
}
