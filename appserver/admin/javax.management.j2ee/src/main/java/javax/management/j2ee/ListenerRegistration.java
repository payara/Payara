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

package javax.management.j2ee;

import java.rmi.RemoteException;
import javax.management.NotificationListener;
import javax.management.NotificationFilter;
import javax.management.ObjectName;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;

/**
 * ListenerRegistration defines the methods which clients of the MEJB
 * use to add and remove event listeners. 
 *
 * @author Hans Hrasna
 */
public interface ListenerRegistration extends java.io.Serializable {

     /**
     * Add a listener to a registered managed object.
     *
     * @param name The name of the managed object on which the listener should be added.
     * @param listener The listener object which will handle the notifications emitted by the registered managed object.
     * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
     * @param handback The context to be sent to the listener when a notification is emitted.
     *
     * @exception InstanceNotFoundException The managed object name provided does not match any of the registered managed objects.
     * @exception RemoteException A communication exception occurred during the execution of a remote method call
     * 
     *
     */
    void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
        throws InstanceNotFoundException, RemoteException;



    /**
     * Remove a listener from a registered managed object.
     *
     * @param name The name of the managed object on which the listener should be removed.
     * @param listener The listener object which will handle the notifications emitted by the registered managed object.
     * This method will remove all the information related to this listener.
     *
     * @exception InstanceNotFoundException The managed object name provided does not match any of the registered managed objects.
     * @exception ListenerNotFoundException The listener is not registered in the managed object.
     * @exception RemoteException A communication exception occurred during the execution of a remote method call
     * 
     * 
     */
    void removeNotificationListener(ObjectName name, NotificationListener listener)
        throws InstanceNotFoundException, ListenerNotFoundException, RemoteException;
}
