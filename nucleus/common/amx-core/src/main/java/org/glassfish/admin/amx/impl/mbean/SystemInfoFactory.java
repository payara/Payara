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
package org.glassfish.admin.amx.impl.mbean;

import javax.management.MBeanServer;

/**
Factory to create the org.glassfish.admin.amx.base.SystemInfo implementation.
For now, only one implementation instance is allowed.
 */
public final class SystemInfoFactory {

    static SystemInfoImpl INSTANCE = null;

    /**
    Return the actual implementation class, because some method(s) are needed internal to the
    server, but not appropriate for the MBean clients.

    @return the SystemInfoImpl, *or null if not yet initialized*
     */
    public static synchronized SystemInfoImpl getInstance() {
        return INSTANCE;
    }

    /**
    Create the singleton instance.  Intended for exclusive use by the appropriate code
    to initialize once at startup.
     */
    public static synchronized SystemInfoImpl createInstance(final MBeanServer server) {
        if (INSTANCE == null) {
            INSTANCE = new SystemInfoImpl(server);

            new SystemInfoIniter(server, INSTANCE).init();
        } else {
            //throw new RuntimeException("can only initialize once--bug");
        }
        return INSTANCE;
    }

    public static synchronized void removeInstance() {
        if(INSTANCE != null) {
            INSTANCE = null;
        }
    }
}








