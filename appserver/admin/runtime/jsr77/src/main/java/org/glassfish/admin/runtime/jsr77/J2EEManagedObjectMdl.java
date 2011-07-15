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

package org.glassfish.admin.runtime.jsr77;

import javax.management.*;
import java.util.Set;
import java.util.HashSet;
import javax.management.j2ee.Management;
import javax.management.j2ee.ManagementHome;
import javax.rmi.PortableRemoteObject;
import javax.naming.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * The base class for all Managed Objects
 *
 * @author Hans Hrasna
 */
public abstract class J2EEManagedObjectMdl {

    // name - key attribute for the managed object
    private final String name;
    private final boolean stateManageable;
    private final boolean statisticsProvider;
    private final boolean eventProvider;
    private final String serverName;
    
    private volatile MBeanServer mServer;
    private volatile ObjectName  mSelfObjectName;

    J2EEManagedObjectMdl(String name,boolean state, boolean statistics, boolean events) {
        this( name, null, state, statistics, events );
    }

    J2EEManagedObjectMdl(String name, String serverName,
                         boolean state, boolean statistics, boolean events) {
        this.name = name;
        stateManageable = state;
        statisticsProvider = statistics;
        eventProvider = events;
        this.serverName = serverName;
        
    }
    
    
    // returns the MBeanServer local to this VM
    // creates one if it doesn't exsist yet
    protected final MBeanServer getMBeanServer() {
        java.util.ArrayList servers =
                (java.util.ArrayList) AccessController.doPrivileged(new PrivilegedAction() {
                    public java.lang.Object run() {
                        return MBeanServerFactory.findMBeanServer(null);
                    }
                });
        if (servers.isEmpty()) {
            return (MBeanServer) AccessController.doPrivileged(new PrivilegedAction() {
                    public java.lang.Object run() {
                        return MBeanServerFactory.createMBeanServer();
                    }
                });
        } else {
            return (MBeanServer)servers.get(0);
        }
    }

    
    protected final Management getMEJB() {
        Management mejb=null;
        try {
            Context ic = new InitialContext();
            String ejbName = System.getProperty("mejb.name","ejb/mgmt/MEJB");
            java.lang.Object objref = ic.lookup(ejbName);
            ManagementHome home = (ManagementHome)PortableRemoteObject.narrow(objref, ManagementHome.class);
            mejb = home.create();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return mejb;
    }
      
    public final Set findNames(String keys) {
        // FIXME
        return null;
    }


    /**
     * The type of the J2EEManagedObject as specified by JSR77. The class that implements a specific type must override this
     * method and return the appropriate type string.
     */
    public abstract String getj2eeType();

    protected final ObjectName getObjectName()
    {
        return mSelfObjectName;
    }
    
    /**
     * The name of the J2EEManagedObject. All managed objects must have a unique name within the context of the management
     * domain. The name must not be null.
     */
    public abstract String getobjectName();

    /**
     * If true, indicates that this managed object implements the StateManageable interface and is state manageable. If false,
     * the managed object does not support state management.
     */
    public  boolean isstateManageable() {
        return stateManageable;
    }

    /**
     * If true, indicates that the managed object supports performance statistics and therefore implements the
     * StatisticsProvider model. If false, the J2EEManagedObject does not support performance statistics
     */
    public final boolean isstatisticsProvider() {
        return statisticsProvider;
    }

    /**
     * If true, indicates that the managed object provides event notification about events that occur on that object. All
     * managed objects that support state management are by default event providers. If the stateManageable attribute for this
     * managed object is true then the eventProvider attribute must also be true.
     */
    public  boolean iseventProvider() {
        return eventProvider;
    }

    /** Accessor method for the parent key */
    public final String getJ2EEServer() {
        if (serverName != null) {
                return serverName;
        } else {
                return "no-server";
        }
    }
    /** Accessor method for the parent key */
    public final String getname(){
        return this.name;
    }
    
        protected final long
    now()
    {
        return System.currentTimeMillis();
    }
}
