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

/* CVS information
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/server/callers/MethodCallers.java,v 1.4 2005/12/25 04:26:39 tcfujii Exp $
 * $Revision: 1.4 $
 * $Date: 2005/12/25 04:26:39 $
*/

package com.sun.enterprise.admin.jmx.remote.server.callers;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import javax.management.MBeanServerConnection;

import com.sun.enterprise.admin.jmx.remote.server.notification.ServerNotificationManager;

/** A list of method callers.
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
 */
public class MethodCallers {
    
    /** Creates a new instance of MethodCallerList */
    
    private MethodCallers() {
    }
    
/* BEGIN -- S1WS_MOD */
//    public static Set callers(MBeanServerConnection mbsc) {
    public static Set callers(MBeanServerConnection mbsc, ServerNotificationManager mgr) {
/* END -- S1WS_MOD */
        final Set callers = new HashSet();
/* BEGIN -- S1WS_MOD */
//        buildCallers(callers, mbsc);
        buildCallers(callers, mbsc, mgr);
/* END -- S1WS_MOD */
        return ( Collections.unmodifiableSet(callers) );
    }
    
/* BEGIN -- S1WS_MOD */
//    private static void buildCallers(Set s, MBeanServerConnection mbsc) {
    private static void buildCallers(Set s, MBeanServerConnection mbsc,
                                     ServerNotificationManager mgr) {
/* END -- S1WS_MOD */
        //can use reflection here. but for now building it statically.
/* BEGIN -- S1WS_MOD */
//        s.add(new AddNotifLsnrObjNameCaller(mbsc));
//        s.add(new AddNotifLsnrsCaller(mbsc));
        s.add(new AddNotifLsnrObjNameCaller(mbsc, mgr));
        s.add(new AddNotifLsnrsCaller(mbsc, mgr));
/* END -- S1WS_MOD */
        s.add(new CreateMBeanCaller(mbsc));
        s.add(new CreateMBeanLoaderCaller(mbsc));
        s.add(new CreateMBeanLoaderParamsCaller(mbsc));
        s.add(new CreateMBeanParamsCaller(mbsc));
        s.add(new GetAttributeCaller(mbsc));
        s.add(new GetAttributesCaller(mbsc));
        s.add(new GetDefaultDomainCaller(mbsc));
        s.add(new GetDomainsCaller(mbsc));
        s.add(new GetMBeanCountCaller(mbsc));
        s.add(new GetMBeanInfoCaller(mbsc));
        s.add(new GetObjectInstanceCaller(mbsc));
        s.add(new InvokeCaller(mbsc));
        s.add(new IsInstanceOfCaller(mbsc));
        s.add(new IsRegisteredCaller(mbsc));
        s.add(new QueryMBeansCaller(mbsc));
        s.add(new QueryNamesCaller(mbsc));
/* BEGIN -- S1WS_MOD */
//        s.add(new RemoveNotifLsnrCaller(mbsc));
//        s.add(new RemoveNotifLsnrFilterHandbackCaller(mbsc));
        s.add(new RemoveNotifLsnrCaller(mbsc, mgr));
        s.add(new RemoveNotifLsnrFilterHandbackCaller(mbsc, mgr));
/* END -- S1WS_MOD */
        s.add(new RemoveNotifLsnrObjNameCaller(mbsc));
/* BEGIN -- S1WS_MOD */
//        s.add(new RemoveNotifLsnrObjNameFilterHandbackCaller(mbsc));
        s.add(new RemoveNotifLsnrObjNameFilterHandbackCaller(mbsc, mgr));
/* END -- S1WS_MOD */
        s.add(new SetAttributeCaller(mbsc));
        s.add(new SetAttributesCaller(mbsc));
        s.add(new UnregisterMBeanCaller(mbsc));
    }
}
