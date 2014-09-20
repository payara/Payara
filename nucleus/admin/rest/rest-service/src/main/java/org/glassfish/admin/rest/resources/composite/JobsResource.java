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
package org.glassfish.admin.rest.resources.composite;

import com.sun.enterprise.v3.admin.commands.ListJobsCommand;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.glassfish.admin.rest.composite.CompositeResource;
import org.glassfish.admin.rest.composite.CompositeUtil;
import org.glassfish.admin.rest.model.RestCollectionResponseBody;
import org.glassfish.api.ActionReport;
import org.glassfish.admin.rest.utils.StringUtil;
import org.glassfish.api.admin.ParameterMap;
import org.jvnet.hk2.annotations.Service;

/**
 * This resource queries for the system for all detached jobs.
 * <h2>Example Interactions</h2>
 * <h4>View the detached jobs</h4>
 *
 * <div class="codeblock">
 * $ curl --user admin:admin123 -v \
 *   -H Accept:application/vnd.oracle.glassfish+json \
 *   -H Content-Type:application/vnd.oracle.glassfish+json \
 *   -H X-Requested-By:MyClient \
 *   http://localhost:4848/management/jobs
 *
 * HTTP/1.1 200 OK
 * {
 *     "items": [{
 *             "exitCode": "COMPLETED",
 *             "jobId": "1",
 *             "jobName": "load-sdp",
 *             "jobState": "COMPLETED",
 *             "executionDate": "Wed Jan 02 11:36:38 CST 2013",
 *             "message": "SDP loaded with name nucleusSDP.",
 *             "user": "admin"
 *         }],
 *     "metadata": [{"id": "http:\/\/localhost:4848\/management\/jobs\/id\/1"}]
 * }
 * </div>
 * @author jdlee
 */
@Service
@Path("/jobs")
public class JobsResource extends CompositeResource {

    /**
     * The GET method on this resource returns a list of Job entities that represent
     * each recent or current job known to this GlassFish instance.
     * <p>
     * Roles: PaasAdmin, TenantAdmin
     * <p>
     *
     * @param currentUser Optional query parameter to restrict the set of returns {@link Job} objects
     * to those for the current user
     * @return A collection of Job entities which contains information for each job resource.
     *  For each job returned, the <code>jobId</code> field can be used to format the URI to
     *  interact with a specific job.
     * @throws Exception
     */
    @GET
    public RestCollectionResponseBody<Job> getItems(
        @QueryParam("currentUser") @DefaultValue("false") final boolean currentUser,
            @QueryParam(INCLUDE) final String include,
            @QueryParam(EXCLUDE) final String exclude) throws Exception {
        RestCollectionResponseBody<Job> rb =
            restCollectionResponseBody(Job.class, "job", null); // there is no parent resource
        ActionReport ar = executeReadCommand(getCommandName(), getParameters());
        Collection<Map<String, Object>> jobMaps = (List<Map<String, Object>>) ar.getExtraProperties().get("jobs");
        if (jobMaps != null) {
            for (Map<String, Object> jobMap : jobMaps) {
                if (currentUser
                        && !StringUtil.compareStrings((String) jobMap.get(ListJobsCommand.USER), this.getAuthenticatedUser())) {
                    continue;
                }
                if (jobMap == null) {
                    continue;
                }
                Job model = constructJobModel(jobMap);
                rb.addItem(filterModel(Job.class, model, include, exclude, "jobId"), model.getJobId());
            }
        }
        return rb;
    }

    @Path("id/{jobId}")
    public JobResource getJobResource() {
        return getSubResource(JobResource.class);
    }

    protected String getCommandName() {
        return "list-jobs";
    }

    protected ParameterMap getParameters() {
        return new ParameterMap();
    }

    static Job constructJobModel(Map<String, Object> jobMap) {
        Job model = CompositeUtil.instance().getModel(Job.class);
        model.setJobId((String) jobMap.get(ListJobsCommand.ID));
        model.setJobName((String) jobMap.get(ListJobsCommand.NAME));
        model.setExecutionDate(jobMap.get(ListJobsCommand.DATE).toString());
        model.setCompletionDate(jobMap.get(ListJobsCommand.COMPLETION_DATE).toString());
        model.setJobState(jobMap.get(ListJobsCommand.STATE).toString());
        model.setExitCode((String) jobMap.get(ListJobsCommand.CODE));
        model.setMessage((String) jobMap.get(ListJobsCommand.MESSAGE));
        model.setUser((String) jobMap.get(ListJobsCommand.USER));
        return model;
    }
}
