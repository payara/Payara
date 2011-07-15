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

package com.sun.ejb.base.stats;

import org.glassfish.j2ee.statistics.RangeStatistic;

import com.sun.enterprise.admin.monitor.stats.BoundedRangeStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableBoundedRangeStatisticImpl;

import com.sun.ejb.spi.stats.SessionBeanStatsProvider;


/**
 * A Class for providing stats for both Stateless and Stateful Containers.
 *
 * @author Mahesh Kannan
 */

public abstract class SessionBeanStatsImpl
    extends EJBStatsImpl
    implements org.glassfish.j2ee.statistics.SessionBeanStats
{
    protected SessionBeanStatsProvider		sessionDelegate;

    private MutableBoundedRangeStatisticImpl	methodReadyCountStat;

    public SessionBeanStatsImpl(SessionBeanStatsProvider delegate, String intfName) {
	super(delegate, intfName);
	this.sessionDelegate = delegate;
    }

    protected void initSessionStats() {
	methodReadyCountStat = new MutableBoundedRangeStatisticImpl(
	    new BoundedRangeStatisticImpl("MethodReadyCount",
		"Count", 0, getMaxReadyCount(), getMinReadyCount()));
    }

    public RangeStatistic getMethodReadyCount() {
	methodReadyCountStat.setCount(sessionDelegate.getMethodReadyCount());
	return (RangeStatistic) methodReadyCountStat.modifiableView();
    }

    protected abstract int getMaxReadyCount();

    protected abstract int getMinReadyCount();
}
