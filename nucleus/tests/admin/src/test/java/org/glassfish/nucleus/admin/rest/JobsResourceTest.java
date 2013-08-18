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

import java.util.Locale;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.composite.CompositeUtil;
import org.glassfish.admin.rest.resources.composite.Job;
import static org.glassfish.tests.utils.NucleusTestUtils.nadminWithOutput;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 *
 * @author jdlee
 */
@Test(testName="JobsResourceTest")
public class JobsResourceTest extends RestTestBase {
    public static final String URL_JOBS = "/jobs";

    public void testJobsListing() {
        assertTrue(isSuccess(get(URL_JOBS)));
    }

    public void testGetJob() throws JSONException {
        // make sure we have at least one job
        issueDetachedCommand();

        // verify getting the collection
        Response response = get(URL_JOBS);
        assertTrue(isSuccess(response));

        // verify the overall structure
        JSONObject json = response.readEntity(JSONObject.class);
        JSONArray resources = json.getJSONArray("resources");
        assertNotNull(resources);
        assertTrue(resources.length() > 0);
        JSONArray items = json.getJSONArray("items");
        assertNotNull(items);
        assertTrue(items.length() > 0);

        // unlike most resources that also return a parent link,
        // the jobs resource only returns child links.
        // verify the first of them
        JSONObject resource = resources.getJSONObject(0);
        String uri = resource.getString("uri");
        assertNotNull(uri);
        assertEquals("job", resource.getString("rel"));
        String jobId = resource.getString("title");
        assertNotNull(jobId);
        assertTrue(uri.endsWith(URL_JOBS + "/id/" + jobId));

        // verify the job it refers to by following the link.
        // it should only have a parent link
        response = get(uri);
        assertTrue(isSuccess(response));
        json = response.readEntity(JSONObject.class);
        JSONObject item = json.getJSONObject("item");
        verifyItem(jobId, item);
        resources = json.getJSONArray("resources");
        assertNotNull(resources);
        assertTrue(resources.length() == 1);
        resource = resources.getJSONObject(0);
        assertEquals("parent", resource.getString("rel"));
        assertTrue(resource.getString("uri").endsWith(URL_JOBS));
        
        // verify that the collection returned the item too
        item = null;
        for (int i = 0; item == null && i < items.length(); i++) {
            JSONObject thisItem = items.getJSONObject(i);
            if (jobId.equals(thisItem.getString("jobId"))) {
                item = thisItem;
            }
        }
        verifyItem(jobId, item);
    }

    private void verifyItem(String jobIdWant, JSONObject itemHave) throws JSONException {
        assertNotNull(itemHave);
        Locale locale = null;
        Job job = CompositeUtil.instance().unmarshallClass(locale, Job.class, itemHave);
        assertNotNull(job);
        assertEquals(jobIdWant, job.getJobId());
    }

    private void issueDetachedCommand() {
        nadminWithOutput("--detach", "uptime");
    }

    @Override
    protected String getResponseType() {
        return Constants.MEDIA_TYPE_JSON;
    }
}
