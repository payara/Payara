/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * MBeanServerConnectionMethods.java
 * $Id: MBeanServerConnectionMethods.java,v 1.3 2005/12/25 04:26:34 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 04:26:34 $
 * Indentation Information:
 * 0. Please (try to) preserve these settings.
 * 1. Tabs are preferred over spaces.
 * 2. In vi/vim -
 *		:set tabstop=4 :set shiftwidth=4 :set softtabstop=4
 * 3. In S1 Studio -
 *		1. Tools->Options->Editor Settings->Java Editor->Tab Size = 4
 *		2. Tools->Options->Indentation Engines->Java Indentation Engine->Expand Tabs to Spaces = False.
 *		3. Tools->Options->Indentation Engines->Java Indentation Engine->Number of Spaces per Tab = 4.
 * Unit Testing Information:
 * 0. Is Standard Unit Test Written (y/n):
 * 1. Unit Test Location: (The instructions should be in the Unit Test Class itself).
 */

package com.sun.enterprise.admin.jmx.remote.internal;
import java.lang.reflect.Method;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.message.MBeanServerRequestMessage;

/** A class that links the class {@link MBeanServerRequestMessage} and interface {@link MBeanServerConnection}.
 * The former of the two contains integers that has ids of the methods in the latter.
 * Instead of designing an enum kind of construct I chose to take the reflection-oriented
 * path because I did not want to worry about the internationalization etc. The cost
 * is one-time and is bourne at the time of loading this class. The class is loaded
 * as late as possible. Note that this class depends on the constant value fields from
 * {@link MBeanServerRequestMessage}
 * @author  mailto:Kedar.Mhaswade@Sun.Com
 * @since Sun Java System Application Server 8
 */
class MBeanServerConnectionMethods {

    private static String[] names = new String[0];
    static {
        initializeMethods();
    }
    
    private MBeanServerConnectionMethods() {
        //disallow
    }
    
    private static void initializeMethods() {
        try {
            final Method[] methods = javax.management.MBeanServerConnection.class.getDeclaredMethods();
            names = new String[methods.length];
            for (int i = 0 ; i < methods.length ; i++) {
                indexMethod(methods[i]);
            }    
        }
        catch(final Exception e) {
            throw new RuntimeException (e);
        }
    }
    
    private static void indexMethod(final Method am) {
        final String m = am.getName();
        if ("getAttribute".equals(m))
            names[MBeanServerRequestMessage.GET_ATTRIBUTE] = am.toString();
        else if ("getAttributes".equals(m))
            names[MBeanServerRequestMessage.GET_ATTRIBUTES] = am.toString();
        else if ("getDefaultDomain".equals(m))
            names[MBeanServerRequestMessage.GET_DEFAULT_DOMAIN] = am.toString();
        else if ("getDomains".equals(m))
            names[MBeanServerRequestMessage.GET_DOMAINS] = am.toString();
        else if ("getMBeanCount".equals(m))
            names[MBeanServerRequestMessage.GET_MBEAN_COUNT] = am.toString();
        else if ("getMBeanInfo".equals(m))
            names[MBeanServerRequestMessage.GET_MBEAN_INFO] = am.toString();
        else if ("getObjectInstance".equals(m))
            names[MBeanServerRequestMessage.GET_OBJECT_INSTANCE] = am.toString();
        else if ("invoke".equals(m))
            names[MBeanServerRequestMessage.INVOKE] = am.toString();
        else if ("isInstanceOf".equals(m))
            names[MBeanServerRequestMessage.IS_INSTANCE_OF] = am.toString();
        else if ("isRegistered".equals(m))
            names[MBeanServerRequestMessage.IS_REGISTERED] = am.toString();
        else if ("queryMBeans".equals(m))
            names[MBeanServerRequestMessage.QUERY_MBEANS] = am.toString();
        else if ("queryNames".equals(m))
            names[MBeanServerRequestMessage.QUERY_NAMES] = am.toString();
        else if ("setAttribute".equals(m))
            names[MBeanServerRequestMessage.SET_ATTRIBUTE] = am.toString();
        else if ("setAttributes".equals(m))
            names[MBeanServerRequestMessage.SET_ATTRIBUTES] = am.toString();
        else if ("unregisterMBean".equals(m))
            names[MBeanServerRequestMessage.UNREGISTER_MBEAN] = am.toString();
        else if ("addNotificationListener".equals(m))
            indexAddNotificationListenerMethod(am);
        else if ("createMBean".equals(m))
            indexCreateMBeanMethod(am);
        else if ("removeNotificationListener".equals(m))
            indexRemoveNotificationListenerMethod(am);
    }
    
    private static void indexAddNotificationListenerMethod(final Method m) {
        final Class[] params = m.getParameterTypes(); //this is ordered list, has to have 4 elements.
        //indexing on the second parameter - it can either be ObjectName or NotificationListener
        if (params[1].getName().indexOf("ObjectName") != -1)
            names[MBeanServerRequestMessage.ADD_NOTIFICATION_LISTENER_OBJECTNAME] = m.toString();
        else {
            assert (params[1].getName().indexOf("NotificationListener")) != -1;
            names[MBeanServerRequestMessage.ADD_NOTIFICATION_LISTENERS] = m.toString();
        }
    }
    private static void indexCreateMBeanMethod(final Method m) {
        final Class[] params = m.getParameterTypes(); //this is ordered list, has to have 2, 3, 4 or 5 elements.
        final int n = params.length;
        if (n == 2)
            names[MBeanServerRequestMessage.CREATE_MBEAN] = m.toString();
        else if (n == 3)
            names[MBeanServerRequestMessage.CREATE_MBEAN_LOADER] = m.toString();
        else if (n == 4)
            names[MBeanServerRequestMessage.CREATE_MBEAN_PARAMS] = m.toString();
        else //has to be 5
            names[MBeanServerRequestMessage.CREATE_MBEAN_LOADER_PARAMS] = m.toString();
    }
    private static void indexRemoveNotificationListenerMethod(final Method m) {
        final Class[] params = m.getParameterTypes(); //this is ordered list, has to have 2, or 4 elements.
        final int n = params.length;
        if (n == 2) {
            if (params[1].getName().indexOf("ObjectName") != -1)
                names[MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_OBJECTNAME] = m.toString();
            else
                names[MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER] = m.toString();
        }
        else {// has to be 4
            if (params[1].getName().indexOf("ObjectName") != -1)
                names[MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK] = m.toString();
            else
                names[MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK] = m.toString();            
        }
    }
    String getName(final int id) {
        return ( names[id] );
    }
}
