/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;
import org.glassfish.internal.config.UnprocessedConfigListener;

/**
 * Return the "restart required" flag.
 *
 * @author Bill Shannon
 */
@Service(name = "_get-restart-required")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.restart.required.command")
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="_get-restart-required", 
        description="Restart Reasons")
})
@AccessRequired(resource="domain", action="dump")
public class GetRestartRequiredCommand implements AdminCommand {
    @Param(optional = true)
    private boolean why;

    @Inject
    private UnprocessedConfigListener ucl;

    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ActionReport.MessagePart mp = report.getTopMessagePart();

        Properties extraProperties = new Properties();
        Map<String, Object> entity = new HashMap<String, Object>();
        mp.setMessage(Boolean.toString(ucl.serverRequiresRestart()));
        entity.put("restartRequired", Boolean.toString(ucl.serverRequiresRestart()));
        List<String> unprocessedChanges = new ArrayList<String>();

        for (UnprocessedChangeEvents es : ucl.getUnprocessedChangeEvents()) {
            for (UnprocessedChangeEvent e : es.getUnprocessed()) {
                if (why) {
                    mp.addChild().setMessage(e.getReason());
                }
                unprocessedChanges.add(e.getReason());
            }
        }

        if (!unprocessedChanges.isEmpty()) {
            entity.put("unprocessedChanges", unprocessedChanges);
        }
        extraProperties.put("entity", entity);
        ((ActionReport) report).setExtraProperties(extraProperties);
    }
}
