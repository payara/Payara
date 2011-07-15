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

package org.glassfish.admin.amxtest.ext.wsmgmt;

import com.sun.appserv.management.ext.wsmgmt.WebServiceEndpointInfo;
import com.sun.appserv.management.ext.wsmgmt.WebServiceMgr;
import com.sun.appserv.management.j2ee.WebServiceEndpoint;
import com.sun.appserv.management.monitor.WebServiceEndpointMonitor;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 */
public final class WebServiceMgrTest
        extends AMXTestBase {

    public WebServiceMgrTest() {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(false);
    }

    public void testGetWebServiceMgr() {
        assert (getDomainRoot().getWebServiceMgr() != null);
    }

    public void testGetWebServiceNames() {
        java.util.Map m = null;

        m = getDomainRoot().getWebServiceMgr().getWebServiceEndpointKeys();

        if (m == null) {
            System.out.println("No web services found ");
            return;
        }

        System.out.println("Number of web services " + m.keySet().size());
        System.out.println("Fully qualified names...");
        for (Iterator iter = m.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            System.out.println("Looking for runtime objects for " + key);
            Set<WebServiceEndpoint> epSet =
                    getDomainRoot().getWebServiceMgr().getWebServiceEndpointSet(key,
                                                                                "server");
            if (epSet != null) {
                System.out.println("Found " + epSet.size() + " for " + key);
                for (Iterator epItr = epSet.iterator(); epItr.hasNext();) {
                    WebServiceEndpoint ep = (WebServiceEndpoint) epItr.next();
                    System.out.println("Found " + ep.getName());
                    WebServiceEndpointMonitor epm = (WebServiceEndpointMonitor)
                            ep.getMonitoringPeer();
                    System.out.println("Monitoing peer for  " + ep.getName() +
                            " is " + epm);

                }
            }
        }
        System.out.println("Display names...");
        for (Iterator iter = m.values().iterator(); iter.hasNext();) {
            System.out.println((String) iter.next());
        }
        assert (true);
    }

    public void testGetWebServiceInfo() {
        Map<Object, String> m = null;

        m = getDomainRoot().getWebServiceMgr().getWebServiceEndpointKeys();

        if (m == null) {
            System.out.println("No web services found ");
            return;
        }

        System.out.println("Number of web services " + m.keySet().size());
        System.out.println("Fully qualified names...");
        for (final Object fqn : m.keySet()) {
            System.out.println("Info for web service " + fqn);

            final WebServiceEndpointInfo info =
                    getDomainRoot().getWebServiceMgr().getWebServiceEndpointInfo(fqn);

            /*
            System.out.println("Keys are  " + propMap.keySet().size());
            for( final String key : infos.keySet() )
            {
                System.out.println( key );
            }
            
            System.out.println("Values are  ");
            for( final WebServiceEndpointInfo info : infos.values() )
            {
                 System.out.println( obj.toString() );
            }
            */
        }
    }

    /**
     Tests to see if any RegistryLocations are present.
     Expects to see atleast one, else the test fails. Create a connection
     pool with a type javax.xml.registry.ConnectionFactory
     */
    public void testListRegistryLocations() {
        String[] list = getDomainRoot().getWebServiceMgr().listRegistryLocations();
        if (list == null) {
            fail("Did not get any registry locations. Please check you have " +
                    "created one with the name foo");
        } else {
            for (int i = 0; i < list.length; i++) {
                System.out.println("RegistryLocation = " + list[i]);
            }
            // if you get any names in the connection definition, pass the test
            assert (true);
        }
    }

    public void testAddRegistryConnectionResources() {
        String jndiname = "eis/SOAR";
        String description = "Duh";
        String purl = "http://publishurl";
        String qurl = "http://queryurl";
        Map<String, String> map = new HashMap<String, String>();
        map.put(WebServiceMgr.QUERY_URL_KEY, qurl);
        map.put(WebServiceMgr.PUBLISH_URL_KEY, purl);

        //getDomainRoot().getWebServiceMgr().addRegistryConnectionResources (jndiname, description, 
        //       map);
        assertTrue(true);
    }

    public void testRemoveRegistryConnectionResources() {
        String jndiname = "eis/SOAR";
        getDomainRoot().getWebServiceMgr().removeRegistryConnectionResources(jndiname);
        assertTrue(true);
    }
}


