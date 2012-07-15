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

package org.glassfish.connectors.admin.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.connectors.config.ResourceAdapterConfig;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.types.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * List Resource Adapter Configs command
 *
 */
@TargetType(value={CommandTarget.DAS,CommandTarget.DOMAIN, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTERED_INSTANCE })
@Service(name="list-resource-adapter-configs")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(value={RuntimeType.DAS})
@I18n("list.resource.adapter.configs")
@RestEndpoints({
    @RestEndpoint(configBean=Resources.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-resource-adapter-configs", 
        description="list-resource-adapter-configs")
})
public class ListResourceAdapterConfigs implements AdminCommand {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListResourceAdapterConfigs.class);    

    @Param(name="raname", optional=true)
    private String raName;

    @Param(name="long", optional=true, defaultValue="false", shortName="l", alias="verbose")
    private Boolean long_opt;

    @Param(primary = true, optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME, alias = "targetName", obsolete = true)
    private String target ;

    @Inject
    private Domain domain;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();

        try {
            HashMap<String, List<Property>> raMap = new HashMap<String, List<Property>>();
            boolean raExists = false;
            Collection<ResourceAdapterConfig> resourceAdapterConfigs =
                    domain.getResources().getResources(ResourceAdapterConfig.class);
            for (ResourceAdapterConfig r : resourceAdapterConfigs) {
                if (raName != null && !raName.isEmpty()) {
                    if (r.getResourceAdapterName().equals(raName)) {
                        raMap.put(raName, r.getProperty());
                        raExists = true;
                        break;
                    }
                } else {
                    raMap.put(r.getResourceAdapterName(), r.getProperty());
                }
            }
            if (raName != null && !raName.isEmpty() && !raExists) {
                report.setMessage(localStrings.getLocalString("delete.resource.adapter.config.notfound",
                        "Resource adapter {0} not found.", raName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
            /**
              get the properties if long_opt=true. Otherwise return the name.
             */
            if (long_opt) {
                for (Entry<String, List<Property>> raEntry : raMap.entrySet()) {
                    final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                    part.setMessage(raEntry.getKey());
                    for (Property prop : raEntry.getValue()) {
                        final ActionReport.MessagePart propPart = part.addChild();
                        propPart.setMessage("\t" + prop.getName() + "=" + prop.getValue());
                    }
                }
            } else {
                for (String ra : raMap.keySet()) {
                    final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                    part.setMessage(ra);
                }
            }
        } catch (Exception e) {
            String failMsg = localStrings.getLocalString("list.resource.adapter.configs.fail",
                    "Unable to list resource adapter configs.");
            Logger.getLogger(ListResourceAdapterConfigs.class.getName()).log(Level.SEVERE,
                    failMsg, e);
            report.setMessage(failMsg + " " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
