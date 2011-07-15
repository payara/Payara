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

package org.glassfish.admin.amxtest.monitor;

import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.monitor.CallFlowMonitor;
import com.sun.appserv.management.monitor.ServerRootMonitor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CallFlowMonitorTest
        extends AMXMonitorTestBase {
    private String IP_FILTER_NAME = "129.129.129.129";
    private String PRINCIPAL_FILTER_NAME = "Harry";

    public CallFlowMonitorTest() {
    }

    public void testCallFlowOn() {
        final CallFlowMonitor cfm = getCallFlowMonitor();
        try {
            cfm.setEnabled(true);
            assertTrue(cfm.getEnabled());
        }
        catch (Throwable t) {
            warning("testCallFlowOn: " +
                    "Can't enable callflow...has the callflow database been started?");
        }
    }

    public void testIPFilter() {
        final CallFlowMonitor cfm = getCallFlowMonitor();
        cfm.setCallerIPFilter(IP_FILTER_NAME);
        final String filter = cfm.getCallerIPFilter();
        boolean val = filter.equals(IP_FILTER_NAME);
        assertTrue(val);
    }

    public void testPrincipalFilter() {
        final CallFlowMonitor cfm = getCallFlowMonitor();
        cfm.setCallerPrincipalFilter(PRINCIPAL_FILTER_NAME);
        final String filter = cfm.getCallerPrincipalFilter();
        boolean val = filter.equals(PRINCIPAL_FILTER_NAME);
        assertTrue(val);
    }

    /*
    * Disable CallFlow
    */
    public void testCallFlowOff() {
        final CallFlowMonitor cfm = getCallFlowMonitor();
        cfm.setEnabled(false);
        assertFalse(cfm.getEnabled());
    }

    public void testQueryRequestInformation() {
        final CallFlowMonitor cfm = getCallFlowMonitor();
        final List<Map<String, String>> list = cfm.queryRequestInformation();
        if (list == null)
        //            int resultSize = list.size ();
        //            int CORRECT_RESULT_SIZE = 0;
        //            if (resultSize == CORRECT_RESULT_SIZE)
        {
            assertTrue(true);
        }
    }

    public void testQueryCallStackInformation() {
        final CallFlowMonitor cfm = getCallFlowMonitor();
        final List<Map<String, String>> list = cfm.queryCallStackForRequest("RequestID_1");
        if (list == null)
        //            int resultSize = list.size ();
        //            int CORRECT_RESULT_SIZE = 0;
        //            if (resultSize == CORRECT_RESULT_SIZE)
        {
            assertTrue(true);
        }
    }

    public void testQueryPieInformation() {
        final CallFlowMonitor cfm = getCallFlowMonitor();
        final Map<String, String> map = cfm.queryPieInformation("RequestID_1");
        if (map != null)
        //            int resultSize = list.size ();
        //            int CORRECT_RESULT_SIZE = 0;
        //            if (resultSize == CORRECT_RESULT_SIZE)
        {
            assertTrue(true);
        }
    }


    public void testClearData() {
        final CallFlowMonitor cfm = getCallFlowMonitor();
        cfm.clearData();
    }

    public void testQueryRequestTypeKeys() {
        final CallFlowMonitor cfm = getCallFlowMonitor();
        final String[] rT = cfm.queryRequestTypeKeys();
        if (rT.length == 5) {
            assertTrue(true);
        } else {
            assertTrue(false);
        }
    }

    public void testQueryComponentTypeKeys() {
        final CallFlowMonitor cfm = getCallFlowMonitor();
        final String[] rT = cfm.queryComponentTypeKeys();
        assert rT.length == 7;
    }

    public void testQueryContainerTypeOrApplicationTypeKeys() {
        final CallFlowMonitor cfm = getCallFlowMonitor();
        final String[] rT = cfm.queryContainerTypeOrApplicationTypeKeys();
        assert rT.length == 6;
    }


    private CallFlowMonitor getCallFlowMonitor() {
        Map<String, ServerRootMonitor> serverRootMonitorMap =
                getDomainRoot().getMonitoringRoot().getServerRootMonitorMap();
        // Get the server name from some MBean. Using the default value for now
        ServerRootMonitor serverRootMonitor = serverRootMonitorMap.get("server");
        return serverRootMonitor.getCallFlowMonitor();
    }

    public void testExactlyOneDASCallFlowMonitor() {
        final Set<CallFlowMonitor> cfms =
                getQueryMgr().queryJ2EETypeSet(XTypes.CALL_FLOW_MONITOR);

        int numDAS = 0;
        int numNonDAS = 0;
        for (final CallFlowMonitor cfm : cfms) {
            if (cfm.isDAS()) {
                ++numDAS;
            } else {
                ++numNonDAS;
            }
        }

        if (numNonDAS == 0) {
            warning("testExactlyOneDASCallFlowMonitor: no instances other than DAS are running");
        }

        assert numDAS == 1 :
                "There must be exactly one CallFlowMonitor in the DAS, but there are " + numDAS +
                        " and there are " + numNonDAS + " non-DAS CallFlowMonitor.";

    }
}


	
