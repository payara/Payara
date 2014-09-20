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
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/ext/wsmgmt/WebServiceMonitorTest.java,v 1.7 2007/05/05 05:24:04 tcfujii Exp $
* $Revision: 1.7 $
* $Date: 2007/05/05 05:24:04 $
*/
package org.glassfish.admin.amxtest.ext.wsmgmt;

import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.j2ee.statistics.NumberStatistic;
import com.sun.appserv.management.monitor.WebServiceEndpointMonitor;
import com.sun.appserv.management.monitor.statistics.WebServiceEndpointAggregateStats;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import org.glassfish.j2ee.statistics.CountStatistic;
import java.io.IOException;
import java.util.Set;

/**
 */
public final class WebServiceMonitorTest
        extends AMXTestBase {

    public WebServiceMonitorTest()
            throws IOException {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(false);
    }

    public void testMonitorMBeans() {
        assert (getDomainRoot().getWebServiceMgr() != null);

        final Set<WebServiceEndpointMonitor> ms =
                getDomainRoot().getQueryMgr().queryJ2EETypeSet(XTypes.WEBSERVICE_ENDPOINT_MONITOR);

        for (final WebServiceEndpointMonitor m : ms) {
            System.out.println("\n \n Name of web service is " + m.getName());

            final WebServiceEndpointAggregateStats s =
                    m.getWebServiceEndpointAggregateStats();

            // verify that we can get each Statistic;
            // there was a problem at one time


            final CountStatistic r1 = s.getTotalFaults();
            assert (r1 != null);
            System.out.println(" total num fault is " + r1.getCount());

            final CountStatistic r2 = s.getTotalNumSuccess();
            assert (r2 != null);
            System.out.println(" total num success is " + r2.getCount());

            final CountStatistic r3 = s.getAverageResponseTime();
            assert (r3 != null);
            System.out.println("avg resp is " + r3.getCount());

            final NumberStatistic c1 = s.getThroughput();
            assert (c1 != null);
            System.out.println(" through put is " + c1.getCurrent());

            final CountStatistic c2 = s.getTotalAuthFailures();
            assert (c2 != null);
            System.out.println(" total num auth success is " + c2.getCount());

            final CountStatistic c3 = s.getTotalAuthSuccesses();
            assert (c3 != null);
            System.out.println(" total num auth failure is " + c3.getCount());

        }
    }
}


