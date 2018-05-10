/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.opentracing.admin;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.opentracing.OpenTracingServiceConfiguration;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Service(name = "set-opentracing-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, 
    CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = OpenTracingServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-opentracing-configuration",
            description = "Sets the OpenTracing Service Configuration")
})
public class SetOpenTracingServiceConfiguration implements AdminCommand {

    private static final Logger logger = Logger.getLogger(SetOpenTracingServiceConfiguration.class.getName());
    
    @Inject
    private Target targetUtil;
    
    @Param(optional = true, alias = "tracerfactory")
    private String tracerFactoryClassName;
    
    @Param(optional = true, defaultValue = "server-config")
    private String target;
    
    @Override
    public void execute(AdminCommandContext context) {
        Config targetConfig = targetUtil.getConfig(target);
        OpenTracingServiceConfiguration openTracingServiceConfiguration = targetConfig
                .getExtensionByType(OpenTracingServiceConfiguration.class);
        
        try {
            ConfigSupport.apply((OpenTracingServiceConfiguration configProxy) -> {
                if (tracerFactoryClassName != null) {
                    configProxy.setTracerFactoryClass(tracerFactoryClassName);
                }
                
                return null;
            }, openTracingServiceConfiguration);
        } catch (TransactionFailure ex) {
            context.getActionReport().failure(logger, "Failed to update Fault Tolerance configuration", ex);
        }
    }
    
}
