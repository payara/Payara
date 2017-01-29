/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.notification.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;

import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author mertcaliskan
 */
public abstract class BaseGetNotifierConfiguration<NC> implements AdminCommand {

    @Inject
    private Target targetUtil;

    @Inject
    ServiceLocator habitat;

    Class<NC> notifierConfigurationClass;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Override
    public void execute(AdminCommandContext context) {
        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        ActionReport mainActionReport = context.getActionReport();

        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        notifierConfigurationClass = (Class<NC>) genericSuperclass.getActualTypeArguments()[0];

        NotificationServiceConfiguration configuration = config.getExtensionByType(NotificationServiceConfiguration.class);
        NC nc = configuration.getNotifierConfigurationByType(notifierConfigurationClass);

        String message;
        Properties extraProps = new Properties();
        Map<String, Object> configMap;
        if (nc == null) {
            message = "Notifier Configuration is not defined";
            configMap = new HashMap<>();
        }
        else {
            message = listConfiguration(nc);
            configMap = getNotifierConfiguration(nc);
        }
        mainActionReport.setMessage(message);
        extraProps.put("notifierConfiguration", configMap);
        mainActionReport.setExtraProperties(extraProps);
        
        mainActionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    protected abstract String listConfiguration(NC nc);
    
    protected abstract Map<String, Object> getNotifierConfiguration(NC nc);
}
