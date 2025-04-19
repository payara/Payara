/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2018-2025] [Payara Foundation and/or its affiliates]

package com.sun.common.util.logging;

import com.sun.logging.LogDomains;
import java.util.Map;

import static java.util.Map.entry;


public class LoggingXMLNames {

    public static final String file = "file";
    
    public static final String payaraNotificationFile = "payara-notification-file";

    public static final String logRotationLimitInBytes = "log-rotation-limit-in-bytes";
    
    public static final String payaraNotificationLogRotationLimitInBytes = "payara-notification-log-rotation-limit-in-bytes";

    public static final String logRotationTimelimitInMinutes = "log-rotation-timelimit-in-minutes";
    
    public static final String payaraNotificationLogRotationTimelimitInMinutes = "payara-notification-log-rotation-timelimit-in-minutes";

    public static final String logFormatter = "log-formatter";
    
    public static final String payaraNotificationLogFormatter = "payara-notification-log-formatter";

    public static final String logHandler = "log-handler";

    public static final String useSystemLogging = "use-system-logging";

    public static final String logFilter = "log-filter";
    
    public static final String logToFile = "log-to-file";

    public static final String fastLogging = "fast-logging";
    
    public static final String payaraNotificationLogToFile = "payara-notification-log-to-file";

    public static final String logToConsole = "log-to-console";

    public static final String alarms = "alarms";
    
    public static final String logStandardStreams = "log-standard-streams";

    public static final String retainErrorStatisticsForHours = "retain-error-statistics-for-hours";
    // logger names from DTD
    public static final String root = "root";
    public static final String server = "server";
    public static final String ejbcontainer = "ejb-container";
    public static final String cmpcontainer = "cmp-container";
    public static final String mdbcontainer = "mdb-container";
    public static final String webcontainer = "web-container";
    public static final String classloader = "classloader";
    public static final String configuration = "configuration";
    public static final String naming = "naming";
    public static final String security = "security";
    public static final String jts = "jts";
    public static final String jta = "jta";
    public static final String admin = "admin";
    public static final String deployment = "deployment";
    public static final String verifier = "verifier";
    public static final String jaxr = "jaxr";
    public static final String jaxrpc = "jaxrpc";
    public static final String saaj = "saaj";
    public static final String corba = "corba";
    public static final String javamail = "javamail";
    public static final String jms = "jms";
    public static final String connector = "connector";
    public static final String jdo = "jdo";
    public static final String cmp = "cmp";
    public static final String util = "util";
    public static final String resourceadapter = "resource-adapter";
    public static final String synchronization = "synchronization";
    public static final String nodeAgent = "node-agent";
    public static final String selfmanagement = "self-management";
    public static final String groupmanagementservice = "group-management-service";
    public static final String managementevent = "management-event";

    private static final String LEVEL = ".level";

//mapping of the names used in domain.xml to the names used in logging.properties

    public static final Map<String, String> xmltoPropsMap = Map.ofEntries(
            entry(logRotationLimitInBytes, LoggingPropertyNames.logRotationLimitInBytes),
            entry(payaraNotificationLogRotationLimitInBytes, LoggingPropertyNames.payaraNotificationLogRotationLimitInBytes),
            entry(logRotationTimelimitInMinutes, LoggingPropertyNames.logRotationTimelimitInMinutes),
            entry(payaraNotificationLogRotationTimelimitInMinutes, LoggingPropertyNames.payaraNotificationLogRotationTimelimitInMinutes),
            entry(file, LoggingPropertyNames.file),
            entry(payaraNotificationFile, LoggingPropertyNames.payaraNotificationFile),
            entry(logFormatter, LoggingPropertyNames.logFormatter),
            entry(payaraNotificationLogFormatter, LoggingPropertyNames.payaraNotificationLogFormatter),
            entry(logStandardStreams, LoggingPropertyNames.logStandardStreams),
            entry(logHandler, LoggingPropertyNames.logHandler),
            entry(useSystemLogging, LoggingPropertyNames.useSystemLogging),
            entry(retainErrorStatisticsForHours, LoggingPropertyNames.retainErrorStatisticsForHours),
            entry(logFilter, LoggingPropertyNames.logFilter),
            entry(logToFile, LoggingPropertyNames.logToFile),
            entry(payaraNotificationLogToFile, LoggingPropertyNames.payaraNotificationLogToFile),
            entry(logToConsole, LoggingPropertyNames.logToConsole),
            entry(fastLogging, LoggingPropertyNames.fastLogging),
            entry(alarms, LoggingPropertyNames.alarms),
            entry(root, LogDomains.DOMAIN_ROOT + LEVEL),
            entry(server, LogDomains.SERVER_LOGGER + LEVEL),
            entry(ejbcontainer, LogDomains.EJB_LOGGER + LEVEL),
            entry(cmpcontainer, LogDomains.CMP_LOGGER + LEVEL),
            entry(mdbcontainer, LogDomains.MDB_LOGGER + LEVEL),
            entry(webcontainer, LogDomains.WEB_LOGGER + LEVEL),
            entry(classloader, LogDomains.LOADER_LOGGER + LEVEL),
            entry(configuration, LogDomains.CONFIG_LOGGER + LEVEL),
            entry(naming, LogDomains.NAMING_LOGGER + LEVEL),
            entry(security, LogDomains.SECURITY_LOGGER + LEVEL),
            entry(jts, LogDomains.TRANSACTION_LOGGER + LEVEL),
            entry(jta, LogDomains.JTA_LOGGER + LEVEL),
            entry(admin, LogDomains.ADMIN_LOGGER + LEVEL),
            entry(deployment, LogDomains.DPL_LOGGER + LEVEL),
            //  entry(verifier, LogDomains.
            entry(jaxr, LogDomains.JAXR_LOGGER + LEVEL),
            entry(jaxrpc, LogDomains.JAXRPC_LOGGER + LEVEL),
            entry(saaj, LogDomains.SAAJ_LOGGER + LEVEL),
            entry(corba, LogDomains.CORBA_LOGGER + LEVEL),
            entry(javamail, LogDomains.JAVAMAIL_LOGGER + LEVEL),
            entry(jms, LogDomains.JMS_LOGGER + LEVEL),
            //  entry(connector, LogDomains.
            entry(jdo, LogDomains.JDO_LOGGER + LEVEL),
            entry(cmp, LogDomains.CMP_LOGGER + LEVEL),
            entry(util, LogDomains.UTIL_LOGGER + LEVEL),
            entry(resourceadapter, LogDomains.RSR_LOGGER + LEVEL),
            //  entry(synchronization, LogDomains.
            //  entry(nodeAgent, LogDomains.
            entry(selfmanagement, LogDomains.SELF_MANAGEMENT_LOGGER + LEVEL)
            //  entry(groupManagementService, LogDomains.
            //  entry(managementEvent, LogDomains.
    );

}

