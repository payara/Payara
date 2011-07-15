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

import java.lang.reflect.Method;

import java.util.ArrayList;

import com.sun.enterprise.admin.monitor.stats.TimeStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableTimeStatisticImpl;

/**
 * A Class for providing stats for an EJB method
 *  All concrete S1AS containers instantiate one instance of
 *  this class for each EJB method. 
 *
 * @author Mahesh Kannan
 */

public class MethodMonitor {

    private String		methodName; 
    private boolean		monitorOn = true;

    private static ThreadLocal	execThreadLocal = new ThreadLocal();
    private Object		lock = new Object();
    private int			successCount = 0;
    private int			errorCount = 0;
    private int			invocationCount = 0;
    private long		totalExecutionTime = 0;

    private MutableTimeStatisticImpl  methodStat;

    public MethodMonitor(Method method, boolean prefixWithClassName) {
        this.methodName = constructMethodName(method);
        if (prefixWithClassName) {
            String prefix = method.getDeclaringClass().getName();
            prefix = prefix.replace('.', '_');
            this.methodName = prefix + "_" + this.methodName;
        }
        this.monitorOn = true;
    }

    void setMutableTimeStatisticImpl(MutableTimeStatisticImpl methodStat) {
	this.methodStat = methodStat;
    }

    public void preInvoke() {
	if (monitorOn) {
	    ArrayList list = (ArrayList) execThreadLocal.get();
	    if (list == null) {
		list = new ArrayList(5);
		execThreadLocal.set(list);
	    }
	    list.add(System.currentTimeMillis());
	    synchronized (lock) {
		invocationCount++;
	    }
	}
    }

    public void postInvoke(Throwable th) {
	if (monitorOn) {
	    ArrayList list = (ArrayList) execThreadLocal.get();
	    if ( (list != null) && (list.size() > 0) ) {
		int index = list.size();
		Long startTime = (Long) list.remove(index-1);
		synchronized(lock) {
		    if (th == null) {
			successCount++;
		    } else {
			errorCount++;
		    }
		    if (startTime != null) {
			long diff = System.currentTimeMillis()
			    - startTime.longValue();
			totalExecutionTime = diff;

			methodStat.incrementCount(diff);
		    }
		}
	    }
	}
    }

    public void resetAllStats(boolean monitorOn) {
	successCount = 0;
	errorCount = 0;
	invocationCount = 0;
	totalExecutionTime = 0;
	this.monitorOn = monitorOn;
    }

    public String getMethodName() {
	return this.methodName;
    }

    public int getTotalInvocations() {
	return invocationCount;
    }

    public long getExecutionTime() {
	return totalExecutionTime;
    }

    public int getTotalNumErrors() {
	return errorCount;
    }

    public int getTotalNumSuccess() {
	return successCount;
    }

    public void appendStats(StringBuffer sbuf) {
	sbuf.append("\n\t[Method ")
	    .append("name=").append(methodName).append("; ")
	    .append("invCount=").append(invocationCount).append("; ")
	    .append("success=").append(successCount).append("; ")
	    .append("errors=").append(errorCount).append("; ")
	    .append("totalTime=").append(totalExecutionTime).append("]");
    }


    private String constructMethodName(Method method) {
	StringBuffer sbuf = new StringBuffer();
	sbuf.append(method.getName());
	Class[] paramTypes = method.getParameterTypes();
	int sz = paramTypes.length;
	if (sz > 0) {
	    String dash = "-";
	    for (int i=0; i<sz; i++) {
		sbuf.append(dash)
		    .append(paramTypes[i].getName());
	    }
	}
	return sbuf.toString();
    }

}
