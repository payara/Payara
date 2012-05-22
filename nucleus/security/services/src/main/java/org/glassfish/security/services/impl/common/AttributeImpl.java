package org.glassfish.security.services.impl.common;


import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Set;

import org.glassfish.security.services.api.common.Attribute;


public class AttributeImpl implements Attribute {

	private String name = null;
	private Set<String> values = new TreeSet<String>();
	
	protected AttributeImpl() {}
	
	public AttributeImpl(String name) {
		this.name = name;
	}
	
	public AttributeImpl(String name, String value) {
		this(name);
		addValue(value);
	}
	
	public AttributeImpl(String name, Set<String> values) {
		this(name);
		addValues(values);
	}
	
	public AttributeImpl(String name, String[] values) {
		this(name);
		addValues(values);
	}

	public int getValueCount() { return values.size(); }

	public String getName() { return name; }

	public String getValue() {
		if(getValueCount() == 0) {
			return null;
		}
		Iterator<String> i = values.iterator();
		return i.next();
	}

	public Set<String> getValues() { return values; }

	public String[] getValuesAsArray() { return values.toArray(new String[0]); }
	
	public void addValue(String value) {
		if (value != null && !value.trim().equals("")) {
			values.add(value);
		}
	}
	
	public void addValues(Set<String> values) {
		addValues(values.toArray(new String[0]));
	}
	
	public void addValues(String[] values) {
		for (int i = 0; i < values.length; i++) {
			addValue(values[i]);
		}
	}
	
	public void removeValue(String value) {
		values.remove(value);
	}
	
	public void removeValues(Set<String> values) {
		this.values.remove(values);
	}
	
	public void removeValues(String[] values) {
		this.values.remove(Arrays.asList(values));
	}
	
	public void clear() {
		values.clear();
	}
	
}
