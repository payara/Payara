/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.monitor.registry;

/**
 * Implementation of this interface enables notifications to 
 * be received for change in monitoring level from a prior 
 * level to a new one. Monitoring levels are defined by the 
 * constants class MonitoringLevel and are currently defined as 
 * OFF, LOW and HIGH.
 * @author  <href=mailto:shreedhar.ganapathy@sun.com>Shreedhar Ganapathy
 */
public interface MonitoringLevelListener {
    /**
     * Sets the monitoring level to a new level. Values are defined by 
     * MonitoringLevel.OFF, MonitoringLevel.LOW, and MonitoringLevel.High
     * @param level corresponding to MonitoringLevel OFF, LOW or HIGH
     * @deprecated
     */
    public void setLevel(MonitoringLevel level);
    
	/**
	 * Method to convey the change in monitoring level. It is a usual practice that
	 * various components may have <em> single instance of listener </em> to listen
	 * to the changes in monitoring-level for various registered Stats objects. This
	 * method gives a context for such components to be returned when it is
	 * called.
	 * @deprecated
	 * @param from		the MonitoringLevel before the change
	 * @param to		the MonitoringLevel after the change
	 * @param handback	the Stats object that was passed to the registry during registration. It
	 * is guaranteed that it will be unchanged by monitoring framework.
	 */
	public void changeLevel(MonitoringLevel from, MonitoringLevel to, 
		org.glassfish.j2ee.statistics.Stats handback);

	/**
	 * Method to convey the change in monitoring level. The configuration
	 * of monitoring pertains to certain components like ejb-container, web-container,
	 * orb, transaction-service, connection-pools, thread-pools etc. The third
	 * parameter loosely corresponds to the configuration in domain.xml as follows:
	 * <ul>
	 * <li> connector-connection-pool : MonitoredObjectType#CONNECTOR_CONN_POOL </li>
	 * <li> ejb-container : MonitoredObjectType#EJB </li>
	 * <li> http-service, http-listeners etc. : MonitoredObjectType#HTTP_SERVICE </li>
	 * <li> jdbc-connection-pool : MonitoredObjectType#JDBC_CONN_POOL </li>
	 * <li> orb : MonitoredObjectType#ORB </li>
	 * <li> thread-pool : MonitoredObjectType#THREAD_POOL </li>
	 * <li> transaction-service : MonitoredObjectType#TRANSACTION_SERVICE </li>
	 * <li> web-container : MonitoredObjectType#WEB_COMPONENT </li>
	 * <li> </li>
	 * </ul>
	 * The core components are expected to follow the above.
	 * When the level changes through administrative interfaces, the notification
	 * is sent to the registered listeners for corresponding types and thus the
	 * dynamic reconfiguration is done.
	 * @param from		the MonitoringLevel before the change
	 * @param to		the MonitoringLevel after the change
	 * @param type		the MonitoredObjectType that had the level changed
	 */
	public void changeLevel(MonitoringLevel from, MonitoringLevel to,
		MonitoredObjectType type);
}
