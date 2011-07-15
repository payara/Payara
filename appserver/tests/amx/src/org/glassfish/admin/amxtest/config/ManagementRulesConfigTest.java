/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.config.ActionConfig;
import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.EventConfig;
import com.sun.appserv.management.config.EventTypeValues;
import com.sun.appserv.management.config.ManagementRuleConfig;
import com.sun.appserv.management.config.ManagementRulesConfig;
import static com.sun.appserv.management.config.ManagementRulesConfig.EVENT_LEVEL_KEY;
import static com.sun.appserv.management.config.ManagementRulesConfig.EVENT_LOG_ENABLED_KEY;
import com.sun.appserv.management.util.misc.StringUtil;
import org.glassfish.admin.amxtest.AMXTestBase;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 */
public final class ManagementRulesConfigTest
        extends AMXTestBase {
    //private static final String ACTION_MBEAN_NAME =
    //"user:type=rule-action,name=" + CustomMBeanConfigTest.getDefaultInstanceName();
    private static final String ACTION_MBEAN_NAME = "com.foo.Bar";


    private Properties
    getDummyProperties() {
        final Properties props = new Properties();

        props.put("Dummy1", "Dummy1-value");
        props.put("Dummy2", "Dummy2-value");

        return props;
    }

    private Map<String, String>
    getOptional() {
        final Map<String, String> optional = new HashMap<String, String>();

        optional.put(EVENT_LOG_ENABLED_KEY, "true");
        optional.put(EVENT_LEVEL_KEY, "INFO");

        return optional;
    }

    public ManagementRulesConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getConfigConfig());
        }
    }

    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("ManagementRulesConfigTest");
    }

    public static ManagementRuleConfig
    ensureDefaultInstance(final ConfigConfig config) {
        final ManagementRulesConfig rules = getManagementRulesConfig(config);

        ManagementRuleConfig result =
                rules.getManagementRuleConfigMap().get(getDefaultInstanceName());

        if (result == null) {
            result = createRule(rules, getDefaultInstanceName(), EventTypeValues.TRACE, ACTION_MBEAN_NAME, null, null);
        }

        return result;
    }

    private static ManagementRulesConfig
    getManagementRulesConfig(final ConfigConfig config) {
        return config.getManagementRulesConfig();
    }


    private ManagementRulesConfig
    getManagementRulesConfig() {
        return getManagementRulesConfig(getConfigConfig());
    }

    public void
    testGet() {
        final ManagementRulesConfig rulesConfig = getManagementRulesConfig();

        if (rulesConfig != null) {
            final Map<String, ManagementRuleConfig> ruleConfigs =
                    rulesConfig.getManagementRuleConfigMap();
            assert (ruleConfigs != null);
        } else {
            warning("testGet: ManagementRulesConfig is null...skipping test");
        }
    }


    private String
    createName(final int i) {
        return "rule" + i;
    }

    private static ManagementRuleConfig
    createRule(
            final ManagementRulesConfig rules,
            final String name,
            final String eventType,
            final String actionMBeanName,
            final Properties props,
            final Map<String, String> optional) {
        final ManagementRuleConfig rule =
                rules.createManagementRuleConfig(
                        name, eventType, actionMBeanName, props, optional);

        return rule;
    }

    private void
    removeRule(final String name) {
        getManagementRulesConfig().removeManagementRuleConfig(name);
    }

    private void
    testEventConfig(final EventConfig eventConfig) {
        assert (eventConfig != null);

        final String eventType = eventConfig.getType();
        eventConfig.setType(eventType);
        eventConfig.setDescription("test description");
    }

    private void
    testRuleConfig(final ManagementRuleConfig ruleConfig) {
        final EventConfig eventConfig = ruleConfig.getEventConfig();
        assert (eventConfig != null);
        testEventConfig(eventConfig);

        final ActionConfig actionConfig = ruleConfig.getActionConfig();
        if (actionConfig != null) {
            final String mbeanName = actionConfig.getActionMBeanName();
            actionConfig.setActionMBeanName(mbeanName);
        }
    }


    public ManagementRuleConfig
    createAndTestRule(final int id)
            throws Exception {
        final ManagementRulesConfig rules = getManagementRulesConfig();

        final String name = createName(id);

        if (rules.getManagementRuleConfigMap().get(name) != null) {
            removeRule(name);
            warning("Removed left over ManagementRuleConfig: " + StringUtil.quote(name));
        }

        String actionMBeanName = ((id % 2) == 0) ? ACTION_MBEAN_NAME : null;

        ManagementRuleConfig ruleConfig =
                createRule(rules, name, EventTypeValues.TRACE, actionMBeanName, getDummyProperties(), getOptional());

        testRuleConfig(ruleConfig);
        ActionConfig actionConfig = ruleConfig.getActionConfig();
        if (actionConfig == null) {
            actionConfig = ruleConfig.createActionConfig(ACTION_MBEAN_NAME);
            testRuleConfig(ruleConfig);
        }

        try {
            ruleConfig.createActionConfig(ACTION_MBEAN_NAME);
        }
        catch (Exception e) {
            // good, we expect to be here
        }

        try {
            ruleConfig.createActionConfig(null);
        }
        catch (Exception e) {
            // good, we expect to be here
        }

        return ruleConfig;
    }

    public void
    testCreateRule()
            throws Exception {
        final ManagementRulesConfig rulesConfig = getManagementRulesConfig();
        if (rulesConfig == null) {
            warning("testCreateRule: ManagementRulesConfig is null...skipping test");
            return;
        }

        final int NUM = 6;
        final ManagementRuleConfig[] ruleConfigs = new ManagementRuleConfig[NUM];

        try {
            for (int i = 0; i < NUM; ++i) {
                ruleConfigs[i] = createAndTestRule(i);
            }
        }
        finally {
            for (int i = 0; i < NUM; ++i) {
                if (ruleConfigs[i] != null) {
                    removeRule(ruleConfigs[i].getName());
                }
            }
        }
    }


}



























