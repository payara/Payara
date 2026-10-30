/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2024] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.internal.notification.admin;

import static java.lang.Boolean.FALSE;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import fish.payara.internal.notification.EventLevel;
import jakarta.inject.Inject;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.SystemPropertyConstants;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;

import fish.payara.internal.notification.NotifierUtils;
import fish.payara.internal.notification.PayaraNotifierConfiguration;

/**
 * The base admin command to get the configuration of a specified notifier.
 * Extend this class to fetch custom notifier configuration options.
 * 
 * @author mertcaliskan
 * @author Matthew Gill
 */
public abstract class BaseGetNotifierConfigurationCommand<C extends PayaraNotifierConfiguration> implements AdminCommand {

    @Inject
    private Target targetUtil;

    @Inject
    protected ServiceLocator habitat;

    private Class<C> notifierConfigurationClass;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

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
        notifierConfigurationClass = (Class<C>) genericSuperclass.getActualTypeArguments()[0];

        NotificationServiceConfiguration configuration = config.getExtensionByType(NotificationServiceConfiguration.class);
        C nc = configuration.getNotifierConfigurationByType(notifierConfigurationClass);

        String message;
        Properties extraProps = new Properties();
        Map<String, Object> configMap = getNotifierProperties(nc);

        if (nc == null) {
            message = "Notifier Configuration is not defined";
        } else {
            message = listConfiguration(nc);
        }
        mainActionReport.setMessage(message);
        extraProps.put("notifierConfiguration", configMap);
        mainActionReport.setExtraProperties(extraProps);
        
        mainActionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    /**
     * @param configuration the configuration to print
     * @return A column formatted string representing the configuration
     * @see #getNotifierConfiguration(PayaraNotifierConfiguration)
     */
    protected String listConfiguration(C configuration) {
        Map<String, Object> configMap = getNotifierConfiguration(configuration);

        Iterator<Entry<String, Object>> configIterator = configMap.entrySet().iterator();
        List<String> headers = new ArrayList<>(2);
        List<Object> values = new ArrayList<>(2);

        while (configIterator.hasNext()) {
            Entry<String, Object> entry = configIterator.next();

            headers.add(entry.getKey());
            values.add(entry.getValue());
        }

        ColumnFormatter columnFormatter = new ColumnFormatter(headers.toArray(new String[0]));
        columnFormatter.addRow(values.toArray());

        return columnFormatter.toString();
    }

    /**
     * @param configuration the configuration to get properties from
     * @return a map from user readable attribute names to their values
     */
    protected Map<String, Object> getNotifierConfiguration(C configuration) {
        Map<String, Object> map = new LinkedHashMap<>(2);

        if (configuration != null) {
            map.put("Enabled", configuration.getEnabled());
            map.put("Filter", configuration.getFilter());
        } else {
            map.put("Enabled", FALSE.toString());
            map.put("Filter", EventLevel.WARNING.toString());
        }

        return map;
    }

    /**
     * Get a camelcase version of
     * {@link #getNotifierConfiguration(PayaraNotifierConfiguration)}. By default
     * will call {@link #getNotifierConfiguration(PayaraNotifierConfiguration)} and
     * convert the keys to camel casing. Override if the result of this method is
     * wrong.
     * 
     * @param configuration the configuration to get properties from
     * @return a map from camelcase attribute names to their values
     */
    protected Map<String, Object> getNotifierProperties(C configuration) {
        Map<String, Object> configMap = getNotifierConfiguration(configuration);
        Map<String, Object> result = new LinkedHashMap<>(2);
        Iterator<Entry<String, Object>> iterator = configMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Object> entry = iterator.next();
            result.put(NotifierUtils.convertToCamelCase(entry.getKey()), entry.getValue());
        }
        return result;
    }
}
