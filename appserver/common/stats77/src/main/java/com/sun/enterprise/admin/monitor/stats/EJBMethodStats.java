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

package com.sun.enterprise.admin.monitor.stats;
import org.glassfish.j2ee.statistics.Stats;
import org.glassfish.j2ee.statistics.CountStatistic;
import org.glassfish.j2ee.statistics.TimeStatistic;

/** A Stats interface to represent the statistical data exposed by an EJB Business Method.
 * These are based on the statistics exposed in S1AS7.0. 
 * All the EJB Methods should expose statistical data by implementing this interface.
 * @author Muralidhar Vempaty
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
 */

public interface EJBMethodStats extends Stats {
    
	/** Returns the statistics of the method invocation as an instance of TimeStatistic.
	 * Note that it returns the number of times the operation called, the total time
	 * that was spent during the invocation and so on. All the calculations of the
	 * statistic are being done over time.
	 * @return in instance of {@link TimeStatistic}
	 */
	public TimeStatistic getMethodStatistic();
	
    /** Returns the total number of errors as a CountStatistic. It is upto the method
	 * implementor to characterize what an error is. Generally if an operation results in 
	 * an exception, this count will increment by one.
	 * @return		an instance of {@link CountStatistic}
     */
    public CountStatistic getTotalNumErrors();
    
    /** Returns the total number of successful runs, as a CountStatistic. It is upto the method
	 * implementor to characterize what a successful run is. Generally if an operation returns
	 * normally, this count will increment by one.
	 * @return		an instance of {@link CountStatistic}
     */
    public CountStatistic getTotalNumSuccess();
    
    /** Returns the time spent during the last successful/unsuccessful attempt to execute the operation, as a CountStatistic.
	 * The time spent is generally an indication of the system load/processing time.
	 * @return		an instance of {@link CountStatistic}
     */
    public CountStatistic getExecutionTime();    
}
