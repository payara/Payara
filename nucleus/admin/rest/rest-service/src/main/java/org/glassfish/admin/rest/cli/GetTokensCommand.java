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
// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package org.glassfish.admin.rest.cli;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.admin.report.ActionReporter;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.PropertyResolver;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 *
 * @author jasonlee
 */
@Service(name="__resolve-tokens")
@PerLookup
@TargetType(value={CommandTarget.DAS,CommandTarget.DOMAIN, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTERED_INSTANCE })
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.GET, 
        path="resolve-tokens", 
        description="Resolve Tokens",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="resolve-tokens", 
        description="Resolve Tokens",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.GET, 
        path="resolve-tokens", 
        description="Resolve Tokens",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Config.class,
        opType=RestEndpoint.OpType.GET, 
        path="resolve-tokens", 
        description="Resolve Tokens",
        params={
            @RestParam(name="target", value="$parent")
        })
})
public class GetTokensCommand implements AdminCommand {
    @Inject
    private Domain domain;

    @Inject
    private ServiceLocator habitat;
    
    @Param(separator=',', primary=true)
    String[] tokens;
    
    @Param(name="check-system-properties", defaultValue="false", optional=true)
    boolean checkSystemProperties;

    @Param(optional=true, defaultValue=SystemPropertyConstants.DAS_SERVER_NAME)
    String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReporter report = (ActionReporter) context.getActionReport();
        PropertyResolver resolver = new PropertyResolver(domain, target);
        
        String sep = "";
        String eol = System.getProperty("line.separator");
        StringBuilder output = new StringBuilder();
        Map<String, String> values = new TreeMap<String, String>();
        Properties properties = new Properties();
        properties.put("tokens", values);
        
        for (String token : tokens) {
            String value = resolver.getPropertyValue(token);
            if ((value == null) && (checkSystemProperties)) {
                value = System.getProperty(token);
            }
            output.append(sep).append(token).append(" = ").append(value);
            sep = eol;
            values.put(token, value);
        }

        report.setMessage(output.toString());
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        report.setExtraProperties(properties);
        
    }
}
