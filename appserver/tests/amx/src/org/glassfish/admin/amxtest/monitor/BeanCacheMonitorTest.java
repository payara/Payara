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
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/monitor/BeanCacheMonitorTest.java,v 1.5 2007/05/05 05:24:05 tcfujii Exp $
* $Revision: 1.5 $
* $Date: 2007/05/05 05:24:05 $
*/
package org.glassfish.admin.amxtest.monitor;

import com.sun.appserv.management.base.QueryMgr;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.monitor.BeanCacheMonitor;
import com.sun.appserv.management.monitor.statistics.EJBCacheStats;

import org.glassfish.j2ee.statistics.BoundedRangeStatistic;
import org.glassfish.j2ee.statistics.CountStatistic;
import java.util.Iterator;
import java.util.Set;


public final class BeanCacheMonitorTest
        extends AMXMonitorTestBase {
    public BeanCacheMonitorTest() {
    }


    public void
    testStats() {
        final QueryMgr q = getQueryMgr();

        final Set beanCacheMonitors = q.queryJ2EETypeSet(XTypes.BEAN_CACHE_MONITOR);

        if (beanCacheMonitors.size() == 0) {
            warning("BeanCacheMonitorTest: no MBeans found to test.");
        } else {
            final Iterator iter = beanCacheMonitors.iterator();

            while (iter.hasNext()) {
                final BeanCacheMonitor m = (BeanCacheMonitor) iter.next();
                final EJBCacheStats s = m.getEJBCacheStats();

                // verify that we can get each Statistic; there was a problem at one time
                final BoundedRangeStatistic b1 = s.getCacheMisses();
                final BoundedRangeStatistic b2 = s.getCacheHits();
                final BoundedRangeStatistic b3 = s.getBeansInCache();

                // these were failing
                final CountStatistic c4 = s.getPassivationSuccesses();
                final CountStatistic c3 = s.getExpiredSessionsRemoved();
                final CountStatistic c2 = s.getPassivationErrors();
                final CountStatistic c1 = s.getPassivations();

            }
        }
    }

}






