/*
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.asadmin.recorder.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.asadmin.recorder.AsadminRecorderConfiguration;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "enable-asadmin-recorder")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("enable.asadmin.recorder")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "enable-asadmin-recorder",
            description = "Enables the asadmin command recorder service")
})
public class EnableAsadminRecorder implements AdminCommand {
    @Inject
    AsadminRecorderConfiguration asadminRecorderConfiguration;
    
    @Override
    public void execute(AdminCommandContext context) {
        try {
            ConfigSupport.apply(new 
                    SingleConfigCode<AsadminRecorderConfiguration>() {
                public Object run(AsadminRecorderConfiguration 
                        asadminRecorderConfigurationProxy) 
                        throws PropertyVetoException, TransactionFailure {
                    
                    if (Boolean.parseBoolean(asadminRecorderConfiguration.isEnabled())) {
                        Logger.getLogger(EnableAsadminRecorder.class.getName())
                                .log(Level.INFO, 
                                        "Asadmin Recorder already enabled");                       
                    } else {
                        asadminRecorderConfigurationProxy.setEnabled(true);
                        Logger.getLogger(EnableAsadminRecorder.class.getName())
                                .log(Level.INFO, 
                                        "Asadmin Recorder enabled");
                    }
                    
                    return null;
                }
            }, asadminRecorderConfiguration);          
        } catch (TransactionFailure ex) {
            Logger.getLogger(EnableAsadminRecorder.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }
    
}
