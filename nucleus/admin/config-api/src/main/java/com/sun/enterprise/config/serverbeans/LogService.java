/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.config.serverbeans;

import java.beans.PropertyVetoException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;

/**
 * By default, logs would be kept in $INSTANCE-ROOT/logs. The following log 
 * files will be stored under the logs directory.
 * access.log                                                                 
 *     keeps default virtual server HTTP access messages.            
 * server.log                                                                 
 *     keeps log messages from default virtual server. Messages from 
 *     other configured virtual servers also go here, unless         
 *     log-file is explicitly specified in the virtual-server        
 *     element.                                                      
 */
@Configured
public interface LogService extends ConfigBeanProxy  {

    /**
     * Gets the value of the file property.
     *
     * Can be used to rename or relocate server.log using absolute path.
     * 
     * @return possible object is {@link String }
     */
    @Attribute
    public String getFile();

    /**
     * Sets the value of the file property.
     *
     * @param value allowed object is {@link String }
     * @throws java.beans.PropertyVetoException
     */
    public void setFile(String value) throws PropertyVetoException;
    
     /**
     * Gets the value of the Payara Notification file property.
     *
     * Can be used to rename or relocate notification.log using absolute path.
     * 
     * @return possible object is {@link String }
     */
    @Attribute
    public String getPayaraNotificationFile();

    /**
     * Sets the value of the  Payara Notification file property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setPayaraNotificationFile(String value) throws PropertyVetoException;

    /**
     * Gets the value of the useSystemLogging property.
     *
     * If true, will utilize Unix syslog service or Windows Event Logging to
     * produce and manage logs.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    public String getUseSystemLogging();

    /**
     * Sets the value of the useSystemLogging property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setUseSystemLogging(String value) throws PropertyVetoException;

    /**
     * Gets the value of the logHandler property.
     *
     * Can plug in a custom log handler to add it to the chain of handlers to
     * log into a different log destination than the default ones given by the
     * system (which are Console, File and Syslog). It is a requirement that
     * customers use the log formatter provided by the the system to maintain
     * uniformity in log messages. The custom log handler will be added at the
     * end of the handler chain after File + Syslog Handler, Console Handler and
     * JMX Handler. User cannot replace the handler provided by the system,
     * because of loosing precious log statements. The Server Initialization
     * will take care of installing the custom handler with the system formatter
     * initialized. The user need to use JSR 047 Log Handler Interface to
     * implement the custom handler.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getLogHandler();

    /**
     * Sets the value of the logHandler property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setLogHandler(String value) throws PropertyVetoException;

    /**
     * Gets the value of the logFilter property.
     *
     * Can plug in a log filter to do custom filtering of log records.
     * By default there is no log filter other than the log level filtering
     * provided by JSR 047 log API.
     * 
     * @return possible object is {@link String }
     */
    @Attribute
    public String getLogFilter();

    /**
     * Sets the value of the logFilter property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setLogFilter(String value) throws PropertyVetoException;

    /**
     * Gets the value of the logToConsole property.
     *
     * logs will be sent to stderr when asadmin start-domain verbose is used
     * 
     * @return possible object is {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    public String getLogToConsole();

    /**
     * Sets the value of the logToConsole property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setLogToConsole(String value) throws PropertyVetoException;
    
    /**
     * Gets the value of the logToFile property.
     *
     * @return possible object is {@link String }
     */
    @Attribute(defaultValue = "true", dataType = Boolean.class)
    public String getLogToFile();

    /**
     * Sets the value of the logToFile property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setLogToFile(String value) throws PropertyVetoException;
    
     /**
     * Gets the value of the  Payara Notification logToFile property.
     *
     * @return possible object is {@link String }
     */
    @Attribute(defaultValue = "true", dataType = Boolean.class)
    public String getPayaraNotificationLogToFile();

    /**
     * Sets the value of the  Payara Notification logToFile property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setPayaraNotificationLogToFile(String value) throws PropertyVetoException;


    /**
     * Gets the value of the logRotationLimitInBytes property.
     *
     * Log Files will be rotated when the file size reaches the limit.
     * Minimum value is 1.
     * @return possible object is {@link String }
     */
    @Attribute (defaultValue="1")
    @Min(value=1)
    public String getLogRotationLimitInBytes();

