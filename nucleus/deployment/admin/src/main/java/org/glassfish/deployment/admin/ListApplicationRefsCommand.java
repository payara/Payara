/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.admin;

import com.sun.enterprise.util.ColumnFormatter;
import java.util.List;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.Param;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Domain;
import java.util.ArrayList;
import java.util.Collection;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import javax.inject.Inject;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

import org.glassfish.deployment.common.DeploymentUtils;

/**
 * List application ref command
 */
@Service(name="list-application-refs")
@I18n("list.application.refs")
@ExecuteOn(value={RuntimeType.DAS})
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@TargetType(value={CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean=Applications.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-application-refs", 
        description="list-applications-refs")
})
public class ListApplicationRefsCommand implements AdminCommand, AdminCommandSecurity.AccessCheckProvider {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListApplicationRefsCommand.class);

    @Param(primary=true, optional=true)
    String target = "server";

    @Param(optional=true, defaultValue="false", name="long", shortName="l")
    public Boolean long_opt = false;

    @Param(optional=true, defaultValue="false", shortName="t")
    public Boolean terse = false;

    @Inject
    Domain domain;
    
    private List<ApplicationRef> appRefs;

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final List<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        appRefs = domain.getApplicationRefsInTarget(target);
        for (ApplicationRef appRef : appRefs) {
            accessChecks.add(new AccessCheck(AccessRequired.Util.resourceNameFromConfigBeanProxy(appRef), "read"));
        }
        return accessChecks;
    }
    
    

    /**
     * Entry point from the framework into the command execution
     * @param context context for the command.
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final ActionReport subReport = report.addSubActionsReport();
        ColumnFormatter cf = new ColumnFormatter();

        ActionReport.MessagePart part = report.getTopMessagePart();
        int numOfApplications = 0;
        if ( !terse && long_opt ) {
            String[] headings= new String[] { "NAME", "STATUS" };
            cf = new ColumnFormatter(headings);
        }
        for (ApplicationRef ref : appRefs) {
            Object[] row = new Object[] { ref.getRef() };
            if( !terse && long_opt ){
                row = new Object[]{ ref.getRef(), getLongStatus(ref) };
            }
            cf.addRow(row);
            numOfApplications++;
        }
        if (numOfApplications != 0) {
            report.setMessage(cf.toString());
        } else if ( !terse) {
            subReport.setMessage(localStrings.getLocalString(
                    DeployCommand.class,
                    "NoSuchAppDeployed",
                    "No applications are deployed to this target {0}.",
                    new Object[] {this.target}));
            part.setMessage(localStrings.getLocalString("list.components.no.elements.to.list", "Nothing to List."));
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private String getLongStatus(ApplicationRef ref) {
       String message = "";
       if (DeploymentUtils.isDomainTarget(target)) {
           // ignore --verbose for target domain
           return message;
       }
       boolean isVersionEnabled = domain.isAppRefEnabledInTarget(ref.getRef(), target);
       if ( isVersionEnabled ) {
           message = localStrings.getLocalString("list.applications.verbose.enabled", "enabled");
       } else {
           message = localStrings.getLocalString("list.applications.verbose.disabled", "disabled");
       }
       return message;
   }
}
