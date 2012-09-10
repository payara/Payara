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

package org.glassfish.admin.monitor.cli;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.admin.monitor.jndi.JndiNameLookupHelper;
import com.sun.enterprise.config.serverbeans.Resources;

import javax.naming.NamingException;
import java.util.List;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;

@Service(name = "list-jndi-entries")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.jndi.entries")
@ExecuteOn(value={RuntimeType.INSTANCE})
@TargetType(value={CommandTarget.DOMAIN, CommandTarget.DAS, 
                   CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER,
                   CommandTarget.CLUSTERED_INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=Resources.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-jndi-entries", 
        description="list-jndi-entries")
})
public class ListJndiEntries implements AdminCommand {

   final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ListJndiEntries.class);

    @Param(name="context", optional = true)
    String contextName;

    @Param(primary = true, optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;

    public void execute(AdminCommandContext context) {
        List<String> names = null;
        final ActionReport report = context.getActionReport();

        try {
            names = getNames(contextName);
        } catch (NamingException e) {
            report.setMessage(localStrings.getLocalString("list.jndi.entries.namingexception", "Naming Exception caught.")
                    + " " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
                                        
        try {
            if (names.isEmpty()) {
                final ActionReport.MessagePart part =
                        report.getTopMessagePart().addChild();
                part.setMessage(localStrings.getLocalString(
                        "list.jndi.entries.empty",
                        "Nothing to list."));
            } else {
                for (String jndiName : names) {
                    final ActionReport.MessagePart part =
                            report.getTopMessagePart().addChild();
                    part.setMessage(jndiName);
                }
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("" +
                    "list.jndi.entries.fail",
                    "Unable to list jndi entries.") + " " +
                    e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

    private List<String> getNames(String context)
            throws NamingException {
        List<String> names = null;
        JndiNameLookupHelper helper = new JndiNameLookupHelper();
        names = helper.getJndiEntriesByContextPath(context);
        return names;
    }
}
