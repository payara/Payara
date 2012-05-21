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
package org.glassfish.elasticity.util;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import org.glassfish.elasticity.api.MetricAttributeRetriever;
import org.glassfish.elasticity.metric.TabularMetricAttribute;
import org.glassfish.elasticity.metric.TabularMetricEntry;


/**
 * A MetricHolder that maintains the data set in a skip list map
 * 
 * @author Mahesh.Kannan@Oracle.Com
 */
public class TabularMetricHolder<V>
	implements TabularMetricAttribute<V> {

	private final ConcurrentSkipListMap<Long, MetricEntryHolder> _emptyMap =
			new ConcurrentSkipListMap<Long, MetricEntryHolder>();
	
    private ConcurrentSkipListMap<Long, MetricEntryHolder> map =
    		new ConcurrentSkipListMap<Long, MetricEntryHolder>();

    private String metricName;
    
    private Class<V> valueClass;

    private MetricAttributeRetriever<V> metricAttributeRetriever;

    private String[] columnNames;

	public TabularMetricHolder(String metricName, Class<V> valueClass) {
		this.metricName = metricName;
		this.valueClass = valueClass;
		this.metricAttributeRetriever = createMetricAttributeRetriever();

        ArrayList<String> colNames = new ArrayList<String>();
        for (Method m : valueClass.getDeclaredMethods()) {
            if (m.getName().startsWith("get") && m.getParameterTypes().length == 0) {
                String attrName = m.getName().substring(3);
                attrName = ("" + attrName.charAt(0)).toLowerCase() + attrName.substring(1);
                colNames.add(attrName);
            }
        }

        columnNames = colNames.toArray(new String[colNames.size()]);
	}
	
	public String getName() {
		return metricName;
	}

    @Override
    public String[] getColumnNames() {
        return columnNames;
    }

    @Override
    public Object getValue() {
        return map;
    }

    public void add(long timeStamp, V v) {
        this.add(timeStamp, TimeUnit.MILLISECONDS, v);
    }
    
    public void add(long timeStamp, TimeUnit unit, V v) {
    	long ts = TimeUnit.MILLISECONDS.convert(timeStamp, unit);
        map.put(ts, createMetricEntryHolder(ts, v));
    }

	public Class<V> getValueClass() {
		return valueClass;
	}
	
    public String getMetricType() {
    	return metricName;
    }
    
    public boolean isEmpty() {
        return map.isEmpty();
    }
	
	public Iterator<TabularMetricEntry<V>> iterator(long duration, TimeUnit unit) {
        Collection<TabularMetricEntry<V>> data = new ArrayList<TabularMetricEntry<V>>();
        try {
            NavigableMap<Long, MetricEntryHolder> subMap = getView(duration, unit, true);
            for (MetricEntryHolder entry : subMap.values()) {
                data.add(entry);
            }
        } catch (NotEnoughMetricDataException ex) {
            throw ex;
        }

        return data.iterator();
	}

	public Iterator<TabularMetricEntry<V>> iterator(long duration, TimeUnit unit, boolean allowPartialData)
        throws NotEnoughMetricDataException {
		return new SubMapValueIterator(getView(duration, unit, allowPartialData));
	}
	
    public void purgeEntriesOlderThan(long duration, TimeUnit unit)
    	throws UnsupportedOperationException {
        getOldestView(duration, unit).clear();
    }

    public Iterator<TabularMetricEntry<V>> idleEntries(long duration, TimeUnit unit) {
        Collection<TabularMetricEntry<V>> data = new ArrayList<TabularMetricEntry<V>>();
        try {
            NavigableMap<Long, MetricEntryHolder> subMap = getOldestView(duration, unit);
            for (MetricEntryHolder entry : subMap.values()) {
                data.add(entry);
            }
        } catch (NotEnoughMetricDataException ex) {
            //throw ex;
        }

        return data.iterator();
    }

    protected MetricEntryHolder createMetricEntryHolder(long ts, V v) {
    	return new MetricEntryHolder(ts, v);
    }

    protected MetricAttributeRetriever<V> createMetricAttributeRetriever() {
		return new ReflectiveAttributeRetriever(valueClass);
    }
    
    private NavigableMap<Long, MetricEntryHolder> getView(long duration, TimeUnit unit, boolean allowPartialData)
        throws NotEnoughMetricDataException {
        NavigableMap<Long, MetricEntryHolder> result = null;
        if (! map.isEmpty()) {
	        Long maxKey = map.lastKey();
	        if (maxKey != null) {
	            Long minKey = map.floorKey(maxKey - TimeUnit.MILLISECONDS.convert(duration, unit));
	            if (minKey != null) {
	                result = map.subMap(minKey, true, maxKey, true);
	            } else if (allowPartialData) {
                    minKey = map.ceilingKey(maxKey - TimeUnit.MILLISECONDS.convert(duration, unit));
	                result = map.subMap(minKey, true, maxKey, true);
                }
	        }
    	}

        if (result == null) {
            throw new NotEnoughMetricDataException("Not enough metric data for "  + duration + " " + unit);
        }

        NavigableMap<Long, MetricEntryHolder> view =
                new ConcurrentSkipListMap<Long, MetricEntryHolder>();
        for (Long k : result.keySet()) {
            view.put(k, result.get(k));
        }

        return view;
    }
    
    private NavigableMap<Long, MetricEntryHolder> getOldestView(long duration, TimeUnit unit) {
    	NavigableMap<Long, MetricEntryHolder> result = _emptyMap;
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
	
	protected class MetricEntryHolder
		implements TabularMetricEntry<V> {

		private long ts;
		private V v;
		
		public MetricEntryHolder(long ts, V v) {
			this.ts = ts;
			this.v = v;
		}

		public long getTimestamp(TimeUnit sourceUnit) {
			return sourceUnit.convert(ts, TimeUnit.MILLISECONDS);
		}

		public long getTimestamp() {
			return getTimestamp(TimeUnit.MILLISECONDS);
		}

		public V getV() {
			return v;
		}

        public Object getValue(String attrName) {
            return metricAttributeRetriever.getAttribute(attrName, v);
        }

		public Object geAttribute(String name) {
			return metricAttributeRetriever.getAttribute(name, v);
		}
		
	}

	protected class SubMapValueIterator
		implements Iterable<TabularMetricEntry<V>>, Iterator<TabularMetricEntry<V>> {
		
		Iterator<MetricEntryHolder> iter;
		
		SubMapValueIterator(NavigableMap<Long, MetricEntryHolder> subMap) {
			iter = subMap.values().iterator();
		}
		
		public Iterator<TabularMetricEntry<V>> iterator() {
			return this;
		}
		
		public boolean hasNext() {
			return iter.hasNext();
		}

		public TabularMetricEntry<V> next() {
			return iter.next();
		}

		public void remove() {
			iter.remove();
		}
	}

	private class ReflectiveAttributeRetriever
		implements MetricAttributeRetriever<V> {

		private Class<V> vClazz;
		
		ReflectiveAttributeRetriever(Class<V> vClazz) {
			this.vClazz = vClazz;
		}
		
		public Object getAttribute(String attributeName, V value) {
			try {
				String getterName = "get" + Character.toTitleCase(attributeName.charAt(0)) + attributeName.substring(1);
				Method getter = vClazz.getMethod(getterName , (Class<?>[]) null);
				
				//TODO: Enclose this with doPrivleged
				getter.setAccessible(true);
				return getter.invoke(value, (Object[]) null);
			} catch (Exception ex) {
				throw new IllegalArgumentException("Exception while retrieving attribute: " + attributeName, ex);
			}
		}
		
	}

    private static class NormalizedTabularMetricEntry<V>
        implements TabularMetricEntry<V> {

        TabularMetricEntry<V> delegate;

        NormalizedTabularMetricEntry(TabularMetricEntry<V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public long getTimestamp() {
            return (delegate.getTimestamp() / 10000 * 10000);
        }

        @Override
        public V getV() {
            return delegate.getV();
        }

        @Override
        public Object getValue(String columnName) throws IllegalArgumentException {
            return delegate.getValue(columnName);
        }
    }

}
