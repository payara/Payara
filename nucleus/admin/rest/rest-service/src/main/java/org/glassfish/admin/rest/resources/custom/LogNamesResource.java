/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.server.logging.logviewer.backend.LogFilter;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.LogManager;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Vector;
import javax.ws.rs.QueryParam;

/**
 * REST resource to get Log Names
 * simple wrapper around internal  LogFilter  class
 *
 * @author ludovic Champenois
 */
public class LogNamesResource {

    protected ServiceLocator habitat = Globals.getDefaultBaseServiceLocator();

    @GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    public String getLogNamesJSON(@QueryParam("instanceName") String instanceName) throws IOException {
        return getLogNames(instanceName, "json");
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML})
    public String getLogNamesJXML(@QueryParam("instanceName") String instanceName) throws IOException {
        return getLogNames(instanceName, "xml");
    }

    private String getLogNames(String instanceName, String type) throws IOException {

        if (habitat.getService(LogManager.class) == null) {
            //the logger service is not install, so we cannot rely on it.
            //return an error
            throw new IOException("The GlassFish LogManager Service is not available. Not installed?");
        }

        LogFilter logFilter = habitat.getService(LogFilter.class);

        return convertQueryResult(logFilter.getInstanceLogFileNames(instanceName),type);

    }

    private String quoted(String s) {
        return "\"" + s + "\"";
    }

    private String convertQueryResult(Vector v, String type) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        if (type.equals("json")) {
            sb.append("{\"InstanceLogFileNames\": [");
        } else {
            sb.append("<InstanceLogFileNames>\n");
        }

        // extract every record
        for (int i = 0; i < v.size(); ++i) {
            String name = (String) v.get(i);

            if (type.equals("json")) {
                sb.append(sep);
                sb.append(quoted(name));
                sep = ",";
            } else {
                sb.append("<" + name + "/>");

            }

        }
        if (type.equals("json")) {
            sb.append("]}\n");
        } else {
            sb.append("\n</InstanceLogFileNames>\n");

        }

        return sb.toString();
    }
}
