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

package org.glassfish.loadbalancer.admin.cli;

import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

import java.beans.PropertyVetoException;

import org.jvnet.hk2.config.*;
import org.glassfish.api.ActionReport;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.config.serverbeans.Cluster;

import org.glassfish.api.admin.*;
import org.glassfish.api.ActionReport.ExitCode;

import javax.inject.Inject;

/**
 * Base class for all the LB commands
 * @author Yamini K B
 */
public class LBCommandsBase {

    @Inject
    Domain domain;
    
    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(LBCommandsBase.class);
    
    Map<String,Integer> getInstanceWeightsMap(String weights) throws CommandException
    {
        HashMap<String,Integer> map = new HashMap();
        StringTokenizer st = new StringTokenizer(weights, ":");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            String insName = null;
            String value = null;
            insName = token.substring(0, token.indexOf("="));
            value = token.substring(token.indexOf("=") + 1);
            Integer weightInt;
            try
            {
                weightInt = Integer.valueOf(value);
            }
            catch (NumberFormatException nfe)
            {
                throw new CommandException("Invalid weight value");
            }
            map.put(insName, weightInt);
        }
        return map;
    }

    void updateLBForCluster(ActionReport report, String clusterName, String value, String timeout) {
        Cluster c = domain.getClusterNamed(clusterName);
        if ( c == null ) {            
            report.setMessage("Cluster not defined");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        for (ServerRef sRef:c.getServerRef()) {
            try {
                updateLbEnabled(sRef, value, timeout);
            } catch(TransactionFailure ex) {
                report.setMessage("Failed to update lb-enabled");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
    }

    ServerRef getServerRefFromCluster(ActionReport report, String target) {
        // check if this server is part of cluster
        Cluster c = domain.getClusterForInstance(target);

        if (c == null) {
            report.setMessage("ServerNotDefined");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return null;
        } else {
            return c.getServerRefByRef(target);
        }
    }

    void updateLbEnabled(final ServerRef ref, final String v, final String tOut)
                    throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<ServerRef>() {
                @Override
                public Object run(ServerRef param) throws PropertyVetoException, TransactionFailure {
                    param.setLbEnabled(v);
                    if(v.equals("false") && tOut != null) {
                        param.setDisableTimeoutInMinutes(tOut);
                    }
                    return Boolean.TRUE;
                }
        }, ref);
    }

    void checkCommandStatus(AdminCommandContext context) throws CommandException {
        if(context.getActionReport().getActionExitCode() != ExitCode.SUCCESS) {
            throw new CommandException(context.getActionReport().getMessage());
        }
    }
}
