/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.cli;

import com.sun.enterprise.config.serverbeans.ConnectorConnectionPool;
import com.sun.enterprise.config.serverbeans.SecurityMap;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.v3.common.ActionReporter;
import java.beans.PropertyVetoException;
import java.util.List;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author jasonlee
 */
@Service(name = "__create-principal")
@Scoped(PerLookup.class)
public class CreatePrincipalCommand implements AdminCommand {
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreatePrincipalCommand.class);

    @Inject
    protected com.sun.enterprise.config.serverbeans.Domain domain;
    
    @Param
    protected String poolName;

    @Param
    protected String mapName;

    @Param(separator = ',')
    protected String[] principals;
    
    @Inject
    protected ConnectorConnectionPool[] ccPools;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReporter report = (ActionReporter) context.getActionReport();

        SecurityMap map = getSpecifiedSecurityMapForPool(ccPools, poolName, mapName);

        if (map == null) {
            report.setMessage(localStrings.getLocalString("create.principal.invalid.map",
                    "A security map named {0} could not be found for connector connection pool {1}. Please check the" +
                    " map and pool names.", mapName, poolName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<SecurityMap>() {
                @Override
                public Object run(SecurityMap map) throws PropertyVetoException, TransactionFailure {
                    if (principals != null) {
                        for (String p : principals) {
                            map.getPrincipal().add(p);
                        }
                    }

                    return "";
                }
            }, map);
        } catch (TransactionFailure tfe) {
            Object params[] = {mapName, poolName};
            report.setMessage(localStrings.getLocalString("create.connector.security.map.fail",
                    "Unable to create connector security map {0} for connector connection pool {1} ", params) +
                    " " + tfe.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
            return;
        }

        report.setActionExitCode(ExitCode.SUCCESS);
    }

    public static SecurityMap getSpecifiedSecurityMapForPool(ConnectorConnectionPool[] ccPools, String poolName, String mapName) {
        SecurityMap securityMap = null;
        for (ConnectorConnectionPool ccp : ccPools) {
            if (ccp.getName().equals(poolName)) {
                List<SecurityMap> maps = ccp.getSecurityMap();
                for (SecurityMap map : maps) {
                    if (map.getName().equals(mapName)) {
                        securityMap = map;
                        break;
                    }
                }
                break;
            }
        }
        return securityMap;
    }
}
