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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]

package org.glassfish.admin.cli.resources;

import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.RefContainer;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

import javax.inject.Inject;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommandSecurity;

/**
 * List Resource Refs Command
 * 
 */
@TargetType(value={CommandTarget.CONFIG, CommandTarget.DAS, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.DEPLOYMENT_GROUP})
@ExecuteOn(value={RuntimeType.DAS})
@Service(name="list-resource-refs")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.resource.refs")
@RestEndpoints({
    @RestEndpoint(configBean=Resources.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-resource-refs", 
        description="list-resource-refs")
})
public class ListResourceRefs implements AdminCommand, AdminCommandSecurity.Preauthorization,
            AdminCommandSecurity.AccessCheckProvider {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListResourceRefs.class);

    @Param(optional=true, primary=true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Inject
    private ConfigBeansUtilities configBeansUtilities;
    
    @Inject
    private Domain domain;

    @AccessRequired.To("read")
    private RefContainer refContainer;
    
    private List<ResourceRef> resourceRefs = null;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        refContainer = CLIUtil.chooseRefContainer(domain, target, configBeansUtilities);
        if (refContainer != null) {
            resourceRefs = refContainer.getResourceRef();
        }
        return true;
    }

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final Collection<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        for (ResourceRef rr : resourceRefs) {
            accessChecks.add(new AccessCheck(rr, "read", true /* isFailureFatal */));
        }
        return accessChecks;
    }
    
    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        
        try {
            if (resourceRefs != null) {
                processResourceRefs(report, resourceRefs);
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            }
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("list.resource.refs.failed",
                    "list-resource-refs failed"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
            
    }

    private void processResourceRefs(ActionReport report, List<ResourceRef> resourceRefs) {
        if (resourceRefs.isEmpty()) {
            final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
            part.setMessage(localStrings.getLocalString(
                    "NothingToList", "Nothing to List."));
        } else {
            for (ResourceRef ref : resourceRefs) {
                final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                part.setMessage(ref.getRef());
            }
        }
    }
}
