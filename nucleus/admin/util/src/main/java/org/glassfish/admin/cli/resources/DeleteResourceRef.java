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

package org.glassfish.admin.cli.resources;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;

import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Delete Resource Ref Command
 *
 * @author Jennifer Chou, Jagadish Ramu 
 */
@TargetType(value={CommandTarget.CONFIG, CommandTarget.DAS, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE })
@RestEndpoints({
        @RestEndpoint(configBean = Resources.class,
                opType = RestEndpoint.OpType.DELETE,
                path = "delete-resource-ref",
                description = "delete-resource-ref")
})
@org.glassfish.api.admin.ExecuteOn(value={RuntimeType.DAS, RuntimeType.INSTANCE})
@Service(name="delete-resource-ref")
@PerLookup
@I18n("delete.resource.ref")
public class DeleteResourceRef implements AdminCommand, AdminCommandSecurity.Preauthorization {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteResourceRef.class);

    @Param(optional=true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Param(name="reference_name", primary=true)
    private String refName;

    //not needed, but mvn based test might not have initialized ConfigBeanUtilities
    @Inject
    private ConfigBeansUtilities configBeanUtilities;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private Domain domain;
    
    @Inject
    private ConfigBeansUtilities configBeansUtilities;

    private RefContainer refContainer = null;
    
    @AccessRequired.To("delete")
    private ResourceRef resourceRef = null;
    
    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        refContainer = CLIUtil.chooseRefContainer(domain, target, configBeansUtilities);
        if (refContainer != null) {
            resourceRef = getResourceRef();
        }
        
        if (resourceRef == null) {
            setResourceRefDoNotExistMessage(context.getActionReport());
        }
        return resourceRef != null;
    }
    
    private ResourceRef getResourceRef() {
        for (ResourceRef rr : refContainer.getResourceRef()) {
            if (rr.getRef().equals(refName)) {
                return rr;
            }
        }
        return null;
    }
    
    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        
        try {
            deleteResourceRef();
            if (refContainer instanceof Cluster) {

                // delete ResourceRef for all instances of Cluster
                Target tgt = habitat.getService(Target.class);
                List<Server> instances = tgt.getInstances(target);
                for (Server svr : instances) {
                    svr.deleteResourceRef(refName);
                }
            }
        } catch(Exception e) {
            setFailureMessage(report, e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        report.setMessage(localStrings.getLocalString("delete.resource.ref.success",
                "resource-ref {0} deleted successfully from target {1}.", refName, target));
    }
    
    private void deleteResourceRef() throws TransactionFailure {
        if (resourceRef != null) {
            ConfigSupport.apply(new SingleConfigCode<RefContainer>() {

                @Override
                public Object run(RefContainer param) {
                    return param.getResourceRef().remove(resourceRef);
                }
            }, refContainer);
        }
    }

    private void setResourceRefDoNotExistMessage(ActionReport report) {
        report.setMessage(localStrings.getLocalString("delete.resource.ref.doesNotExist",
                "A resource ref named {0} does not exist for target {1}.", refName, target));
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
    }

    private void setFailureMessage(ActionReport report, Exception e) {
        report.setMessage(localStrings.getLocalString("delete.resource.ref.failed",
                "Resource ref {0} deletion failed", refName));
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        report.setFailureCause(e);
    }
}