    /**
     * Sets the value of the logRotationLimitInBytes property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setLogRotationLimitInBytes(String value) throws PropertyVetoException;
    
    /**
     * Gets the value of the  Payara Notification logRotationLimitInBytes property.
     *
     * Log Files will be rotated when the file size reaches the limit.
     *
     * @return possible object is {@link String }
     */
    @Attribute (defaultValue="1")
    @Min(value=1)
    public String getPayaraNotificationLogRotationLimitInBytes();

    /**
     * Sets the value of the  Payara Notification logRotationLimitInBytes property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setPayaraNotificationLogRotationLimitInBytes(String value) throws PropertyVetoException;

    /**
     * Gets the value of the logRotationTimelimitInMinutes property.
     *
     * This is a new attribute to enable time based log rotation.
     * The Log File will be rotated only if this value is non-zero and the valid
     * range is 60 minutes (1 hour) to 10*24*60 minutes (10 days). If the value
     * is zero then the files will be rotated based on size specified in
     * log-rotation-limit-in-bytes.
     * 
     * @return possible object is {@link String }
     */
    @Attribute (defaultValue="0")
    @Min(value=0)
    @Max(value=14400)
    public String getLogRotationTimelimitInMinutes();

    /**
     * Sets the value of the logRotationTimelimitInMinutes property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setLogRotationTimelimitInMinutes(String value) throws PropertyVetoException;

    /**
     * Gets the value of the  Payara Notification logRotationTimelimitInMinutes property.
     *
     * This is a new attribute to enable time based log rotation.
     * The Log File will be rotated only if this value is non-zero and the valid
     * range is 60 minutes (1 hour) to 10*24*60 minutes (10 days). If the value
     * is zero then the files will be rotated based on size specified in
     * log-rotation-limit-in-bytes.
     * 
     * @return possible object is {@link String }
     */
    @Attribute (defaultValue="0")
    @Min(value=0)
    @Max(value=14400)
    public String getPayaraNotificationLogRotationTimelimitInMinutes();

    /**
     * Sets the value of the  Payara Notification logRotationTimelimitInMinutes property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setPayaraNotificationLogRotationTimelimitInMinutes(String value) throws PropertyVetoException;

    /**
     * Gets the value of the alarms property.
     *
     * if true, will turn on alarms for the logger. The SEVERE and WARNING
     * messages can be routed through the JMX framework to raise SEVERE and
     * WARNING alerts. Alarms are turned off by default.
     * 
     * @return possible object is {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    public String getAlarms();

    /**
     * Sets the value of the alarms property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setAlarms(String value) throws PropertyVetoException;

    /**
     * Gets the value of the retainErrorStatisticsForHours property.
     *
     * The number of hours since server start, for which error statistics should
     * be retained in memory. The default and minimum value is 5 hours.
     * The maximum value allowed is 500 hours. Note that larger values will
     * incur additional memory overhead.
     * 
     * @return possible object is {@link String }
     */
    @Attribute (defaultValue="5")
    @Min(value=5)
    @Max(value=500)
    public String getRetainErrorStatisticsForHours();

    /**
     * Sets the value of the retainErrorStatisticsForHours property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setRetainErrorStatisticsForHours(String value) throws PropertyVetoException;

    /**
     * Gets the value of the logStandardStreams property.
     *
     */
    @Attribute(defaultValue = "true", dataType = Boolean.class)
    public String getLogStandardStreams();

    /**
     * Sets the value of the logStandardStreams property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    public void setLogStandardStreams(String value) throws PropertyVetoException;
    
    /**
     * Gets the value of the moduleLogLevels property.
     *
     * @return possible object is {@link ModuleLogLevels }
     */
    @Element
    public ModuleLogLevels getModuleLogLevels();

    /**
     * Sets the value of the moduleLogLevels property.
     *
     * @param value allowed object is {@link ModuleLogLevels }
     * @throws PropertyVetoException
     */
    public void setModuleLogLevels(ModuleLogLevels value) throws PropertyVetoException;

}
