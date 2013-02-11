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

package org.glassfish.resources.admin.cli;

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
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.resources.admin.cli.ResourceConstants.JNDI_NAME;


/**
 * Create Custom Resource
 *
 */
@TargetType(value={CommandTarget.DAS,CommandTarget.DOMAIN, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE })
@RestEndpoints({
        @RestEndpoint(configBean=Resources.class,
                opType=RestEndpoint.OpType.POST,
                path="create-custom-resource",
                description="create-custom-resource")
})
@org.glassfish.api.admin.ExecuteOn(RuntimeType.ALL)
@Service(name="create-custom-resource")
@PerLookup
@I18n("create.custom.resource")
public class CreateCustomResource implements AdminCommand {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(CreateCustomResource.class);

    @Param(name = "restype")
    private String resType;

    @Param(name = "factoryclass")
    private String factoryClass;

    @Param(optional = true, defaultValue = "true")
    private Boolean enabled;

    @Param(optional = true)
    private String description;

    @Param(name = "property", optional = true, separator = ':')
    private Properties properties;

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Param(name = "jndi_name", primary = true)
    private String jndiName;

    @Inject
    private Domain domain;

    @Inject
    private org.glassfish.resources.admin.cli.CustomResourceManager customResMgr;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        HashMap attrList = new HashMap();
        attrList.put("factory-class", factoryClass);
        attrList.put("res-type", resType);
        attrList.put(ResourceConstants.ENABLED, enabled.toString());
        attrList.put(JNDI_NAME, jndiName);
        attrList.put(ServerTags.DESCRIPTION, description);

        ResourceStatus rs;

        try {
            rs = customResMgr.create(domain.getResources(), attrList, properties, target);
        } catch(Exception e) {
            Logger.getLogger(CreateCustomResource.class.getName()).log(Level.SEVERE,
                    "Unable to create custom resource " + jndiName, e);
            String def = "Custom resource: {0} could not be created, reason: {1}";
            report.setMessage(localStrings.getLocalString("create.custom.resource.fail",
                    def, jndiName) + " " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        if (rs.getStatus() == ResourceStatus.FAILURE) {
            ec = ActionReport.ExitCode.FAILURE;
            if (rs.getMessage() == null) {
                 report.setMessage(localStrings.getLocalString("create.custom.resource.fail",
                    "Custom resource {0} creation failed", jndiName, ""));
            }
            if (rs.getException() != null)
                report.setFailureCause(rs.getException());
        }
        if(rs.getMessage() != null){
            report.setMessage(rs.getMessage());
        }
        report.setActionExitCode(ec);
    }
}
