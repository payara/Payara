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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.Server;
import org.glassfish.api.admin.*;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Dom;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;

import java.util.Map;
import java.util.HashMap;

import java.util.*;

import com.sun.enterprise.v3.common.PropsFileActionReporter;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.flashlight.MonitoringRuntimeDataRegistry;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.admin.AccessRequired.AccessCheck;

/**
 * User: Jerome Dochez
 * Date: Jul 12, 2008
 * Time: 1:27:53 AM
 */
@Service(name="list")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
public class ListCommand extends V2DottedNameSupport implements AdminCommand,
        AdminCommandSecurity.Preauthorization, AdminCommandSecurity.AccessCheckProvider {

    @Inject
    private MonitoringReporter mr;

    @Inject
    Domain domain;

    @Inject
    ServerEnvironment serverEnv;

    @Inject
    Target targetService;

    @Inject
    ServiceLocator habitat;

    //How to define short option name?
    @Param(name="MoniTor", optional=true, defaultValue="false", shortName="m", alias="Mon")
    Boolean monitor;

    @Param(primary = true)
    String pattern="";

    @Inject @Optional
    private MonitoringRuntimeDataRegistry mrdr;
    
    private Map<Dom, String> matchingNodes;
    
    private TreeNode[] parentNodes;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        if (monitor) {
            return preAuthorizationForMonitoring(context);
        } else {
            return preAuthorizationForNonMonitoring(context);
        }
    }
    
    private boolean preAuthorizationForMonitoring(final AdminCommandContext context) {
        mr.prepareList(context, pattern);
        return true;
    }

    private boolean preAuthorizationForNonMonitoring(final AdminCommandContext context) {
        // first let's get the parent for this pattern.
        parentNodes = getAliasedParent(domain, pattern);
        Map<Dom, String> dottedNames =  new HashMap<Dom, String>();
        for (TreeNode parentNode : parentNodes) {
               dottedNames.putAll(getAllDottedNodes(parentNode.node));
        }
        // reset the pattern.
        pattern = parentNodes[0].relativeName;

        matchingNodes = getMatchingNodes(dottedNames, pattern);
        if (matchingNodes.isEmpty() && pattern.lastIndexOf('.')!=-1) {
            // it's possible the user is just looking for an attribute, let's remove the
            // last element from the pattern.
            matchingNodes = getMatchingNodes(dottedNames, pattern.substring(0, pattern.lastIndexOf(".")));
        }
        return true;
    }
    
    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        if (monitor) {
            return getAccessChecksForMonitoring();
        } else {
            return getAccessChecksForNonMonitoring();
        }
    }
    
    private Collection<? extends AccessCheck> getAccessChecksForMonitoring() {
        return mr.getAccessChecksForList();
    }
    
    private Collection<? extends AccessCheck> getAccessChecksForNonMonitoring() {
        final Collection<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        for (Map.Entry<Dom,String> entry : matchingNodes.entrySet()) {
            accessChecks.add(new AccessCheck(AccessRequired.Util.resourceNameFromDom((Dom)entry.getKey()), "read"));
        }
        return accessChecks;
    }
    
    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();
        
        /* Issue 5918 Used in ManifestManager to keep output sorted */
        try {
            PropsFileActionReporter reporter = (PropsFileActionReporter) report;
            reporter.useMainChildrenAttribute(true);
        } catch(ClassCastException e) { 
            // ignore, this is not a manifest output
        }

        if (monitor) {
            listMonitorElements(context);
            return;
        }
        
        List<Map.Entry> matchingNodesSorted = sortNodesByDottedName(matchingNodes);
        for (Map.Entry<Dom, String> node : matchingNodesSorted) {
            ActionReport.MessagePart part = report.getTopMessagePart().addChild();
            part.setChildrenType("DottedName");
            if (parentNodes[0].name.isEmpty()) {
                part.setMessage(node.getValue());
            } else {
                part.setMessage(parentNodes[0].name + "." + node.getValue());
            }
        }
    }
    
    private void listMonitorElements(AdminCommandContext ctxt) {
        mr.execute();
    }

    public void callInstance(ActionReport report, AdminCommandContext context, String targetName) {
        try {
            ParameterMap paramMap = new ParameterMap();
            paramMap.set("MoniTor", "true");
            paramMap.set("DEFAULT", pattern);
            List<Server> targetList = targetService.getInstances(targetName);
            ClusterOperationUtil.replicateCommand("list", FailurePolicy.Error, FailurePolicy.Warn, 
                    FailurePolicy.Ignore, targetList, context, paramMap, habitat);
        } catch(Exception ex) {
            report.setActionExitCode(ExitCode.FAILURE);
            report.setMessage("Failure while trying get details from instance " + targetName);
        }
    }
}
