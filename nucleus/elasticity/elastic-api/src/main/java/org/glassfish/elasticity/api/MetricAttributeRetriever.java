package org.glassfish.elasticity.api;

public interface MetricAttributeRetriever<V> {

	public <T> T getAttribute(String name, V value, Class<T> type);
	
}
