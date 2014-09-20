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

package org.glassfish.connectors.admin.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.connectors.config.WorkSecurityMap;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Delete Connector Work Security Map
 *
 */
@ExecuteOn(RuntimeType.ALL)
@Service(name="delete-connector-work-security-map")
@PerLookup
@I18n("delete.connector.work.security.map")
public class DeleteConnectorWorkSecurityMap implements AdminCommand {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(DeleteConnectorWorkSecurityMap.class);

    @Param(name="raname")
    private String raName;

    @Param(name="mapname", primary=true)
    private String mapName;

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

        // ensure we already have this resource
        if (!isResourceExists()) {
            report.setMessage(localStrings.getLocalString(
                    "delete.connector.work.security.map.notFound",
                    "A connector work security map named {0} for resource adapter {1} does not exist.",
                    mapName, raName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            // delete connector-work-security-map
            ConfigSupport.apply(new SingleConfigCode<Resources>() {

                Collection<WorkSecurityMap> workSecurityMaps =
                        domain.getResources().getResources(WorkSecurityMap.class);
                public Object run(Resources param) throws PropertyVetoException,
                        TransactionFailure {
                    for (WorkSecurityMap resource : workSecurityMaps) {
                        if (resource.getName().equals(mapName) &&
                                resource.getResourceAdapterName().equals(raName)) {
                            param.getResources().remove(resource);
                            break;
                        }
                    }
                    return workSecurityMaps;
                }
            }, domain.getResources());

        } catch (TransactionFailure tfe) {
            Logger.getLogger(DeleteConnectorWorkSecurityMap.class.getName()).log(Level.SEVERE,
                    "delete-connector-work-security-map failed", tfe);
            report.setMessage(localStrings.getLocalString("" +
                    "delete.connector.work.security.map.fail",
                    "Unable to delete connector work security map {0} for resource adapter {1}", mapName, raName) + " "
                    + tfe.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
            return;
        }
        
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private boolean isResourceExists() {
        for (Resource resource : domain.getResources().getResources()) {
            if (resource instanceof WorkSecurityMap) {
                if (((WorkSecurityMap) resource).getName().equals(mapName) &&
                        ((WorkSecurityMap) resource).getResourceAdapterName().equals(raName))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
