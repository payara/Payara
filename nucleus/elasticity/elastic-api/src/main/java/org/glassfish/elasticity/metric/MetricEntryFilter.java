package org.glassfish.elasticity.metric;

/**
 * A class that determines if an entry should be added to the collection
 * 	that is iterated over by the TabularMetricAtribute.iterator() method.
 * 
 * @author Mahesh Kannan
 *
 */
public interface MetricEntryFilter {

	/**
	 * Determines if an entry should be added to the collection that is 
	 * iterated over by the TabularMetricAtribute.iterator() method
	 * 
	 * @param entry the filter
	 * 
	 * @return true if the entry can be added to the collection. False if not.
	 */
	public boolean satisfies(TabularMetricEntry entry);
	
}
