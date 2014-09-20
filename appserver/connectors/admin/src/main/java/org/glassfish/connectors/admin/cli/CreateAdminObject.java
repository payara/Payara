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

package org.glassfish.connectors.admin.cli;

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
import javax.inject.Provider;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.connectors.admin.cli.CLIConstants.AOR.*;
import static org.glassfish.connectors.admin.cli.CLIConstants.DESCRIPTION;
import static org.glassfish.connectors.admin.cli.CLIConstants.PROPERTY;
import static org.glassfish.resources.admin.cli.ResourceConstants.*;

/**
 * Create Admin Object Command
 *
 * @author Jennifer Chou, Jagadish Ramu 
 */
@TargetType(value={CommandTarget.DAS,CommandTarget.CONFIG, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE })
@ExecuteOn(RuntimeType.ALL)
@Service(name=AOR_CREATE_COMMAND_NAME)
@PerLookup
@I18n("create.admin.object")
public class CreateAdminObject implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateAdminObject.class);    

    @Param(name=AOR_RES_TYPE)
    private String resType;

    @Param(name=AOR_CLASS_NAME, optional=true)
    private String className;

    @Param(name=AOR_RA_NAME, alias="resAdapter")
    private String raName;

    @Param(name=CLIConstants.ENABLED, optional=true, defaultValue="true")
    private Boolean enabled;

    @Param(name=DESCRIPTION, optional=true)
    private String description;
    
    @Param(name=PROPERTY, optional=true, separator=':')
    private Properties properties;
    
    @Param(optional=true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Param(name=AOR_JNDI_NAME, primary=true)
    private String jndiName;
    
    @Inject
    private Domain domain;

    @Inject
    private Provider<AdminObjectManager>  adminObjectManagerProvider;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        HashMap attrList = new HashMap();
        attrList.put(RES_TYPE, resType);
        attrList.put(ADMIN_OBJECT_CLASS_NAME, className);
        attrList.put(ResourceConstants.ENABLED, enabled.toString());
        attrList.put(JNDI_NAME, jndiName);
        attrList.put(ServerTags.DESCRIPTION, description);
        attrList.put(RES_ADAPTER, raName);

        ResourceStatus rs;

        try {
            AdminObjectManager adminObjMgr = adminObjectManagerProvider.get();
            rs = adminObjMgr.create(domain.getResources(), attrList, properties, target);
        } catch(Exception e) {
            Logger.getLogger(CreateAdminObject.class.getName()).log(Level.SEVERE,
                    "Something went wrong in create-admin-object", e);
            String def = "Admin object: {0} could not be created, reason: {1}";
            report.setMessage(localStrings.getLocalString("create.admin.object.fail",
                    def, jndiName) + " " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        if (rs.getMessage() != null) {
                report.setMessage(rs.getMessage());
        }
        if (rs.getStatus() == ResourceStatus.FAILURE) {
            ec = ActionReport.ExitCode.FAILURE;
            if(rs.getMessage() == null) {
                 report.setMessage(localStrings.getLocalString("create.admin.object.fail",
                    "Admin object {0} creation failed", jndiName, ""));
            }
            if (rs.getException() != null)
                report.setFailureCause(rs.getException());
        }
        report.setActionExitCode(ec);
    }
}
