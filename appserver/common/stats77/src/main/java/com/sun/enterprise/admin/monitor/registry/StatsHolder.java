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

import org.glassfish.j2ee.statistics.Stats;
import javax.management.ObjectName;
import java.util.Collection;


/**
 * Provides the ability to associate various j2ee components
 * and sub components in a hierarchical tree. Holds references to
 * underlying Stats objects. On instantiation, the Stats object is
 * converted to a DynamicMBean instance. When monitoring level is
 * changed from OFF to LOW or HIGH, the MBean is registered with an
 * MBeanServer. Calls made to the MBean are delegated to this object
 * which in turn delegates it to underlying Stats object.
 * @author  Shreedhar Ganapathy <mailto:shreedhar.ganapathy@sun.com>
 */
public interface StatsHolder {
    /**
     * Add a child node or leaf to this node.
     * @param statsHolder
     */
    StatsHolder addChild(String name, MonitoredObjectType type);
    
    /**
     * return an array of StatHolder objects each representing a child
     * of this node.
     * @return Collection
     */
    Collection getAllChildren();

    /**
     * removes all children belonging to this node.
     */
    void removeAllChildren();

    /**
     * Returns name of this hierarchical node
     */
    String getName();
    
    /**
     * Returns type of this hierarchical node
     */
    MonitoredObjectType getType();
    
    /**
     * sets this hierarchical node's associated stats object. Used when node was
     * originally created without a Stats implementation or a new monitoring 
     * level has been set requiring a new Stats registration
     */
    void setStats(Stats stats);
	
	Stats getStats();
    
	void setStatsClass(Class c);
	
	Class getStatsClass();
	
	void setStatsClassName(String cName);
	
	String getStatsClassName();

	/**
     * Sets the ObjectName pertaining to the MBean for this node.
     */
    void setObjectName(ObjectName name);    
    
    /**
     * Gets the ObjectName pertaining to the MBean for this node.
     */
    ObjectName getObjectName();    

	/**
	 * Sets the hierarchically denoted dotted name for this node.
	 */
	void setDottedName(String dottedName);
	
	/**
	 * Gets the hierarchically denoted dotted name for this node.
	 */
	String getDottedName();
	
    /**
     * Registers a monitoring MBean with the MBeanServer
     */
    void registerMBean();
    
    /**
     * Unregisters a monitoring MBean from the MBean Server
     */
    void unregisterMBean();
	
	void setType(MonitoredObjectType type);
	
	StatsHolder getChild(String name);
	
	void removeChild(String name);
}
