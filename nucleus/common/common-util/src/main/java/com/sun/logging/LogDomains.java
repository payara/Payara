/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016] [Payara Foundation]

package com.sun.logging;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Class LogDomains
 */
public class LogDomains {

    /**
     * DOMAIN_ROOT the prefix for the logger name. This is public only so it can
     * be accessed w/in the ias package space.
     */
    public static final String DOMAIN_ROOT = "javax.";

    /**
     * Upgrade logger name.
     */
    public static final String UPGRADE_LOGGER = "upgradeLogger";

    /**
     * PACKAGE_ROOT the prefix for the packages where logger resource bundles
     * reside. This is public only so it can be accessed w/in the ias package
     * space.
     */
    public static final String PACKAGE_ROOT = "com.sun.logging.";

    /**
     * RESOURCE_BUNDLE the name of the logging resource bundles.
     */
    public static final String RESOURCE_BUNDLE = "LogStrings";

    /**
     * Field
     */
    public static final String STD_LOGGER = DOMAIN_ROOT + "enterprise.system.std";

    /**
     * Field
     */
    public static final String TOOLS_LOGGER = DOMAIN_ROOT + "enterprise.system.tools";

    /**
     * Field
     */
    public static final String EJB_LOGGER = DOMAIN_ROOT + "enterprise.system.container.ejb";

    /**
     * JavaMail Logger
     */
    public static final String JAVAMAIL_LOGGER = DOMAIN_ROOT + "enterprise.resource.javamail";

    /**
     * IIOP Logger public static final String IIOP_LOGGER = DOMAIN_ROOT +
     * "enterprise.resource.iiop";
     */

    /**
     * JMS Logger
     */
    public static final String JMS_LOGGER = DOMAIN_ROOT + "enterprise.resource.jms";

    /**
     * Field
     */
    public static final String WEB_LOGGER = DOMAIN_ROOT + "enterprise.system.container.web";

    /**
     * Field
     */
    public static final String CMP_LOGGER = DOMAIN_ROOT + "enterprise.system.container.cmp";

    /**
     * Field
     */
    public static final String JDO_LOGGER = DOMAIN_ROOT + "enterprise.resource.jdo";

    /**
     * Field
     */
    public static final String ACC_LOGGER = DOMAIN_ROOT + "enterprise.system.container.appclient";

    /**
     * Field
     */
    public static final String MDB_LOGGER = DOMAIN_ROOT + "enterprise.system.container.ejb.mdb";

    /**
     * Field
     */
    public static final String SECURITY_LOGGER = DOMAIN_ROOT + "enterprise.system.core.security";

    /**
     * Field
     */
    public static final String SECURITY_SSL_LOGGER = DOMAIN_ROOT + "enterprise.system.ssl.security";

    /**
     * Field
     */
    public static final String TRANSACTION_LOGGER = DOMAIN_ROOT + "enterprise.system.core.transaction";

    /**
     * Field
     */
    public static final String CORBA_LOGGER = DOMAIN_ROOT + "enterprise.resource.corba";

    /**
     * Field
     */
    // START OF IASRI 4660742
    /**
     * Field
     */
    public static final String UTIL_LOGGER = DOMAIN_ROOT + "enterprise.system.util";
    /**
     * Field
     */
    public static final String NAMING_LOGGER = DOMAIN_ROOT + "enterprise.system.core.naming";

    /**
     * Field
     */
    public static final String JNDI_LOGGER = DOMAIN_ROOT + "enterprise.system.core.naming";
    /**
     * Field
     */
    public static final String ACTIVATION_LOGGER = DOMAIN_ROOT + "enterprise.system.activation";
    /**
     * Field
     */
    public static final String JTA_LOGGER = DOMAIN_ROOT + "enterprise.resource.jta";

    /**
     * Resource Logger
     */
    public static final String RSR_LOGGER = DOMAIN_ROOT + "enterprise.resource.resourceadapter";
    // END OF IASRI 4660742

