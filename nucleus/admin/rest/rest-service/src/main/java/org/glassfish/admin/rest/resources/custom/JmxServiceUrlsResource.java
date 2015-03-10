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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Dom;

/**
 *
 * @author jasonlee
 */
public class JmxServiceUrlsResource {
    @Context
    protected ServiceLocator habitat;

    public void setEntity(Dom p) {
        // ugly no-op hack. For now.
    }

    @GET
    @Produces({"text/html",MediaType.APPLICATION_JSON+";qs=0.5",MediaType.APPLICATION_XML+";qs=0.5"})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    public ActionReportResult getJmxServiceUrl() {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            JMXServiceURL[] urls = (JMXServiceURL[]) mBeanServer.getAttribute(getBootAMXMBeanObjectName(), "JMXServiceURLs");
            List<String> jmxUrls = new ArrayList<String>();
            for (JMXServiceURL url : urls) {
                jmxUrls.add(url.getURLPath());
            }
            RestActionReporter ar = new RestActionReporter();
            ar.setActionDescription("Get JMX Service URLs");
            ar.setSuccess();
            ar.getExtraProperties().put("jmxServiceUrls", jmxUrls);
            return new ActionReportResult(ar);
        } catch (final JMException e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectName getBootAMXMBeanObjectName() {
        try {
            return new ObjectName("amx-support:type=boot-amx");
        } catch (final Exception e) {
            throw new RuntimeException("bad ObjectName", e);
        }
    }
}
