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

import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.ClusterSupportRequired;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import java.util.HashMap;
import java.util.Map;

/**
 Unit test class to create-delete lb-config and load-balancer elements
 */
public final class LBTest
        extends AMXTestBase
        implements ClusterSupportRequired {

    final boolean runDels = true;
    final boolean runCreates = true;
    final boolean testGetLoadBalancerConfigMap = false;

    public LBTest() {}

    public void testCreateLBConfig() {
        if (checkNotOffline("testDeleteLBConfig")) {
            if (!runCreates) {
                return;
            }
            String name = "test-lb-config";
            boolean monitoringEnabled = true;
            boolean routeCookieEnabled = false;
            boolean httpsRouting = false;
            String responseTimeout = "130";
            String reloadInterval = "380";

            Map<String, String> params = new HashMap<String, String>();
            //params.put("name", name);
            params.put("route-cookie-enabled", "" + routeCookieEnabled);
            params.put("monitoring-enabled", "" + monitoringEnabled);
            params.put("https-routing", "" + httpsRouting);
            params.put("response-timeout-in-seconds", responseTimeout);
            params.put("reload-poll-interval-in-seconds", reloadInterval);

            try {
                getDomainConfig().getLBConfigsConfig().createLBConfig(name, params);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void testCreateLoadBalancerConfig() {
        if (checkNotOffline("testDeleteLBConfig")) {
            if (!runCreates) {
                return;
            }
            String name = "test-load-balancer";
            String lbConfigName = "test-lb-config";
            boolean autoApplyEnabled = true;
            Map<String, String> optional = null;
            try {
                getDomainConfig().getLoadBalancersConfig().createLoadBalancerConfig(name, lbConfigName, autoApplyEnabled, optional);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void testGetLBConfigMap() {
        try {
            Map map = getDomainConfig().getLBConfigsConfig().getLBConfigMap();
            //System.out.println("Here is a list of Load Balancer Config MBeans in DAS: ");
            //System.out.println(map);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void testGetLoadBalancerConfigMap() {
        if (testGetLoadBalancerConfigMap == false) {
            return;
        }
        try {
            Map map = getDomainConfig().getLoadBalancersConfig().getLoadBalancerConfigMap();
            //System.out.println("Here is a list of Load Balancer Config MBeans in DAS: ");
            //System.out.println(map);
            ObjectName objName = new ObjectName("amx:j2eeType=X-LoadBalancerConfig,name=test-load-balancer");
            MBeanInfo minfo = getConnection().getMBeanInfo(objName);
            MBeanAttributeInfo[] mattrsinfo = minfo.getAttributes();
            /*
            for (MBeanAttributeInfo mattrinfo : mattrsinfo) 
            {
                System.out.println("Attribute Name is : " + mattrinfo.getName());
            }
            */

            String[] attrsNames = (String[]) getConnection().getAttribute(objName, "AttributeNames");
            /*for (String attrName : attrsNames) 
                System.out.println("Actual Attribute Name is : " + attrName);            
            System.out.println("AttributeNames are : " + attrsNames);
            System.out.println("MBeanInfo is \n"+minfo);*/

            String attrName = (String) getConnection().getAttribute(objName, "LbConfigName");
            System.out.println("attrName is = " + attrName);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void testDeleteLoadBalancerConfig() {
        if (checkNotOffline("testDeleteLBConfig")) {

            if (!runDels) {
                return;
            }
            String name = "test-load-balancer";
            try {
                getDomainConfig().getLoadBalancersConfig().removeLoadBalancerConfig(name);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void testDeleteLBConfig() {
        if (checkNotOffline("testDeleteLBConfig")) {

            if (!runDels) {
                return;
            }
            String name = "test-lb-config";
            try {
                getDomainConfig().getLBConfigsConfig().removeLBConfig(name);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
