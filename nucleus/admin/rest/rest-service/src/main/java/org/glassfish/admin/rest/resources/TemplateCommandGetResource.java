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

// Portions Copyright [2016-2019] [Payara Foundation and/or affiliates]

package org.glassfish.admin.rest.resources;


import org.glassfish.api.admin.ParameterMap;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.admin.rest.Constants;

import org.glassfish.jersey.media.sse.SseFeature;

/**
 *
 * @author ludovic champenois ludo@dev.java.net
 * Code moved from generated classes to here. Gen code inherits from this template class
 * that contains the logic for mapped commands RS Resources
 *
 */
public class TemplateCommandGetResource extends TemplateExecCommand {

    public TemplateCommandGetResource(String resourceName, String commandName, String commandMethod, boolean b) {
        super(resourceName, commandName, commandMethod, "GET", commandName, b);
    }

    @GET
    @Produces({
        MediaType.TEXT_HTML,
        MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML,
        MediaType.APPLICATION_FORM_URLENCODED})
    public Response processGetLegacyFormat() {
        return executeCommandLegacyFormat(prepareParameters());
    }

    @GET
    @Produces(Constants.MEDIA_TYPE_JSON)
    public CommandResult processGet() {
        return executeCommand(prepareParameters());
    }

    @GET
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public Response processSseGet() {
        return executeCommandAsSse(prepareParameters());
    }

    private ParameterMap prepareParameters() {
        ParameterMap data = new ParameterMap();
        processCommandParams(data);
        addQueryString(uriInfo.getQueryParameters(), data);
        purgeEmptyEntries(data);
        adjustParameters(data);
        return data;
    }
}
