/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.samples.mpjwt.fish6022;


import java.net.URI;

import javax.persistence.criteria.Root;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class InvalidTokenOnPublicEndpointTest {
    static final String MPCONFIG = "mp.jwt.verify.issuer=airhacks\n"
            + "mp.jwt.verify"
            + ".publickey=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAin3fGoTp6LNzNd5NtITVrQUl2vxnKGr249mRbHw02cZhLStaUMMFt8DR2Z5HfM8upR"
            + "+0Y6bnlrn3dQdm4kE5ri1vr05mWhjF1wGflKaux54VtXTR8Xuu1zeZzasxgxYeYp680r9pkYJw7kK4QYx4tEMo5FHKsitIOnTxxAT3+mpMVQEOPjTkt2r929p82XYO9WRR"
            + "/OwLcHH28s9epY+eNfQIjZ2FHawF2NJeyN3fUyJqUdRsrKoodorOoog"
            + "/mMFimYB1XbctBeZRBE8utLtbyP8hhR2NkvAzGcmy7d7bS9aRbdH236DCcREg5iDpNCt5rDcHLO7ScDKEMMz/jFJ9zwIDAQAB";

    static final String AUTH = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(JaxrsApplication.class, RootResource.class, PublicServlet.class)
                .addAsManifestResource(new StringAsset(MPCONFIG), "microprofile-config.properties");
    }

    @ArquillianResource
    URI base;

    @Test
    public void passesWithoutTokenAccess() {
        WebTarget target = ClientBuilder.newClient().target(base).path("resources");

        Response response = target.request().header("Authorization", AUTH).get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void unauthorizedOnTokenAccess() {
        WebTarget target = ClientBuilder.newClient().target(base).path("resources").path("token");

        Response response = target.request().header("Authorization", AUTH).get();
        assertEquals(401, response.getStatus());
    }

    @Test
    public void servletPassesWithoutTokenAccess() {
        WebTarget target = ClientBuilder.newClient().target(base).path("servlet");

        Response response = target.request().header("Authorization", AUTH).get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void servletUnauthorizedOnTokenAccess() {
        WebTarget target = ClientBuilder.newClient().target(base).path("servlet");

        Response response = target.queryParam("token", "true").request().header("Authorization", AUTH).get();
        assertEquals(500, response.getStatus());
    }

}
