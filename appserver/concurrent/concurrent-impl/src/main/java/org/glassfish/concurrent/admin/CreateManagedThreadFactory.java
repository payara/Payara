/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.concurrent.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resources.admin.cli.ResourceConstants;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Properties;


/**
 * Create Managed Thread Factory Command
 *
 */
@TargetType(value={CommandTarget.DAS, CommandTarget.DOMAIN, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE })
@ExecuteOn(RuntimeType.ALL)
@Service(name="create-managed-thread-factory")
@PerLookup
@I18n("create.managed.thread.factory")
public class CreateManagedThreadFactory implements AdminCommand {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateManagedThreadFactory.class);

    @Param(name="jndi_name", primary=true)
    private String jndiName;

    @Param(optional=true, defaultValue="true")
    private Boolean enabled;

    @Param(name="contextinfoenabled", alias="contextInfoEnabled", defaultValue="true", optional=true)
    private Boolean contextinfoenabled;

    @Param(name="contextinfo", alias="contextInfo", defaultValue=ResourceConstants.CONTEXT_INFO_DEFAULT_VALUE, optional=true)
    private String contextinfo;

    @Param(name="threadpriority", alias="threadPriority", defaultValue=""+Thread.NORM_PRIORITY, optional=true)
    private Integer threadpriority;

    @Param(optional=true)
    private String description;

    @Param(name="property", optional=true, separator=':')
    private Properties properties;

    @Param(optional=true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Inject
    private Domain domain;

    @Inject
    private ManagedThreadFactoryManager managedThreadFactoryMgr;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        HashMap attrList = new HashMap();
        attrList.put(ResourceConstants.JNDI_NAME, jndiName);
        attrList.put(ResourceConstants.CONTEXT_INFO_ENABLED, contextinfoenabled.toString());
        attrList.put(ResourceConstants.CONTEXT_INFO, contextinfo);
        attrList.put(ResourceConstants.THREAD_PRIORITY, 
            threadpriority.toString());
        attrList.put(ServerTags.DESCRIPTION, description);
        attrList.put(ResourceConstants.ENABLED, enabled.toString());
        ResourceStatus rs;

        try {
            rs = managedThreadFactoryMgr.create(domain.getResources(), attrList, properties, target);
        } catch(Exception e) {
            report.setMessage(localStrings.getLocalString("create.managed.thread.factory.failed", "Managed thread factory {0} creation failed", jndiName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        if (rs.getMessage() != null){
             report.setMessage(rs.getMessage());
        }
        if (rs.getStatus() == ResourceStatus.FAILURE) {
            ec = ActionReport.ExitCode.FAILURE;
            if (rs.getException() != null)
                report.setFailureCause(rs.getException());
        }
        report.setActionExitCode(ec);
    }
}
