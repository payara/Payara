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

// Portions Copyright 2018 [Payara Foundation and/or its affiliates]

/*
 * Constants.java
 *
 * Created on January 21, 2004, 11:31 PM
 */

package com.sun.enterprise.backup;

/**
 *
 * @author  bnevins
 */
public interface Constants
{
    final static String    loggingResourceBundle = "com.sun.enterprise.backup.LocalStrings";
    final static String    exceptionResourceBundle = "/com/sun/enterprise/backup/LocalStrings.properties";
    final static String    BACKUP_DIR = "backups";
    final static String    OSGI_CACHE = "osgi-cache";
    final static String    PROPS_USER_NAME = "user.name";
    final static String    PROPS_TIMESTAMP_MSEC = "timestamp.msec";
    final static String    PROPS_TIMESTAMP_HUMAN = "timestamp.human";
    final static String    PROPS_DOMAINS_DIR = "domains.dir";
    final static String    PROPS_DOMAIN_DIR = "domain.dir";
    final static String    PROPS_DOMAIN_NAME = "domain.name";
    final static String    PROPS_BACKUP_FILE = "backup.file";
    final static String    PROPS_DESCRIPTION = "description";
    final static String    PROPS_HEADER = "Backup Status";
    final static String    PROPS_VERSION = "version";
    final static String    PROPS_TYPE = "type";
    final static String    BACKUP_CONFIG = "backupConfig";
    final static String    PROPS_FILENAME = "backup.properties";
    final static String    CONFIG_ONLY ="configOnly";
    final static String    FULL ="full";
    final static String    CONFIG_DIR = "config";
    final static String    NO_CONFIG = " ";
    final static String    LOGGING_CONFIG = "logging.properties";
    final static String    GF_FILE_HANDLER_LOG_TO_FILE = "com.sun.enterprise.server.logging.GFFileHandler.logtoFile";
    final static String    PAYARA_HANDLER_LOG_TO_FILE = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.logtoFile";
    final static String    PAYARA_HANDLER_ROTATION_ON_DATE_CHANGE = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.rotationOnDateChange";
    final static String    PAYARA_HANDLER_ROTATION_ON_TIME_LIMIT = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.rotationTimelimitInMinutes";
    final static String    PAYARA_HANDLER_ROTATION_ON_FILE_SIZE = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.rotationLimitInBytes";
    final static String    PAYARA_HANDLER_MAXIMUM_FILES = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.maxHistoryFiles";
    final static String    PAYARA_HANDLER_LOG_FILE = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.file";
    final static String    GF_FILE_HANDLER_LOG_TO_FILE_VALUE = GF_FILE_HANDLER_LOG_TO_FILE + "=true";
    final static String    PAYARA_HANDLER_LOG_TO_FILE_VALUE = PAYARA_HANDLER_LOG_TO_FILE + "=true";
    final static String    PAYARA_HANDLER_ROTATION_ON_DATE_CHANGE_VALUE = PAYARA_HANDLER_ROTATION_ON_DATE_CHANGE + "=false";
    final static String    PAYARA_HANDLER_ROTATION_ON_TIME_LIMIT_VALUE = PAYARA_HANDLER_ROTATION_ON_TIME_LIMIT + "=0";
    final static String    PAYARA_HANDLER_ROTATION_ON_FILE_SIZE_VALUE = PAYARA_HANDLER_ROTATION_ON_FILE_SIZE + "=2000000";
    final static String    PAYARA_HANDLER_MAXIMUM_FILES_VALUE = PAYARA_HANDLER_MAXIMUM_FILES + "=0";
    final static String    PAYARA_HANDLER_LOG_FILE_VALUE = PAYARA_HANDLER_LOG_FILE + "=${com.sun.aas.instanceRoot}/logs/notification.log";
}
