/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.resources.custom;

import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.utils.ProxyImpl;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.jvnet.hk2.config.Dom;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * @author Mitesh Meswani
 */
@Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5"})
@Path("domain/proxy/{path:.*}")
public class ManagementProxyResource {
    @Context
    protected UriInfo uriInfo;

    @Context
    protected ServiceLocator habitat;

    @GET
    public ActionReportResult proxyRequest() {

        RestActionReporter ar = new RestActionReporter();
        ar.setActionDescription("Proxied Data");
        ar.setSuccess();

        ActionReportResult result = new ActionReportResult(ar);

        Properties proxiedResponse = new ManagementProxyImpl().proxyRequest(uriInfo, Util.getJerseyClient(), habitat);
        ar.setExtraProperties(proxiedResponse);
        return result;
    }

    private static class ManagementProxyImpl extends ProxyImpl {
        private static int TARGET_INSTANCE_NAME_PATH_INDEX = 2; //pathSegments == { "domain", "proxy", "instanceName", ....}

        @Override
        public UriBuilder constructTargetURLPath(UriInfo sourceUriInfo, URL responseURLReceivedFromTarget) {
            return sourceUriInfo.getBaseUriBuilder().replacePath(responseURLReceivedFromTarget.getFile());
        }

        @Override
        public UriBuilder constructForwardURLPath(UriInfo sourceUriInfo) {
            // The sourceURI is of the form /mangement/domain/proxy/<instanceName>/forwardSegment1/forwardSegment2/....
            // The forwardURI constructed is of the form /mangement/domain/forwardSegment1/forwardSegment2/....
            List<PathSegment> sourcePathSegments = sourceUriInfo.getPathSegments();
            List<PathSegment> forwardPathSegmentsHead =  sourcePathSegments.subList(0, TARGET_INSTANCE_NAME_PATH_INDEX - 1); //path that precedes proxy/<instancenName>
            List<PathSegment> forwardPathSegmentsTail =  sourcePathSegments.subList(TARGET_INSTANCE_NAME_PATH_INDEX + 1, sourcePathSegments.size()); //path that follows <instanceName>
            UriBuilder forwardUriBuilder = sourceUriInfo.getBaseUriBuilder(); // Gives /management/domain
            for (PathSegment pathSegment : forwardPathSegmentsHead) { //append domain
                forwardUriBuilder.segment(pathSegment.getPath());
            }

            for (PathSegment pathSegment : forwardPathSegmentsTail) { //append forwardSegment1/forwardSegment2/....
                forwardUriBuilder.segment(pathSegment.getPath());
            }
            return forwardUriBuilder;
        }

        @Override
        public String extractTargetInstanceName(UriInfo uriInfo) {
            return uriInfo.getPathSegments().get(TARGET_INSTANCE_NAME_PATH_INDEX).getPath();
        }
    }

    public void setEntity(Dom p) {
        // ugly no-op hack to keep the generated code happy.
    }

}
