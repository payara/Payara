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

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.connectors.config.ConnectorResource;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resourcebase.resources.admin.cli.ResourceUtil;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;

/**
 * Delete Connector Resource command
 *
 * @author Jennifer Chou, Jagadish Ramu
 * 
 */
@TargetType(value={CommandTarget.DAS,CommandTarget.DOMAIN, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE })
@RestEndpoints({
        @RestEndpoint(configBean = Resources.class,
                opType = RestEndpoint.OpType.DELETE,
                path = "delete-connector-resource",
                description = "delete-connector-resource")
})

@ExecuteOn(value={RuntimeType.ALL})
@Service(name="delete-connector-resource")
@PerLookup
@I18n("delete.connector.resource")
public class DeleteConnectorResource implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteConnectorResource.class);

    @Param(optional=true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;
    
    @Param(name="connector_resource_name", primary=true)
    private String jndiName;

    @Inject
    private ResourceUtil resourceUtil;
    
    @Inject
    private Domain domain;

    @Inject
    private ServerEnvironment environment;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();

        if (jndiName == null) {
            report.setMessage(localStrings.getLocalString("delete.connector.resource.noJndiName",
                            "No JNDI name defined for connector resource."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        // ensure we already have this resource
        Resource r = ConnectorsUtil.getResourceByName(domain.getResources(), ConnectorResource.class, jndiName);
        if (r == null) {
            report.setMessage(localStrings.getLocalString("delete.connector.resource.notfound",
                    "A connector resource named {0} does not exist.", jndiName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        if ("system-all-req".equals(r.getObjectType())) {
            report.setMessage(localStrings.getLocalString("delete.connector.resource.notAllowed", 
                    "The {0} resource cannot be deleted as it is required to be configured in the system.", 
                    jndiName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        if (environment.isDas()) {

            if ("domain".equals(target)) {
                if (resourceUtil.getTargetsReferringResourceRef(jndiName).size() > 0) {
                    report.setMessage(localStrings.getLocalString("delete.connector.resource.resource-ref.exist",
                            "connector-resource [ {0} ] is referenced in an" +
                                    "instance/cluster target, Use delete-resource-ref on appropriate target",
                            jndiName));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            } else {
                if (!resourceUtil.isResourceRefInTarget(jndiName, target)) {
                    report.setMessage(localStrings.getLocalString("delete.connector.resource.no.resource-ref",
                            "connector-resource [ {0} ] is not referenced in target [ {1} ]",
                            jndiName, target));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;

                }

                if (resourceUtil.getTargetsReferringResourceRef(jndiName).size() > 1) {
                    report.setMessage(localStrings.getLocalString("delete.connector.resource.multiple.resource-refs",
                            "connector resource [ {0} ] is referenced in multiple " +
                                    "instance/cluster targets, Use delete-resource-ref on appropriate target",
                            jndiName));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }

        try {
            //delete resource-ref
            resourceUtil.deleteResourceRef(jndiName, target);

            // delete connector-resource
            if (ConfigSupport.apply(new SingleConfigCode<Resources>() {
                public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                    ConnectorResource resource = (ConnectorResource)
                            ConnectorsUtil.getResourceByName(domain.getResources(), ConnectorResource.class, jndiName );
                    return param.getResources().remove(resource);
                }
            }, domain.getResources()) == null) {
                report.setMessage(localStrings.getLocalString("delete.connector.resource.fail",
                                "Connector resource {0} delete failed ", jndiName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        } catch(TransactionFailure tfe) {
            report.setMessage(localStrings.getLocalString("delete.connector.resource.fail",
                            "Connector resource {0} delete failed ", jndiName)
                            + " " + tfe.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
        }

        report.setMessage(localStrings.getLocalString("delete.connector.resource.success",
                "Connector resource {0} deleted successfully", jndiName));
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
