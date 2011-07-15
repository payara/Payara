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

package org.glassfish.admin.amxtest.config;


import com.sun.appserv.management.config.AvailabilityServiceConfig;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.ClusterSupportRequired;

import javax.management.InstanceNotFoundException;

import com.sun.appserv.management.helper.AttributeResolverHelper;

public class AvailabilityServiceConfigTest
        extends AMXTestBase
        implements ClusterSupportRequired {
    public AvailabilityServiceConfigTest()
            throws InstanceNotFoundException {
    }

    private AvailabilityServiceConfig
    getIt() {
        return getConfigConfig().getAvailabilityServiceConfig();
    }

    public void
    testWarnAvail() {
        if (getIt() == null) {
            assert false : "AvailabilityServiceConfigTest:  no AvailabilityServiceConfig to test";
        }
    }

    /**
     Test of [g/s]etAvailabilityEnabled method, of class com.sun.appserv.management.config.AvailabilityServiceConfig.
     */
    public void testAvailabilityEnabled() {
        final AvailabilityServiceConfig  asc = getIt();
        if (asc != null) {
            asc.setAvailabilityEnabled("" + false);
            assertFalse("getAvailabilityEnabled() was supposed to return false.", asc.getAvailabilityEnabled().equals("false"));
            asc.setAvailabilityEnabled( ""+ true);
            assertTrue("getAvailabilityEnabled() was supposed to return true.", asc.getAvailabilityEnabled().equals("true"));
        }
    }

    /**
     Test of [g/s]etAutoManageHAStore method, of class com.sun.appserv.management.config.AvailabilityServiceConfig.
     */
    public void testAutoManageHAStore() {
        final AvailabilityServiceConfig  asc = getIt();
        if (asc != null) {
            final String save = asc.getAutoManageHAStore();
            asc.setAutoManageHAStore("" + true);
            assertTrue("getAutoManageHAStore() was supposed to return true.", asc.getAutoManageHAStore().equals("true"));
            
            asc.setAutoManageHAStore("" + false);
            assertFalse("getAutoManageHAStore() was supposed to return false.", asc.getAutoManageHAStore().equals("false"));
            asc.setAutoManageHAStore(save);
        }
    }

    /**
     Test of [g/s]etHAAgentHosts methods, of class com.sun.appserv.management.config.AvailabilityServiceConfig.
     */
    public void testHAAgentHosts() {
        final AvailabilityServiceConfig  asc = getIt();
        if (asc != null) {
            final String hosts = "hp,hp,hp,hp";
            final String save = asc.getHAAgentHosts();
            asc.setHAAgentHosts(hosts);
            String s = asc.getHAAgentHosts();
            assertEquals(hosts, s);
            asc.setHAAgentHosts((save == null) ? "" : save);
        }
    }

    /**
     Test of [g/s]etHAAgentPort methods, of class com.sun.appserv.management.config.AvailabilityServiceConfig.
     */
    public void testHAAgentPort() {
        final AvailabilityServiceConfig  asc = getIt();
        if (asc != null) {
            final String port = "3456";
            final String save = asc.getHAAgentPort();
            asc.setHAAgentPort(port);
            final String s = asc.getHAAgentPort();
            assertEquals(port, s);
            asc.setHAAgentPort((save == null) ? "" : save);
        }
    }

    /**
     Test of [g/s]etHAStoreHealthcheckIntervalSeconds methods, of class com.sun.appserv.management.config.AvailabilityServiceConfig.
     */
    public void testHAStoreHealthcheckIntervalSeconds() {
        final AvailabilityServiceConfig  asc = getIt();
        if (asc != null) {
            final String time = "90";
            final String save = asc.getHAStoreHealthcheckIntervalSeconds();
            asc.setHAStoreHealthcheckIntervalSeconds(time);
            String s = asc.getHAStoreHealthcheckIntervalSeconds();
            assertEquals(time, s);
            asc.setHAStoreHealthcheckIntervalSeconds((save == null) ? "" : save);
        }
    }

    /**
     Test of [g/s]etHAStoreName methods, of class com.sun.appserv.management.config.AvailabilityServiceConfig.
     */
    public void testHAStoreName() {
        final AvailabilityServiceConfig  asc = getIt();
        if (asc != null) {
            final String storeName = "cluster1";
            final String save = asc.getHAStoreName();
            asc.setHAStoreName(storeName);
            final String s = asc.getHAStoreName();
            assertEquals(storeName, s);
            asc.setHAStoreName((save == null) ? "" : save);
        }
    }

    /**
     Test of [g/s]etStorePoolName methods, of class com.sun.appserv.management.config.AvailabilityServiceConfig.
     */
    public void testStorePoolName() {
        final AvailabilityServiceConfig  asc = getIt();
        if (asc!= null) {
            final String storeName = "xxxx";
            final String save = asc.getStorePoolName();
            asc.setStorePoolName(storeName);
            final String s = asc.getStorePoolName();
            assertEquals(storeName, s);
            asc.setStorePoolName((save == null) ? "" : save);
        }
    }

    /**
     Test of [g/s]etHAStoreHealthcheckEnabled methods, of class com.sun.appserv.management.config.AvailabilityServiceConfig.
     */
    public void testHAStoreHealthcheckEnabled() {
        final AvailabilityServiceConfig  asc = getIt();
        if (asc!= null) {
            final String save = asc.getHAStoreHealthcheckEnabled();
            final boolean b = AttributeResolverHelper.resolveBoolean( asc, save);

            asc.setHAStoreHealthcheckEnabled("" + false);
            assertFalse("getHAStoreHealthcheckEnabled() was supposed to return false.",
                AttributeResolverHelper.resolveBoolean( asc, asc.getHAStoreHealthcheckEnabled()));
                
            asc.setHAStoreHealthcheckEnabled("" + true);
            assertTrue("getHAStoreHealthcheckEnabled() was supposed to return true.",
                AttributeResolverHelper.resolveBoolean( asc, asc.getHAStoreHealthcheckEnabled()));
            asc.setHAStoreHealthcheckEnabled( save );
        }
	}
}


