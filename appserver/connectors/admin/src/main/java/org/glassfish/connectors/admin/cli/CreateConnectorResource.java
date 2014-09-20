/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.glassfish.resources.admin.cli.ResourceConstants;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.connectors.admin.cli.CLIConstants.CR.*;
import static org.glassfish.connectors.admin.cli.CLIConstants.*;
import static org.glassfish.resources.admin.cli.ResourceConstants.JNDI_NAME;
import static org.glassfish.resources.admin.cli.ResourceConstants.POOL_NAME;


/**
 * Create Connector Resource Command
 *
 * @author Jennifer Chou, Jagadish Ramu
 * 
 */
@TargetType(value={CommandTarget.DAS,CommandTarget.DOMAIN, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE })
@RestEndpoints({
        @RestEndpoint(configBean=Resources.class,
                opType=RestEndpoint.OpType.POST,
                path="create-connector-resource",
                description="create-connector-resource")
})
@ExecuteOn(RuntimeType.ALL)
@Service(name="create-connector-resource")
@PerLookup
@I18n("create.connector.resource")
public class CreateConnectorResource implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateConnectorResource.class);    

    @Param(name=CR_POOL_NAME)
    private String poolName;

    @Param(name=CLIConstants.ENABLED, optional=true, defaultValue="true")
    private Boolean enabled;

    @Param(name=DESCRIPTION, optional=true)
    private String description;

    @Param(name=CR_OBJECT_TYPE, defaultValue="user", optional=true)
    private String objectType;
    
    @Param(name=PROPERTY, optional=true, separator=':')
    private Properties properties;
    
    @Param(name=TARGET, optional=true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Param(name=CR_JNDI_NAME, primary=true)
    private String jndiName;

    @Inject
    private Domain domain;

    @Inject
    private ConnectorResourceManager connResMgr;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        HashMap attrList = new HashMap();
        attrList.put(POOL_NAME, poolName);
        attrList.put(ResourceConstants.ENABLED, enabled.toString());
        attrList.put(JNDI_NAME, jndiName);
        attrList.put(ServerTags.DESCRIPTION, description);
        attrList.put(ServerTags.OBJECT_TYPE, objectType);

        ResourceStatus rs;

        try {
            rs = connResMgr.create(domain.getResources(), attrList, properties, target);
        } catch(Exception e) {
            Logger.getLogger(CreateConnectorResource.class.getName()).log(Level.SEVERE,
                    "Unable to create connector resource " + jndiName, e);
            String def = "Connector resource: {0} could not be created, reason: {1}";
            report.setMessage(localStrings.getLocalString("create.connector.resource.fail",
                    def, jndiName) + " " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        if(rs.getMessage() != null){
            report.setMessage(rs.getMessage());
        }
        if (rs.getStatus() == ResourceStatus.FAILURE) {
            ec = ActionReport.ExitCode.FAILURE;
            if (rs.getMessage() == null) {
                 report.setMessage(localStrings.getLocalString("create.connector.resource.fail",
                    "Connector resource {0} creation failed", jndiName, ""));
            }
            if (rs.getException() != null)
                report.setFailureCause(rs.getException());
        }
        report.setActionExitCode(ec);
    }
}
