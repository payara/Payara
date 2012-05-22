package org.glassfish.security.services.api.common;


import java.util.Set;

/**
 * The Attribute interface defines an interface for interacting with individual Attributes.  It is a read-only interface.
 * <br>
 * Note that, because the collection used to hold attribute values is a Set, the attribute can be multi-valued.  Each value
 * must be distinct, however -- it is not a bag that can contain multiple instances of the same value.
 */
public interface Attribute {
	
	/**
	 * Get the name of this attribute.
	 * 
	 * @return The name.
	 */
	public String getName();
	
	/**
	 * Get a count of the number of values this attribute has (0-n).
	 * 
	 * @return The value count.
	 */
	public int getValueCount();
	
	/**
	 * Get the first value from the Set of attribute values, or null if the attribute has no values.
	 * This is a shorthand method that should be useful for single-valued attributes, but note that
	 * there are no guarantees about which value is returned in the case that there are multiple values.
	 * The value returned will be whichever value the underlying Set implementation returns first.
	 * 
	 * @return The attribute value.
	 */
	public String getValue();
	
	/**
	 * Get the Set of values for this attribute.  The Set returned is a copy of the original; changes
	 * to this Set will not affect the original.
	 * 
	 * @return The attribute values Set.
	 */
	public Set<String> getValues();
	
	/**
	 * Return the attributes values as a String array.  Note that this array can be zero-length
	 * in the case that there are no values.
	 * 
	 * @return The attribute values array.
	 */
	public String[] getValuesAsArray();
	
	public void addValue(String value);
	public void addValues(Set<String> values);
	public void addValues(String[] values);
	
	public void removeValue(String value);
	public void removeValues(Set<String> values);
	public void removeValues(String[] values);
	
	public void clear();

}
