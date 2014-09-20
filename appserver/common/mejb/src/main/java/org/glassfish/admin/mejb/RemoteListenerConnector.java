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

package org.glassfish.admin.mejb;

import java.rmi.Naming;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/** 
 * RemoteListenerConnectors are instantiated by the ListenerRegistry
 * with the address of the callback port of the RemoteEventListener
 * and then registered with the MBeanServer by the MEJB.
 *
 * @author Hans Hrasna
 */
public final class RemoteListenerConnector implements NotificationListener, java.io.Serializable {
    private static final boolean debug = true;
    private static void debug( final String s ) { if ( debug ) { System.out.println(s); } }

    private final String proxyAddress;	// the RMI address of the remote event listener
    private RemoteEventListener listener = null;  //the remote event listener
    private transient MBeanServer server = null;    // the MBeanServer holds the object this is listening to
                                   		  // which is set when this is registered in the MEJB
    private final String id = hashCode() + ":" + System.currentTimeMillis();

    public RemoteListenerConnector(String address) {
        proxyAddress = address;
    }

    public synchronized void handleNotification(Notification evt, Object h) {
        try {
            debug("RemoteListenerConnector.handleNotification()");
            if (listener == null) {
            	listener = (RemoteEventListener)Naming.lookup(proxyAddress);
            }
            listener.handleNotification(evt, h);
        } catch (java.rmi.RemoteException ce) {
            if (server != null) {
                debug("RemoteListenerConnector.server.removeNotificationListener("+ (ObjectName)evt.getSource() + ", " + this + ")");
                try {
                    server.removeNotificationListener((ObjectName)evt.getSource(), this);
                } catch (javax.management.ListenerNotFoundException e) {
                    debug(toString() + ": " + e); //occurs normally if event was fowarded from J2EEDomain
                } catch (Exception e1) {
                    debug(toString() + ": " + e1);
                }
            }
        } catch (Exception e) {
            debug(toString() + ": " + e);
            if (debug) {
            	try {
                	debug("Naming.list(\"//localhost:1100\")");
                	String [] names = Naming.list("//localhost:1100");
            		for(int x=0;x<names.length;x++) {
                		debug("names["+x+"] = " + names[x]);
            		}
            	} catch(Exception e1) {
                	e1.printStackTrace();
            	}
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setMBeanServer(MBeanServer s) {
        server = s;
    }
}
