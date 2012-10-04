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

package org.glassfish.resources.javamail.admin.cli;

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
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resources.javamail.config.MailResource;
import org.glassfish.resourcebase.resources.util.BindableResourcesHelper;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * List Mail Resources command
 *
 */
@TargetType(value={CommandTarget.DAS,CommandTarget.DOMAIN, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTERED_INSTANCE })
@ExecuteOn(value={RuntimeType.DAS})
@Service(name="list-javamail-resources")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.javamail.resources")
@RestEndpoints({
    @RestEndpoint(configBean=Resources.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-javamail-resources", 
        description="List JavaMail Resources")
})
public class ListJavaMailResources implements AdminCommand {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ListJavaMailResources.class);

    @Param(primary = true, optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String targetOperand;

    @Param(optional = true, obsolete = true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Inject
    private BindableResourcesHelper bindableResourcesHelper;

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
            List<String> list = new ArrayList<String>();
            Collection<MailResource> mailResources =
                    domain.getResources().getResources(MailResource.class);
            for (MailResource mailResource : mailResources) {
                if(bindableResourcesHelper.resourceExists(mailResource.getJndiName(), targetOperand)){
                    list.add(mailResource.getJndiName());
                }
            }

            for (String jndiName : list) {
                final ActionReport.MessagePart part =
                        report.getTopMessagePart().addChild();
                part.setMessage(jndiName);
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString(
                    "list.javamail.resources.failed",
                    "Unable to list mail resources") + " " +
                    e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }
}
