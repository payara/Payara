/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.source.extension;

import static java.lang.Boolean.FALSE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

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
import fish.payara.nucleus.microprofile.config.spi.ConfigSourceConfiguration;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;

/**
 * The base admin command to get the configuration of a specified config source.
 * Extend this class to fetch custom config source configuration options.
 * 
 * @author mertcaliskan
 * @author Matthew Gill
 */
public abstract class BaseGetConfigSourceConfigurationCommand<C extends ConfigSourceConfiguration> implements AdminCommand {

    @Inject
    private Target targetUtil;

    @Inject
    protected ServiceLocator habitat;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Override
    public void execute(AdminCommandContext context) {
        // Get the command report
        final ActionReport report = context.getActionReport();

        // Get the target configuration
        final Config targetConfig = targetUtil.getConfig(target);
        if (targetConfig == null) {
            report.setMessage("No such config named: " + target);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        // Initialise the extra properties
        Properties extraProperties = report.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            report.setExtraProperties(extraProperties);
        }

        final MicroprofileConfigConfiguration mpConfigConfiguration = targetConfig.getExtensionByType(MicroprofileConfigConfiguration.class);
        Class<C> configSourceConfigurationClass = ConfigSourceExtensions.getConfigurationClass(getClass());
        
        C c = mpConfigConfiguration.getConfigSourceConfigurationByType(configSourceConfigurationClass);

        Properties extraProps = new Properties();
        extraProps.put("configSourceConfiguration", getConfigSourceProperties(c));
        if (c == null) {
            report.setMessage("ConfigSource Configuration is not defined");
        } else {
            report.setMessage(listConfiguration(c));
        }

        report.setExtraProperties(extraProps);
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    /**
     * @param configuration the configuration to print
     * @return A column formatted string representing the configuration
     * @see #getConfigSourceConfiguration(PayaraConfigSourceConfiguration)
     */
    protected String listConfiguration(C configuration) {
        Map<String, Object> configMap = getConfigSourceConfiguration(configuration);

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
    protected Map<String, Object> getConfigSourceConfiguration(C configuration) {
        Map<String, Object> map = new LinkedHashMap<>(2);

        if (configuration != null) {
            map.put("Enabled", configuration.getEnabled());
        } else {
            map.put("Enabled", FALSE.toString());
        }

        return map;
    }

    /**
     * Get a camelcase version of
     * {@link #getConfigSourceConfiguration(ConfigSourceConfiguration)}. By default
     * will call {@link #getConfigSourceConfiguration(ConfigSourceConfiguration)} and
     * convert the keys to camel casing. Override if the result of this method is
     * wrong.
     * 
     * @param configuration the configuration to get properties from
     * @return a map from camelcase attribute names to their values
     */
    protected Map<String, Object> getConfigSourceProperties(C configuration) {
        Map<String, Object> configMap = getConfigSourceConfiguration(configuration);
        Map<String, Object> result = new LinkedHashMap<>(2);
        Iterator<Entry<String, Object>> iterator = configMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Object> entry = iterator.next();
            // TODO: This utility needs moving into generic Extension utilities
            result.put(NotifierUtils.convertToCamelCase(entry.getKey()), entry.getValue());
        }
        return result;
    }
}
