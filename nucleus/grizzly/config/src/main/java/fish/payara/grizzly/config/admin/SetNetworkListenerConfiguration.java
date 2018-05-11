/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.grizzly.config.admin;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author jonathan coustick
 * @since 4.1.2.182
 */
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@Service(name = "set-network-listener-configuration")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("set-network-listener-configuration")
@RestEndpoints({
    @RestEndpoint(configBean = NetworkListener.class,
            opType = RestEndpoint.OpType.POST,
            description = "Configures a network listener")
})
public class SetNetworkListenerConfiguration implements AdminCommand {

    private Logger logger = Grizzly.logger(SetNetworkListenerConfiguration.class);
    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(SetNetworkListenerConfiguration.class);

    //Parameters
    @Param(name = "listener-name", primary = true)
    private String listenerName;

    @Param(name = "address", optional = true)
    private String address;

    @Param(name = "listenerport", optional = true)
    private Integer listenerport;

    @Param(name = "threadpool", optional = true)
    private String threadpool;

    @Param(name = "protocol", optional = true)
    private String protocol;

    @Param(name = "transport", optional = true)
    private String transport;

    @Param(name = "jkenabled", optional = true)
    private Boolean jkEnabled;

    @Param(name = "enabled", optional = true)
    private Boolean enabled;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Inject
    NetworkConfig config;
    

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();
        NetworkListener listener = config.getNetworkListener(listenerName);

        if (!validate(actionReport)){
            return;
        }
        
        listener.setEnabled(enabled.toString());
        listener.setAddress(address);
        listener.setPort(listenerport.toString());
        listener.setProtocol(protocol);
        listener.setThreadPool(threadpool);
        listener.setTransport(transport);
        
        
        

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean validate(ActionReport report) {
        config.getNetworkListener(listenerName);
        if (config.getNetworkListener(listenerName) == null) {
            report.setMessage(strings.getLocalString("snlc.unknown", "Unknown network listener: {0}", listenerName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }

        if (listenerport < 1 || listenerport > 65535){
            report.setMessage(strings.getLocalString("snlc.invalid.port",
                    "Invalid listener port - port must be between 1 and 65535. If port is below 1024, then Payara must have superuser permissions"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        return true;
    }

}
