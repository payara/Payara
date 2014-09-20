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
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.connectors.config.WorkSecurityMap;
import org.glassfish.connectors.config.PrincipalMap;
import org.glassfish.connectors.config.GroupMap;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.*;

import javax.inject.Inject;

/**
 * List Connector Work Security Maps
 *
 */
@Service(name="list-connector-work-security-maps")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.connector.work.security.maps")
@RestEndpoints({
    @RestEndpoint(configBean=SecurityService.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-connector-work-security-maps", 
        description="List Connector Work Security Maps")
})
public class ListConnectorWorkSecurityMaps implements AdminCommand {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ListConnectorWorkSecurityMaps.class);

    @Param(name="securitymap", optional=true)
    String securityMap;

    @Param(name="resource-adapter-name", primary=true)
    String raName;

    @Inject
    Domain domain;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final ActionReport.MessagePart mp = report.getTopMessagePart();

        try {
            boolean foundWSM = false;
            Collection<WorkSecurityMap> workSecurityMaps =
                    domain.getResources().getResources(WorkSecurityMap.class);
            for (WorkSecurityMap wsm : workSecurityMaps) {
                if (wsm.getResourceAdapterName().equals(raName)) {
                    if (securityMap == null) {
                        listWorkSecurityMap(wsm, mp);
                        foundWSM = true;
                    } else if (wsm.getName().equals(securityMap)) {
                        listWorkSecurityMap(wsm, mp);
                        foundWSM = true;
                        break;
                    }
                }
            }
            if (!foundWSM) {
                 report.setMessage(localStrings.getLocalString(
                        "list.connector.work.security.maps.workSecurityMapNotFound",
                        "Nothing to list. Either the resource adapter {0} does not exist or the" +
                                "resource adapter {0} is not associated with any work security map.", raName));
            }

        } catch (Exception e) {
            Logger.getLogger(ListConnectorWorkSecurityMaps.class.getName()).log(Level.SEVERE,
                    "list-connector-work-security-maps failed", e);
            report.setMessage(localStrings.getLocalString("" +
                    "list.connector.work.security.maps.fail",
                    "Unable to list connector work security map {0} for resource adapter {1}", securityMap, raName) + " " +
                    e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private void listWorkSecurityMap(WorkSecurityMap wsm, ActionReport.MessagePart mp) {
        List<PrincipalMap> principalList = wsm.getPrincipalMap();
        List<GroupMap> groupList = wsm.getGroupMap();

        for (PrincipalMap map : principalList) {
            final ActionReport.MessagePart part = mp.addChild();
            part.setMessage(localStrings.getLocalString(
                    "list.connector.work.security.maps.eisPrincipalAndMappedPrincipal",
                    "{0}: EIS principal={1}, mapped principal={2}",
                    wsm.getName(), map.getEisPrincipal(), map.getMappedPrincipal()));
        }

        for (GroupMap map : groupList) {
            final ActionReport.MessagePart part = mp.addChild();
            part.setMessage(localStrings.getLocalString(
                    "list.connector.work.security.maps.eisGroupAndMappedGroup",
                    "{0}: EIS group={1}, mapped group={2}",
                    wsm.getName(), map.getEisGroup(), map.getMappedGroup()));
        }
    }
}
