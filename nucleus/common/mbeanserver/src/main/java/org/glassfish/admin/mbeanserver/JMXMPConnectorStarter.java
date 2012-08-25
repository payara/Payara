/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;

import javax.management.remote.*;
import javax.management.remote.jmxmp.JMXMPConnectorServer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
Start and stop JMX connectors.
 */
final class JMXMPConnectorStarter extends ConnectorStarter
{
    JMXMPConnectorStarter(
        final MBeanServer mbeanServer, 
        final String address, 
        final int port, 
        final boolean securityEnabled, 
        final ServiceLocator habitat, 
        final BootAMXListener bootListener)
    {
        super(mbeanServer, address, port, securityEnabled, habitat, bootListener);
    }


    public synchronized JMXConnectorServer start()
    {
        if (mConnectorServer != null)
        {
            return mConnectorServer;
        }

        final boolean tryOtherPorts = false;
        final int TRY_COUNT = tryOtherPorts ? 100 : 1;

        int port = mPort;
        int tryCount = 0;
        while (tryCount < TRY_COUNT)
        {
            try
            {
                mConnectorServer = startJMXMPConnectorServer(port);
                break;
            }
            catch (final java.net.BindException e)
            {
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }

            if (port < 1000)
            {
                port += 1000;   // in case it's a permissions thing
            }
            else
            {
                port = port + 1;
            }
        }
        return mConnectorServer;
    }

    public static final String JMXMP = "jmxmp";

    private JMXConnectorServer startJMXMPConnectorServer(final int port)
        throws MalformedURLException, IOException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException
    {
        final Map<String, Object> env = new HashMap<String, Object>();
        env.put("jmx.remote.protocol.provider.pkgs", "com.sun.jmx.remote.protocol");
        env.put("jmx.remote.protocol.provider.class.loader", this.getClass().getClassLoader());
        JMXAuthenticator authenticator = getAccessController();
        if (authenticator != null)
        {
            env.put("jmx.remote.authenticator", authenticator);
        }

        final JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:" + JMXMP + "://" +hostname() + ":" + port);
        JMXConnectorServer jmxmp = null;

        boolean startedOK = false;
        try
        {
            jmxmp = new JMXMPConnectorServer(serviceURL, env, mMBeanServer);
            if ( mBootListener != null )
            {
                jmxmp.addNotificationListener(mBootListener, null, serviceURL.toString() );
            }

            jmxmp.start();
            startedOK = true;
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            // we do it this way so that the original exeption will be thrown out
            if (!startedOK)
            {
                try
                {
                    if (jmxmp != null)
                    {
                        jmxmp.stop();
                    }
                }
                catch (Exception e)
                {
                    ignore(e);
                }
            }
        }

        mJMXServiceURL  = serviceURL;
        mConnectorServer = jmxmp;

        // verify
        //final JMXConnector jmxc = JMXConnectorFactory.connect(serviceURL, null);
        //jmxc.getMBeanServerConnection().getMBeanCount();
        //jmxc.close();

        return mConnectorServer;
    }
}







