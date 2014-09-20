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
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/ext/wsmgmt/WebServiceConfigTest.java,v 1.7 2007/05/05 05:24:04 tcfujii Exp $
* $Revision: 1.7 $
* $Date: 2007/05/05 05:24:04 $
*/
package org.glassfish.admin.amxtest.ext.wsmgmt;

import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.TransformationRuleConfig;
import com.sun.appserv.management.config.WebServiceEndpointConfig;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public final class WebServiceConfigTest
        extends AMXTestBase {

    public WebServiceConfigTest()
            throws IOException {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(false);
    }

    public void testConfigMBeans() {
        assert (getDomainRoot().getWebServiceMgr() != null);

        final Set<WebServiceEndpointConfig> s =
                getDomainRoot().getQueryMgr().queryJ2EETypeSet(
                        XTypes.WEB_SERVICE_ENDPOINT_CONFIG);

        for (final WebServiceEndpointConfig wsc : s) {
            String oldSize = wsc.getMaxHistorySize();
            System.out.println("Old Max History size is " + oldSize);
            System.out.println("Setting Max History size to 1 ");
            wsc.setMaxHistorySize("1");
            System.out.println("New Max History size is  "
                    + wsc.getMaxHistorySize());
            assert ("1".equals(wsc.getMaxHistorySize()));
            System.out.println("Resetting Max History size to " + oldSize);
            wsc.setMaxHistorySize(oldSize);
            System.out.println("Config value is " + wsc.getMonitoringLevel());

            Map m = wsc.getTransformationRuleConfigMap();

            System.out.println("Transformation rules found " + m.size());

            Iterator itr = m.values().iterator();
            while (itr.hasNext()) {
                TransformationRuleConfig tc = (TransformationRuleConfig)
                        itr.next();
                System.out.println("rule name is " + tc.getName());
            }
            System.out.println("Getting tranformation rules in order ");
            List l = wsc.getTransformationRuleConfigList();

            System.out.println("Transformation rules found " + l.size());

            Iterator litr = l.iterator();
            while (litr.hasNext()) {
                TransformationRuleConfig tc = (TransformationRuleConfig)
                        litr.next();
                System.out.println("rule name is " + tc.getName());
            }
        }
        assert (true);
    }
}


