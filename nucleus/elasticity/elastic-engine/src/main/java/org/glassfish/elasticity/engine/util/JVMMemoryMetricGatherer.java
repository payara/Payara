/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.elasticity.engine.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.TimeUnit;

import org.glassfish.elasticity.api.MetricAttributeRetriever;
import org.glassfish.elasticity.api.MetricEntry;
import org.glassfish.elasticity.util.Average;
import org.glassfish.elasticity.util.SimpleMetricHolder;
import org.glassfish.hk2.PostConstruct;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jvm_memory")
public class JVMMemoryMetricGatherer
	extends SimpleMetricHolder<JVMMemoryMetricEntry>
		implements PostConstruct, Runnable {

	@Inject
	ElasticEngineThreadPool tpool;

	public JVMMemoryMetricGatherer() {
		super("jvm_memory", JVMMemoryMetricEntry.class);
	}

	public void postConstruct() {
		tpool.scheduleAtFixedRate(this, 15, 15, TimeUnit.SECONDS);
	}

	public void run() {
		MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage memUsage = memBean.getHeapMemoryUsage();
		try {
		super.add(
				System.currentTimeMillis(),
				new JVMMemoryMetricEntry(memUsage.getUsed(), memUsage
						.getCommitted(), memUsage.getMax()));

		Average<Number> avg = new Average<Number>();
		for (MetricEntry<JVMMemoryMetricEntry> e : super.values(1, TimeUnit.MINUTES)) {
			avg.visit((Long) e.geAttribute("used", Long.class));
		}

//		System.out.println("Avg mem for the last one minute: "
//				+ avg.value());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public String toString() {
		return "JVMMemoryMetricGatherer";
	}

//
//	private class JVMMetricAttributeRetriever implements
//			MetricAttributeRetriever<JVMMemoryMetricEntry> {
//
//		public Long getAttribute(String name, JVMMemoryMetricEntry value,
//				Class<Long> type) {
//			switch (name.charAt(0)) {
//			case 'u':
//				System.out.println("Mem.used: " + value.used);
//				return value.used;
//			case 'c':
//				return value.committed;
//			default:
//				return value.max;
//			}
//
//		}
//
//	}

	

}

class JVMMemoryMetricEntry {

	long used;

	long committed;

	long max;

	JVMMemoryMetricEntry(long used, long committed, long max) {
		this.used = used;
		this.committed = committed;
		this.max = max;
	}

	public long getUsed() {
		return used;
	}

	public long getCommitted() {
		return committed;
	}

	public long getMax() {
		return max;
	}
	
}