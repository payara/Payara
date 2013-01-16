/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.composite.CompositeUtil;
import org.glassfish.admin.rest.resources.composite.Job;
import static org.glassfish.tests.utils.NucleusTestUtils.nadminWithOutput;
import org.testng.Assert;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;

/**
 *
 * @author jdlee
 */
@Test(testName="JobsResourceTest")
public class JobsResourceTest extends RestTestBase {
    public static final String URL_JOBS = "/jobs";

    public void testJobsListing() {
        Assert.assertTrue(isSuccess(get(URL_JOBS)));
    }

    public void testGetJob() throws JSONException {
        issueDetachedCommand();
        Response response = get(URL_JOBS);
        JSONObject json = response.readEntity(JSONObject.class);
        assertNotNull(json.get("items"));
        final JSONArray metadata = json.getJSONArray("metadata");
        assertNotNull(metadata);
        Assert.assertTrue(json.getJSONArray("metadata").length() > 0);
        JSONObject jobRow = metadata.getJSONObject(0);
        String uri = jobRow.getString("id");
        response = get(uri);
        Assert.assertTrue(isSuccess(response));
        final JSONObject entity = response.readEntity(JSONObject.class);
        Job job = CompositeUtil.instance().unmarshallClass(Job.class, entity);
        Assert.assertNotNull(job);
    }

    private void issueDetachedCommand() {
        nadminWithOutput("--detach", "uptime");
    }

    @Override
    protected String getResponseType() {
        return Constants.MEDIA_TYPE_JSON;
    }
}
