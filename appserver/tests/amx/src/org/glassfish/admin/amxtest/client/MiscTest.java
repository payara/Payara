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

package org.glassfish.admin.amxtest.client;

import com.sun.appserv.management.base.NotificationService;
import com.sun.appserv.management.base.NotificationServiceMgr;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.util.misc.ExceptionUtil;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import java.io.IOException;


/**
 */
public final class MiscTest
        extends AMXTestBase {
    public MiscTest()
            throws IOException {
    }

    public void
    testMBeanInfo() {
        final MBeanInfo info = new MBeanInfo(
                "foo.bar",
                null,
                null,
                null,
                null,
                null);

        assert (info.getNotifications() != null);
        assert (info.getOperations() != null);
        assert (info.getAttributes() != null);
        assert (info.getConstructors() != null);
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(true);
    }

    /**
     Hangs were occuring in getPropertyNames().  Repeatedly invoke it to see if the hang
     can be reproduced.
     public void
     testGetPropertyNames()
     throws ClassNotFoundException
     {
     final Set	s	= getQueryMgr().queryInterfaceSet( PropertiesAccess.class.getName(), null );

     for( int i = 0; i < 5000; ++i )
     {
     final Iterator iter	= s.iterator();
     while ( iter.hasNext() )
     {
     final PropertiesAccess	pa	= (PropertiesAccess)iter.next();

     pa.getPropertyNames();
     }
     }
     }
     */


    /**
     Verify that when an MBean is removed, the proxy
     throws an InstanceNotFoundException.  This test is included here because
     it otherwise causes problems when running other unit tests that want to operate
     on all MBeans--this test creates and removes one, which causes the other
     tests to fail.
     */
    public void
    testProxyDetectsMBeanRemoved()
            throws InstanceNotFoundException {
        // use the NotificationServiceMgr as a convenient way of making
        // an MBean (a NotificationService) come and go.
        final NotificationServiceMgr mgr = getDomainRoot().getNotificationServiceMgr();

        final NotificationService ns = mgr.createNotificationService("UserData", 10);
        assert (ns.getUserData().equals("UserData"));
        final ObjectName nsObjectName = Util.getObjectName(ns);

        mgr.removeNotificationService(ns.getName());
        try {
            // all calls should fail
            Util.getObjectName(ns);
            ns.getName();
            ns.getUserData();
            failure("expecting exception due to missing MBean");
        }
        catch (Exception e) {
            // root cause should be an InstanceNotFoundException containing the ObjectName
            final Throwable t = ExceptionUtil.getRootCause(e);
            assert (t instanceof InstanceNotFoundException);
            final InstanceNotFoundException inf = (InstanceNotFoundException) t;

            final String msg = inf.getMessage();
            final int objectNameStart = msg.indexOf("amx:");
            final String objectNameString = msg.substring(objectNameStart, msg.length());

            final ObjectName on = Util.newObjectName(objectNameString);

            assert (on.equals(nsObjectName));
		}
	}
	


}


