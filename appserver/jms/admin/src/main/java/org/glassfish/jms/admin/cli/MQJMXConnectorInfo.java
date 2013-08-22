/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jms.admin.cli;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectorFactory;
import javax.management.MBeanServerConnection;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

/**
 * The <code>MQJMXConnectorInfo</code> holds MBean Server connection information
 * to a SJSMQ broker instance. This API is used by the admin infrastructure for
 * performing MQ administration/configuration operations on a broker instance.
 *
 * @author Sivakumar Thyagarajan
 * @since SJSAS 9.0
 */
public class MQJMXConnectorInfo {
    private String jmxServiceURL = null;
    private Map<String,?> jmxConnectorEnv = null;
    private String asInstanceName = null;
    private String brokerInstanceName = null;
    private String brokerType = null;
    private static final Logger _logger = Logger.getLogger(LogUtils.JMS_ADMIN_LOGGER);
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(MQJMXConnectorInfo.class);
    private JMXConnector connector = null;

    public MQJMXConnectorInfo(String asInstanceName, String brokerInstanceName,
                              String brokerType, String jmxServiceURL,
                                       Map<String, ?> jmxConnectorEnv) {
        this.brokerInstanceName = brokerInstanceName;
        this.asInstanceName = asInstanceName;
        this.jmxServiceURL = jmxServiceURL;
        this.brokerType = brokerType;
        this.jmxConnectorEnv = jmxConnectorEnv;
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "MQJMXConnectorInfo : brokerInstanceName " +
                            brokerInstanceName + " ASInstanceName " + asInstanceName +
                            " jmxServiceURL "  + jmxServiceURL +  " BrokerType " + brokerType
                            + " jmxConnectorEnv " + jmxConnectorEnv);
        }
    }

    public String getBrokerInstanceName(){
        return this.brokerInstanceName;
    }

    public String getBrokerType(){
        return this.brokerType;
    }

    public String getASInstanceName(){
        return this.asInstanceName;
    }

    public String getJMXServiceURL(){
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"MQJMXConnectorInfo :: JMXServiceURL is " + this.jmxServiceURL);
        }
        return this.jmxServiceURL;
    }

    public Map<String, ?> getJMXConnectorEnv(){
        return this.jmxConnectorEnv;
    }

    /**
     * Returns an <code>MBeanServerConnection</code> representing the MQ broker instance's MBean
     * server.
     * @return
     * @throws ConnectorRuntimeException
     */
    //XXX:Enhance to support SSL (once MQ team delivers support in the next drop)
    //XXX: Discuss how <code>ConnectionNotificationListeners</code> could
    //be shared with the consumer of this API
    public MBeanServerConnection getMQMBeanServerConnection() throws ConnectorRuntimeException {
        try {
            if (getJMXServiceURL() == null || getJMXServiceURL().equals("")) {
                String msg = localStrings.getLocalString("error.get.jmsserviceurl",
                                "Failed to get MQ JMXServiceURL of {0}.", getASInstanceName());
                throw new ConnectorRuntimeException(msg);
            }
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                "creating MBeanServerConnection to MQ JMXServer with "+getJMXServiceURL());
            }
            JMXServiceURL jmxServiceURL = new JMXServiceURL(getJMXServiceURL());
            connector = JMXConnectorFactory.connect(jmxServiceURL, this.jmxConnectorEnv);
            //XXX: Do we need to pass in a Subject?
            MBeanServerConnection mbsc = connector.getMBeanServerConnection();
            return mbsc;
        } catch (Exception e) {
            e.printStackTrace();
            ConnectorRuntimeException cre = new ConnectorRuntimeException(e.getMessage());
            cre.initCause(e);
            throw cre;
        }
    }

    public void closeMQMBeanServerConnection() throws ConnectorRuntimeException {
        try {
           if (connector != null) {
                 connector.close();
           }
        } catch (IOException e) {
            ConnectorRuntimeException cre = new ConnectorRuntimeException(e.getMessage());
            cre.initCause(e);
            throw cre;
        }
    }
 }
