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
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/ext/wsmgmt/WebServiceConfigCRTest.java,v 1.4 2007/05/05 05:24:04 tcfujii Exp $
* $Revision: 1.4 $
* $Date: 2007/05/05 05:24:04 $
*/
package org.glassfish.admin.amxtest.ext.wsmgmt;

import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.J2EEApplicationConfig;
import com.sun.appserv.management.config.TransformationRuleConfig;
import com.sun.appserv.management.config.WebServiceEndpointConfig;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 */
public final class WebServiceConfigCRTest
        extends AMXTestBase {

    public WebServiceConfigCRTest()
            throws IOException {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(false);
    }

    public void testConfigCR() {
        assert (getDomainRoot().getWebServiceMgr() != null);

        final Set<J2EEApplicationConfig> s =
                getDomainRoot().getQueryMgr().queryJ2EETypeSet(XTypes.J2EE_APPLICATION_CONFIG);

        final Iterator iter = s.iterator();
        while (iter.hasNext()) {

            J2EEApplicationConfig ac = (J2EEApplicationConfig) iter.next();
            System.out.println("config name is " + ac.getName());

            Map m = ac.getWebServiceEndpointConfigMap();
            int init = m.size();
            System.out.println("WebServiceEndpoints found " + init);
            Iterator itr = m.values().iterator();
            while (itr.hasNext()) {
                WebServiceEndpointConfig wsCfg = (WebServiceEndpointConfig)
                        itr.next();
                System.out.println("WebServiceEndpoint's name " +
                        wsCfg.getName());
            }

            /*
             if ( !( ac.getName().equals("jaxrpc-simple") ) ){
                continue;
             }

            ac.createWebServiceEndpointConfig("remove#me", null);

            m = ac.getWebServiceEndpointConfigMap();             
            int afterCreate  = m.size();
            System.out.println("WebServiceEndpoints found " + afterCreate);

            assert ( init +1== afterCreate);
            */
        }
        assert (true);
    }

    public void testWSConfigCR() {
        assert (getDomainRoot().getWebServiceMgr() != null);

        final Set<WebServiceEndpointConfig> s =
                getDomainRoot().getQueryMgr().queryJ2EETypeSet(
                        XTypes.WEB_SERVICE_ENDPOINT_CONFIG);

        for (final WebServiceEndpointConfig wsc : s) {
            Map m = wsc.getTransformationRuleConfigMap();
            int init = m.size();
            System.out.println("Transformation rules found " + init);
            Iterator itr = m.values().iterator();
            while (itr.hasNext()) {
                TransformationRuleConfig trCfg = (TransformationRuleConfig)
                        itr.next();
                System.out.println("Transformation Rule's name " +
                        trCfg.getName());
            }

            /*

            /* Finish later, creating a transformation rule requires the
             * transformation file to be uploaded to DAS.

            wsc.createTransformationRuleConfig("xrule22",
                "/tmp/req.xsl", false, "request", null);

            m = wsc.getTransformationRuleConfigMap();
            int afterCreate  = m.size();
            System.out.println("Transformation rules found " + afterCreate);

            assert ( init +1== afterCreate);

            wsc.removeTransformationRuleConfig("xrule22");
            m = wsc.getTransformationRuleConfigMap();
            int afterDel  = m.size();
            System.out.println("Transformation rules found " + afterDel);

            assert ( init == afterDel);
            */
        }
        assert (true);
    }

}