    /**
     * Deployment Logger
     */
    public static final String DPL_LOGGER = DOMAIN_ROOT + "enterprise.system.tools.deployment";

    /**
     * Deployment audit logger
     */
    public static final String DPLAUDIT_LOGGER = DOMAIN_ROOT + "enterprise.system.tools.deployment.audit";

    /**
     * Field
     */
    public static final String DIAGNOSTICS_LOGGER = DOMAIN_ROOT + "enterprise.system.tools.diagnostics";

    /**
     * JAXRPC Logger
     */
    public static final String JAXRPC_LOGGER = DOMAIN_ROOT + "enterprise.system.webservices.rpc";

    /**
     * JAXR Logger
     */
    public static final String JAXR_LOGGER = DOMAIN_ROOT + "enterprise.system.webservices.registry";

    /**
     * SAAJ Logger
     */
    public static final String SAAJ_LOGGER = DOMAIN_ROOT + "enterprise.system.webservices.saaj";

    /**
     * Self Management Logger
     */
    public static final String SELF_MANAGEMENT_LOGGER = DOMAIN_ROOT + "enterprise.system.core.selfmanagement";

    /**
     * SQL Tracing Logger
     */
    public static final String SQL_TRACE_LOGGER = DOMAIN_ROOT + "enterprise.resource.sqltrace";

    /**
     * Admin Logger
     */
    public static final String ADMIN_LOGGER = DOMAIN_ROOT + "enterprise.system.tools.admin";
    /**
     * Server Logger
     */
    public static final String SERVER_LOGGER = DOMAIN_ROOT + "enterprise.system";
    /**
     * core Logger
     */
    public static final String CORE_LOGGER = DOMAIN_ROOT + "enterprise.system.core";
    /**
     * classloader Logger
     */
    public static final String LOADER_LOGGER = DOMAIN_ROOT + "enterprise.system.core.classloading";

    /**
     * Config Logger
     */
    public static final String CONFIG_LOGGER = DOMAIN_ROOT + "enterprise.system.core.config";

    /**
     * Process Launcher Logger
     */
    public static final String PROCESS_LAUNCHER_LOGGER = DOMAIN_ROOT + "enterprise.tools.launcher";

    /**
     * GMS Logger
     */
    public static final String GMS_LOGGER = DOMAIN_ROOT + "org.glassfish.gms";

    /**
     * AMX Logger
     */
    public static final String AMX_LOGGER = DOMAIN_ROOT + "enterprise.system.amx";

    /**
     * JMX Logger
     */
    public static final String JMX_LOGGER = DOMAIN_ROOT + "enterprise.system.jmx";

    /**
     * core/kernel Logger
     */
    public static final String SERVICES_LOGGER = DOMAIN_ROOT + "enterprise.system.core.services";

    /**
     * webservices logger
     */
    public static final String WEBSERVICES_LOGGER = DOMAIN_ROOT + "enterprise.webservices";

    /**
     * monitoring logger
     */
    public static final String MONITORING_LOGGER = DOMAIN_ROOT + "enterprise.system.tools.monitor";

    /**
     * persistence logger
     */
    public static final String PERSISTENCE_LOGGER = DOMAIN_ROOT + "org.glassfish.persistence";

    /**
     * virtualization logger
     */
    public static final String VIRTUALIZATION_LOGGER = DOMAIN_ROOT + "org.glassfish.virtualization";

    /**
     * PaaS logger
     */
    public static final String PAAS_LOGGER = DOMAIN_ROOT + "org.glassfish.paas";

    /**
     * Returns initialized logger using resource bundle found by the class's
     * classloader.
     *
     * @param clazz
     * @param name
     * @return
     */
    public static Logger getLogger(final Class clazz, final String namePrefix) {
        return getLogger(clazz, namePrefix, true);
    }


