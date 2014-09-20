/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.plugin.common;

import org.glassfish.web.config.serverbeans.WebModuleConfig;
import org.glassfish.web.config.serverbeans.ContextParam;
import com.sun.enterprise.config.serverbeans.Application;
import java.text.MessageFormat;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 *
 * @author tjquinn
 */
@Service(name="list-web-context-param")
@I18n("listWebContextParam.command")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=Application.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-web-context-param", 
        description="list-web-context-param",
        params={
            @RestParam(name="name", value="$parent")
        })
})
public class ListWebContextParamCommand extends WebModuleConfigCommand {
    @Param(name="name",optional=true)
    private String name;

    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();

        WebModuleConfig config = webModuleConfig(report);
        if (config == null) {
            return;
        }

        ActionReport.MessagePart part = report.getTopMessagePart();
        final String format = localStrings.getLocalString(
                "listWebContextParamFormat", "{0} = {1} ignoreDescriptorItem={2} //{3}");
        int reported = 0;
        for (ContextParam param : config.contextParamsMatching(name)) {
            ActionReport.MessagePart childPart = part.addChild();
            childPart.setMessage(MessageFormat.format(format,
                    param.getParamName(),
                    param.getParamValue(),
                    param.getIgnoreDescriptorItem(),
                    descriptionValueOrNotSpecified(param.getDescription())));
            reported++;
        }
        succeed(report, "listSummary",
                "Reported {0,choice,0#no {1} settings|1#one {1} setting|1<{0,number,integer} {1} settings}",
                reported, "context-param");
    }
}
