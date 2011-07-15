/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.j2ee.statistics.CountStatistic;
import org.glassfish.j2ee.statistics.TimeStatistic;

import com.sun.enterprise.admin.monitor.stats.CountStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableCountStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.TimeStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableTimeStatisticImpl;


/**
 * A Class for providing stats for Bean Methods
 *  Used by both Entity, Stateless and Stateful Container
 *
 * @author Mahesh Kannan
 */

public class EJBMethodStatsImpl
    extends StatsImpl
    implements com.sun.enterprise.admin.monitor.stats.EJBMethodStats
{
    private MethodMonitor delegate;
    private MutableCountStatisticImpl executionStat;
    private MutableCountStatisticImpl errorStat;
    private MutableTimeStatisticImpl  methodStat;
    private MutableCountStatisticImpl successStat;

    public EJBMethodStatsImpl(MethodMonitor delegate) {
	this.delegate = delegate;

	initialize();
    }

    private void initialize() {
	super.initialize("com.sun.enterprise.admin.monitor.stats.EJBMethodStats");

	executionStat = new MutableCountStatisticImpl(
		new CountStatisticImpl("ExecutionTime", "Milliseconds" ));
	errorStat = new MutableCountStatisticImpl(
		new CountStatisticImpl("TotalNumErrors"));

	long now = System.currentTimeMillis();
	methodStat = new MutableTimeStatisticImpl(
		new TimeStatisticImpl(0, 0, 0, 0,
		    "MethodStatistic", "", "", now, now));
	successStat = new MutableCountStatisticImpl(
		new CountStatisticImpl("TotalNumSuccess"));

	delegate.setMutableTimeStatisticImpl(methodStat);
    }

    public CountStatistic getExecutionTime() {
	executionStat.setCount(delegate.getExecutionTime());
	return (CountStatistic) executionStat.modifiableView();
    }

    public CountStatistic getTotalNumErrors() {
	errorStat.setCount(delegate.getTotalNumErrors());
	return (CountStatistic) errorStat.modifiableView();
    }

    public TimeStatistic getMethodStatistic() {
	return (TimeStatistic) methodStat.modifiableView();
    }

    public CountStatistic getTotalNumSuccess() {
	successStat.setCount(delegate.getTotalNumSuccess());
	return (CountStatistic) successStat.modifiableView();
    }

    protected void resetAllStats() {
	//Implementations must override this to reset stat values

	executionStat.reset();
	errorStat.reset();
	synchronized (methodStat) {
	    methodStat.reset();
	}
	successStat.reset();

	delegate.resetAllStats(monitorOn);
    }

}