    /**
     * Returns initialized logger. If the resourceBundleLookup is true, tries to
     * find and load the LogStrings.properties via the clazz's classloader.
     *
     * @param clazz
     * @param name
     * @return
     */
    public static Logger getLogger(final Class clazz, final String namePrefix, final boolean resourceBundleLookup) {
        final ClassLoader contextClassLoader = resourceBundleLookup ? clazz.getClassLoader() : null;
        return getLogger(clazz, namePrefix, contextClassLoader);
    }

    /**
     * Returns initialized logger. If the resourceBundleLoader is not null,
     * tries to find and load the LogStrings.properties.
     *
     * @param clazz
     * @param namePrefix
     * @param resourceBundleLoader
     * @return
     */
    public static Logger getLogger(final Class clazz, final String namePrefix, final ClassLoader resourceBundleLoader) {
        final String loggerName = getLoggerName(clazz, namePrefix);

        final LogManager logManager = LogManager.getLogManager();
        final Logger cachedLogger = logManager.getLogger(loggerName);
        if (cachedLogger != null) {
            return cachedLogger;
        }

        final String rbName = getResourceBundleNameForDomainRoot(namePrefix);
        final ResourceBundle resourceBundle;
        if (resourceBundleLoader == null) {
            resourceBundle = null;
        } else {
            resourceBundle = getResourceBundle(rbName, clazz, resourceBundleLoader);
        }

        // we should only add a logger of the same name at time.
        final Logger cLogger = new LogDomainsLogger(loggerName, resourceBundle);

        // We must not return an orphan logger (the one we just created) if
        // a race condition has already created one
        boolean added = logManager.addLogger(cLogger);
        if (added) {
            return cLogger;
        }

        // Another thread was faster
        return logManager.getLogger(cLogger.getName());
    }

    // uncomment System.x.println lines only for some voodoo testing
    private static ResourceBundle getResourceBundle(final String name, final Class clazz,
            final ClassLoader resourceBundleLoader) {
        final ResourceBundle classBundle = findResourceBundle(name, clazz, resourceBundleLoader);
        if (classBundle != null) {
//            System.out.println("Found resource bundle by given classloader: " + name + " for " + clazz);
            return classBundle;
        }
        Logger.getAnonymousLogger().info("Cannot find the resource bundle for the name " + name + " for " + clazz + " using "
                + resourceBundleLoader);
        return null;
    }

    private static ResourceBundle findResourceBundle(final String name, final Class clazz,
            final ClassLoader classLoader) {
        final ResourceBundle packageRootBundle = tryTofindResourceBundle(name, classLoader);
        if (packageRootBundle != null) {
            return packageRootBundle;
        }
        // not found. Ok, let's try to go through the class's package tree
        final StringBuilder rbPackage = new StringBuilder(clazz.getPackage().getName());
        while (true) {
            final ResourceBundle subPkgBundle = tryTofindResourceBundle(rbPackage.toString(), classLoader);
            if (subPkgBundle != null) {
                return subPkgBundle;
            }
            final int lastDotIndex = rbPackage.lastIndexOf(".");
            if (lastDotIndex == -1) {
                break;
            }
            rbPackage.delete(lastDotIndex, rbPackage.length());
        }

        return null;
    }

    private static String getLoggerName(Class clazz, String logDomainsConstantName) {
        String pkgName = clazz.getPackage().getName();
        String loggerName = logDomainsConstantName + "." + pkgName;
        return loggerName;
    }

    private static ResourceBundle tryTofindResourceBundle(final String name, final ClassLoader classLoader) {
//        System.out.println("name=" + name + ", classLoader=" + classLoader);
        try {
            return ResourceBundle.getBundle(name + "." + LogDomains.RESOURCE_BUNDLE, Locale.getDefault(), classLoader);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    private static String getResourceBundleNameForDomainRoot(final String loggerNamePrefix) {
        if (loggerNamePrefix.startsWith(LogDomains.DOMAIN_ROOT)) {
            return loggerNamePrefix.replaceFirst(LogDomains.DOMAIN_ROOT, LogDomains.PACKAGE_ROOT);
        }
        return loggerNamePrefix;
    }
}
