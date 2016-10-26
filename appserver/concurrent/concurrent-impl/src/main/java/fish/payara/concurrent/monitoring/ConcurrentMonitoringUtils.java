/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2016 Payara Foundation and/or its affiliates.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.concurrent.monitoring;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.concurrent.LogFacade;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;

/**
 * Utility class for Concurrent monitoring.
 * 
 * @author Andrew Pielage
 */
public class ConcurrentMonitoringUtils
{
    private static final Logger _logger = LogFacade.getLogger();
    
    static final String NODE = "/";
    static final String SEP = "-";
    static final String APPLICATION_NODE = "applications" + NODE;
    static final String CONCURRENT_MONITORING_NODE = "thread-pool";
    static final String METHOD_NODE = NODE + "bean-methods" + NODE;

    static String registerSingleComponent(String nodeItemName, 
            Object listener) {
        String beanTreeNode = "executorService/" + nodeItemName;
        try {
            StatsProviderManager.register(CONCURRENT_MONITORING_NODE, 
                    PluginPoint.APPLICATIONS, beanTreeNode, listener);
        } catch (Exception ex) {
            _logger.log(Level.SEVERE, "[**ConcurrentMonitoringUtils**] Could not "
                    + "register listener for " + nodeItemName,
                    ex);
            return null;
        }
        return beanTreeNode;
    }

    public static String stringify(Method m) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("==> Converting method to String: " + m);
        }
        StringBuffer sb = new StringBuffer();
        sb.append(m.getName());
        Class[] args = m.getParameterTypes();
        for (Class c : args) {
            sb.append(SEP).append(c.getName().replaceAll("_", "\\."));
        }
        String result = sb.toString().replaceAll("\\.", "\\\\.");
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("==> Converted method String: " + result);
        }
        return result;
    }

    static String getBeanNode(String appName, String moduleName, String beanName) {
        StringBuffer sb = new StringBuffer();
        /** sb.append(APPLICATION_NODE); **/

        if (appName != null) {
            sb.append(appName).append(NODE);
        }
        sb.append(moduleName).append(NODE).append(beanName);

        String beanSubTreeNode = sb.toString().replaceAll("\\.", "\\\\.").
               replaceAll("_jar", "\\\\.jar").replaceAll("_war", "\\\\.war");

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("BEAN NODE NAME: " + beanSubTreeNode);
        }
        return beanSubTreeNode;
    }

    public static String getInvokerId(String appName, String modName, String beanName) {
        if (appName == null) {
            return "_" + modName + "_" + beanName;
        }

        return "_" + appName + "_" + modName + "_" + beanName;
    }


    public static String getDetailedLoggingName(String appName, String modName, String beanName) {
        if (appName == null) {
            return "modName=" + modName + "; beanName=" + beanName;
        }

        return "appName=" + appName + "; modName=" + modName + "; beanName=" + beanName;
    }

    public static String getLoggingName(String appName, String modName, String beanName) {
        if (appName == null) {
            return modName + ":" + beanName;
        }

        return appName + ":" + modName + ":" + beanName;
    }
}
