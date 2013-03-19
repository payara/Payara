/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.mbeanserver;

import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.hk2.api.ServiceLocator;

import javax.management.MBeanServer;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
Start and stop JMX connectors, base class.
 */
abstract class ConnectorStarter {

    protected static void debug(final String s) {
        System.out.println(s);
    }
    protected final MBeanServer mMBeanServer;
    protected final String mHostName;
    protected final int mPort;
    protected final boolean mSecurityEnabled;
    private final ServiceLocator mHabitat;
    protected final BootAMXListener mBootListener;
    protected volatile JMXServiceURL mJMXServiceURL = null;
    protected volatile JMXConnectorServer mConnectorServer = null;

    public JMXServiceURL getJMXServiceURL() {
        return mJMXServiceURL;
    }

    public String hostname() throws UnknownHostException {
        if (mHostName.equals("") || mHostName.equals("0.0.0.0")) {
            return Util.localhost();
        } else if (mHostName.contains(":") && !mHostName.startsWith("[")) {
            return "["+mHostName+"]";
        }
        return mHostName;
    }

    ConnectorStarter(
            final MBeanServer mbeanServer,
            final String host,
            final int port,
            final boolean securityEnabled,
            final ServiceLocator habitat,
            final BootAMXListener bootListener) {
        mMBeanServer = mbeanServer;
        mHostName = host;
        mPort = port;
        mSecurityEnabled = securityEnabled;
        mHabitat = habitat;
        mBootListener = bootListener;
    }

    abstract JMXConnectorServer start() throws Exception;

    public JMXAuthenticator getAccessController() {

        // we return a proxy to avoid instantiating the jmx authenticator until it is actually
        // needed by the system.
        return new JMXAuthenticator() {

            /**
             * We actually wait for the first authentication request to delegate/
             * @param credentials
             * @return
             */
            public Subject authenticate(Object credentials) {
                // lazy init...
                // todo : lloyd, if this becomes a performance bottleneck, we should cache
                // on first access.
                JMXAuthenticator controller = mHabitat.getService(JMXAuthenticator.class);
                return controller.authenticate(credentials);
            }
        };
    }

    public synchronized void stop() {
        try {
            if (mConnectorServer != null) {
                mConnectorServer.stop();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static protected void ignore(Throwable t) {
        // ignore
    }

    protected boolean isSecurityEnabled() {
        return mSecurityEnabled;
    }
}







