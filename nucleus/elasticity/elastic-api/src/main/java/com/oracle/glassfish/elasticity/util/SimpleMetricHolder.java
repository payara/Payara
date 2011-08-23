/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.glassfish.elasticity.util;

import java.util.Iterator;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import org.jvnet.hk2.annotations.Service;

import com.oracle.glassfish.elasticity.api.MetricEntry;
import com.oracle.glassfish.elasticity.api.MetricHolder;


/**
 * A MetricHolder that maintains the data set in a skip list map
 * 
 * @author Mahesh.Kannan@Oracle.Com
 */
public class SimpleMetricHolder<V extends MetricEntry>
	implements MetricHolder<V> {

	private final ConcurrentSkipListMap<Long, MetricEntryHolder<V>> _emptyMap =
			new ConcurrentSkipListMap<Long, MetricEntryHolder<V>>();
	
    private ConcurrentSkipListMap<Long, MetricEntryHolder<V>> map = new ConcurrentSkipListMap<Long, MetricEntryHolder<V>>();

    private String metricType;
    
    private MetricHolder.MetricAttributeInfo[] infos;
    

	public SimpleMetricHolder(String metricType, MetricHolder.MetricAttributeInfo[] infos) {
		this.metricType = metricType;
		this.infos = infos;
	}
	
    public void add(long timeStamp, V v) {
        this.add(timeStamp, TimeUnit.MILLISECONDS, v);
    }
    
    public void add(long timeStamp, TimeUnit unit, V v) {
    	long ts = TimeUnit.MILLISECONDS.convert(timeStamp, unit);
        map.put(ts, new MetricEntryHolder<V>(ts, v));
    }

    public String getMetricType() {
    	return metricType;
    }
    
    public boolean isEmpty() {
        return map.isEmpty();
    }
	
    public MetricHolder.MetricAttributeInfo[] getMetricAttributeInfos() {
		return infos;
	}
	
	public Iterable<V> values(long duration, TimeUnit unit) {
		return new SubMapValueIterator<V>(getView(duration, unit));
	}
	
    public void purgeEntriesOlderThan(long duration, TimeUnit unit)
    	throws UnsupportedOperationException {
        getOldestView(duration, unit).clear();
    }

    private ConcurrentNavigableMap<Long, MetricEntryHolder<V>> getView(long duration, TimeUnit unit) {
    	ConcurrentNavigableMap<Long, MetricEntryHolder<V>> result = _emptyMap;
    	if (! map.isEmpty()) {
	        Long maxKey = map.lastKey();
	        if (maxKey != null) {
	            Long minKey = map.ceilingKey(maxKey - TimeUnit.MILLISECONDS.convert(duration, unit));
	            if (minKey != null) {
	                result = map.subMap(minKey, true, maxKey, true);
	            }
	        }
    	}
    	
    	return result;
    }
    private ConcurrentNavigableMap<Long, MetricEntryHolder<V>> getOldestView(long duration, TimeUnit unit) {
    	ConcurrentNavigableMap<Long, MetricEntryHolder<V>> result = _emptyMap;
    	if (! map.isEmpty()) {
	        Long maxKey = map.lastKey();
	        if (maxKey != null) {
	            Long minKey = map.floorKey(maxKey - TimeUnit.MILLISECONDS.convert(duration, unit));
	            if (minKey != null) {
	                result = map.subMap(map.firstKey(), true, minKey, true);
	            }
	        }
    	}
    	
    	return result;
    }
	
	protected static class MetricEntryHolder<V extends MetricEntry> {

		long ts;
		V v;
		
		public MetricEntryHolder(long ts, V v) {
			this.ts = ts;
			this.v = v;
		}

		public long getTimestamp(TimeUnit sourceUnit) {
			return sourceUnit.convert(ts, TimeUnit.MILLISECONDS);
		}

		public V getValue() {
			return v;
		}
		
	}

	private static class SubMapValueIterator<V extends MetricEntry>
		implements Iterable<V>, Iterator<V> {

		ConcurrentNavigableMap<Long, MetricEntryHolder<V>> subMap;
		
		Iterator<MetricEntryHolder<V>> iter;
		
		SubMapValueIterator(ConcurrentNavigableMap<Long, MetricEntryHolder<V>> subMap) {
			this.subMap = subMap;
			iter = subMap.values().iterator();
		}
		
		public Iterator<V> iterator() {
			return this;
		}
		
		public boolean hasNext() {
			return iter.hasNext();
		}

		public V next() {
			return iter.next().getValue();
		}

		public void remove() {
			iter.remove();
		}
	}


}
