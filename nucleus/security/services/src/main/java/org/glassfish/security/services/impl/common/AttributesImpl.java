package org.glassfish.security.services.impl.common;


import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.glassfish.security.services.api.common.Attribute;
import org.glassfish.security.services.api.common.Attributes;


public class AttributesImpl implements Attributes {

	private Map<String, Attribute> attributes = new TreeMap<String, Attribute>();
	
	public AttributesImpl() { }

	public int getAttributeCount() {
		return attributes.size();
	}

	public Set<String> getAttributeNames() {
		return attributes.keySet();
	}

	public Attribute getAttribute(String name) {
		return attributes.get(name);
	}

	public String getAttributeValue(String name) {
		Attribute a = attributes.get(name);
		if(a != null) {
			return a.getValue();
		}
		return null;
	}

	public Set<String> getAttributeValues(String name) {
		Attribute a = attributes.get(name);
		if(a != null) {
			return a.getValues();
		}
		return null;
	}

	public String[] getAttributeValuesAsArray(String name) {
		Attribute a = attributes.get(name);
		if(a != null) {
			return a.getValuesAsArray();
		}
		return null;
	}

	public void addAttribute(String name, String value, boolean replace) {
		Attribute a = attributes.get(name);
		if(a != null && !replace) {
			a.addValue(value);
		}
		else {
			attributes.put(name, new AttributeImpl(name, value));
		}
	}

	public void addAttribute(String name, Set<String> values, boolean replace) {
		Attribute a = attributes.get(name);
		if(a != null && !replace) {
			a.addValues(values);
		}
		else {
			attributes.put(name, new AttributeImpl(name, values));
		}
	}

	public void addAttribute(String name, String[] values, boolean replace) {
		Attribute a = attributes.get(name);
		if(a != null && !replace) {
			a.addValues(values);
		}
		else {
			attributes.put(name, new AttributeImpl(name, values));
		}
	}

	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	public void removeAttributeValue(String name, String value) {
		Attribute a = attributes.get(name);
		if (a != null) {
			a.removeValue(value);
		}
	}

	public void removeAttributeValues(String name, Set<String> values) {
		Attribute a = attributes.get(name);
		if (a != null) {
			a.removeValues(values);
		}
	}

	public void removeAttributeValues(String name, String[] values) {
		Attribute a = attributes.get(name);
		if (a != null) {
			a.removeValues(values);
		}
	}

	public void removeAllAttributeValues(String name) {
		Attribute a = attributes.get(name);
		if (a != null) {
			a.clear();
		}
	}

	public void clear() {
		attributes.clear();

	}

}
