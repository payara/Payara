/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.enterprise.config;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.StringUtils;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * Returns the translated value of a property on a local instance
 * @author jonathan coustick
 * @see TranslatedConfigView
 */
@Service(name = "_get-translated-config-value") 
@PerLookup
@RestEndpoints({
    @RestEndpoint(configBean = Config.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-config-ordinal",
            description = "Gets the Ordinal of a builtin Config Source")
})
public class GetTranslatedConfigValue implements AdminCommand {
    
    @Param(name="propertyName")
    String propertyName;
    
    @Param
    String target;
    
    @Inject
    ServerEnvironment env;

    @Override
    public void execute(AdminCommandContext context) {
        if (StringUtils.ok(target) && !target.equals(env.getInstanceName())) {
            return;
        }
        ActionReport report = context.getActionReport();
        report.setMessage(TranslatedConfigView.expandConfigValue(propertyName));
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
    
}
