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

/*
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/client/AppserverConnectionSourceTest.java,v 1.5 2007/05/05 05:23:53 tcfujii Exp $
* $Revision: 1.5 $
* $Date: 2007/05/05 05:23:53 $
*/
package org.glassfish.admin.amxtest.client;

import com.sun.appserv.management.client.AppserverConnectionSource;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import java.io.IOException;

/**
 Tests AppserverConnectionSource.
 <p/>
 Note that no actual connect test can be done through normal junit tests since there
 is no host/port available and no guarantee of a running server.  All other aspects
 can be tested.
 */
public final class AppserverConnectionSourceTest
        extends AMXTestBase {
    public AppserverConnectionSourceTest() {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(false);
    }

    private static void
    testConnect(
            final String host,
            final int port,
            final String protocol,
            final String user,
            final String password)
            throws IOException {
        final AppserverConnectionSource source =
                new AppserverConnectionSource(protocol, host, port, user, password, null);

        source.getMBeanServerConnection(true);

    }

    public void
    testConnect()
            throws Exception {
        final String host = (String) getEnvValue("HOST");
        final String port = (String) getEnvValue("PORT");
        final String protocol = (String) getEnvValue("PROTOCOL");
        final String user = (String) getEnvValue("USER");
        final String password = (String) getEnvValue("PASSWORD");

        if (host == null || port == null || protocol == null ||
                user == null || password == null ||
                !AppserverConnectionSource.isSupportedProtocol(protocol)) {
            trace("AppserverConnectionSourceTest: skipped connect test; missing config:" +
                    "host = " + host +
                    ", port = " + port +
                    ", protocol = " + protocol +
                    ", user = " + user +
                    ", password = " + password);
        } else {
            testConnect(host, new Integer(port).intValue(), protocol, user, password);
        }
    }

    private AppserverConnectionSource
    create(final String protocol) {
        return (new AppserverConnectionSource(protocol, "localhost", 9999, "admin", "admin123", null));
    }

    public void
    testCreateS1ASHTTP() {
        create(AppserverConnectionSource.PROTOCOL_HTTP);
    }

    public void
    testCreateRMI() {
        create(AppserverConnectionSource.PROTOCOL_RMI);
    }

    public void
    testCreateIllegal() {
        try {
            create("jmxmp");
        }
        catch (IllegalArgumentException e) {
            // good
        }
    }

    public void
    testToString() {
        create(AppserverConnectionSource.PROTOCOL_RMI).toString();
        create(AppserverConnectionSource.PROTOCOL_HTTP).toString();
    }
}






