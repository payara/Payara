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
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.glassfish.admin.rest.composite.CompositeResource;
import org.glassfish.admin.rest.composite.CompositeUtil;
import org.glassfish.admin.rest.composite.RestCollection;
import org.glassfish.api.ActionReport;
import org.glassfish.admin.rest.utils.StringUtil;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author jdlee
 */
@Service
@Path("/jobs")
public class JobsResources extends CompositeResource {

    @GET
    public RestCollection<Job> getJobs(@QueryParam("currentUser") @DefaultValue("false") final boolean currentUser) throws Exception {
        RestCollection<Job> rc = new RestCollection<Job>();
        ActionReport ar = executeReadCommand("list-jobs");
        Collection<Map<String, Object>> jobMaps = (List<Map<String, Object>>) ar.getExtraProperties().get("jobs");
        if (jobMaps != null) {
            for (Map<String, Object> jobMap : jobMaps) {
                if (currentUser
                        && !StringUtil.compareStrings((String) jobMap.get(ListJobsCommand.USER), this.getAuthenticatedUser())) {
                    continue;
                }
                Job model = constructJobModel(jobMap);
                URI uri = getChildItemUri(model.getJobId());
                rc.put(uri.toASCIIString(), model);
            }
        }
        return rc;
    }

    @Path("id/{jobId}")
    public JobResource getJobResource() {
        return getSubResource(JobResource.class);
    }

    public static Job constructJobModel(Map<String, Object> jobMap) {
        if (jobMap == null) {
            return null;
        }
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
