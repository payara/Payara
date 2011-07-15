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

package com.sun.enterprise.admin.monitor.util;

import org.glassfish.j2ee.statistics.Statistic;
import org.glassfish.j2ee.statistics.CountStatistic;
import org.glassfish.j2ee.statistics.BoundedRangeStatistic;
import org.glassfish.j2ee.statistics.RangeStatistic;
import org.glassfish.j2ee.statistics.TimeStatistic;

/**
 * Provides for String representation of all the Statistic classes.
 * @author  <a href="mailto:Kedar.Mhaswade@sun.com">Kedar Mhaswade</a>
 * @since S1AS8.0
 * @version $Revision: 1.2 $
 */
final class StatisticToString {
	
	private final Statistic stc;
	private final String SEP = ":";
	/** Creates a new instance of StatisticToString for given Statistic. Instances of
	 * this class are immutable. Note that the returned Strings are basically for debug
	 * purposes and should be displayed taking that into account.
	 * @param	c	Statistic instance that needs its string representation from this class
	 */
	StatisticToString(Statistic stc) {
		this.stc = stc;
	}
	
	/**
	 */
	public String toString() {
		final StringBuffer s = new StringBuffer();
		return ( s.append(baseString()).append(SEP).append(specificString()).toString() );
	}
	
	private String baseString() {
		final StringBuffer s = new StringBuffer();
		s.append(stc.getName()).append(SEP).append(stc.getUnit()).append(SEP).
		append(stc.getDescription()).append(stc.getStartTime()).append(stc.getStartTime());
		return (s.toString());
	}
	private String specificString() {
		final StringBuffer s = new StringBuffer();
		if (stc instanceof CountStatistic)
			s.append(countStatisticSpecificString());
		if (stc instanceof RangeStatistic)
			s.append(rangeStatisticSpecificString());
		if (stc instanceof BoundedRangeStatistic)
			s.append(boundedRangeStatisticSpecificString());
		if (stc instanceof TimeStatistic)
			s.append(timeStatisticSpecificString());
		return ( s.toString() );
	}
	private String countStatisticSpecificString() {
		final StringBuffer s = new StringBuffer();
		final CountStatistic cs = (CountStatistic)stc;
		return ( s.append(cs.getCount()).toString() );
	}
	private String rangeStatisticSpecificString() {
		final StringBuffer s = new StringBuffer();
		final RangeStatistic rs = (RangeStatistic)stc;
		return ( s.append(rs.getLowWaterMark()).append(SEP).append(rs.getHighWaterMark()).toString() );
	}
	private String boundedRangeStatisticSpecificString() {
		final StringBuffer s = new StringBuffer();
		final BoundedRangeStatistic bs = (BoundedRangeStatistic)stc;
		return ( s.append(bs.getUpperBound()).append(SEP).append(bs.getLowerBound()).toString() );
	}
	private String timeStatisticSpecificString() {
		final StringBuffer s = new StringBuffer();
		final TimeStatistic ts = (TimeStatistic)stc;
		return ( s.append(ts.getMaxTime()).append(SEP).append(ts.getMinTime()).append(SEP).append(ts.getTotalTime()).toString() );
	}
}
