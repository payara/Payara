/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

/**
 *	This generated bean class InstanceStats
 *	matches the schema element 'instance-stats'.
 *
 *	Generated on Fri Aug 19 00:43:43 IST 2005
 */

package com.sun.enterprise.admin.monitor.stats.lb;

public class InstanceStats implements com.sun.enterprise.admin.monitor.stats.lb.InstanceStatsInterface, com.sun.enterprise.admin.monitor.stats.lb.CommonBean {
	private java.lang.String _Id;
	private java.lang.String _Health;
	private java.lang.String _NumTotalRequests;
	private java.lang.String _NumActiveRequests;
	private java.util.List _ApplicationStats = new java.util.ArrayList();	// List<boolean>
	private java.lang.String _ApplicationStatsId;
	private java.lang.String _ApplicationStatsAverageResponseTime;
	private java.lang.String _ApplicationStatsMinResponseTime;
	private java.lang.String _ApplicationStatsMaxResponseTime;
	private java.lang.String _ApplicationStatsNumFailoverRequests;
	private java.lang.String _ApplicationStatsNumErrorRequests;
	private java.lang.String _ApplicationStatsNumActiveRequests;
	private java.lang.String _ApplicationStatsNumIdempotentUrlRequests;
	private java.lang.String _ApplicationStatsNumTotalRequests;

	public InstanceStats() {
		_Id = "";
		_Health = "";
		_NumTotalRequests = "";
		_NumActiveRequests = "";
		_ApplicationStatsId = "";
		_ApplicationStatsAverageResponseTime = "";
		_ApplicationStatsMinResponseTime = "";
		_ApplicationStatsMaxResponseTime = "";
		_ApplicationStatsNumFailoverRequests = "";
		_ApplicationStatsNumErrorRequests = "";
		_ApplicationStatsNumActiveRequests = "";
		_ApplicationStatsNumIdempotentUrlRequests = "";
		_ApplicationStatsNumTotalRequests = "";
	}

	// Deep copy
	public InstanceStats(com.sun.enterprise.admin.monitor.stats.lb.InstanceStats source) {
		_Id = source._Id;
		_Health = source._Health;
		_NumTotalRequests = source._NumTotalRequests;
		_NumActiveRequests = source._NumActiveRequests;
		for (java.util.Iterator it = source._ApplicationStats.iterator(); 
			it.hasNext(); ) {
			_ApplicationStats.add(it.next());
		}
		_ApplicationStatsId = source._ApplicationStatsId;
		_ApplicationStatsAverageResponseTime = source._ApplicationStatsAverageResponseTime;
		_ApplicationStatsMinResponseTime = source._ApplicationStatsMinResponseTime;
		_ApplicationStatsMaxResponseTime = source._ApplicationStatsMaxResponseTime;
		_ApplicationStatsNumFailoverRequests = source._ApplicationStatsNumFailoverRequests;
		_ApplicationStatsNumErrorRequests = source._ApplicationStatsNumErrorRequests;
		_ApplicationStatsNumActiveRequests = source._ApplicationStatsNumActiveRequests;
		_ApplicationStatsNumIdempotentUrlRequests = source._ApplicationStatsNumIdempotentUrlRequests;
		_ApplicationStatsNumTotalRequests = source._ApplicationStatsNumTotalRequests;
	}

	// This attribute is mandatory
	public void setId(java.lang.String value) {
		_Id = value;
	}

	public java.lang.String getId() {
		return _Id;
	}

	// This attribute is mandatory
	public void setHealth(java.lang.String value) {
		_Health = value;
	}

	public java.lang.String getHealth() {
		return _Health;
	}

	// This attribute is mandatory
	public void setNumTotalRequests(java.lang.String value) {
		_NumTotalRequests = value;
	}

	public java.lang.String getNumTotalRequests() {
		return _NumTotalRequests;
	}

	// This attribute is mandatory
	public void setNumActiveRequests(java.lang.String value) {
		_NumActiveRequests = value;
	}

	public java.lang.String getNumActiveRequests() {
		return _NumActiveRequests;
	}

	// This attribute is an array, possibly empty
	public void setApplicationStats(boolean[] value) {
		if (value == null)
			value = new boolean[0];
		_ApplicationStats.clear();
		for (int i = 0; i < value.length; ++i) {
			_ApplicationStats.add((value[i] ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
		}
	}

	public void setApplicationStats(int index, boolean value) {
		_ApplicationStats.set(index, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
	}

	public boolean[] getApplicationStats() {
		boolean[] arr = new boolean[_ApplicationStats.size()];
		for (int i = 0; i < arr.length; ++i) {
			arr[i] = ((java.lang.Boolean)_ApplicationStats.get(i)).booleanValue();
		}
		return arr;
	}

	public java.util.List fetchApplicationStatsList() {
		return _ApplicationStats;
	}

	public boolean isApplicationStats(int index) {
		return ((java.lang.Boolean)_ApplicationStats.get(index)).booleanValue();
	}

	// Return the number of applicationStats
	public int sizeApplicationStats() {
		return _ApplicationStats.size();
	}

	public int addApplicationStats(boolean value) {
		_ApplicationStats.add((value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
		return _ApplicationStats.size()-1;
	}

	// Search from the end looking for @param value, and then remove it.
	public int removeApplicationStats(boolean value) {
		int pos = _ApplicationStats.indexOf((value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
		if (pos >= 0) {
			_ApplicationStats.remove(pos);
		}
		return pos;
	}

	// This attribute is mandatory
	public void setApplicationStatsId(java.lang.String value) {
		_ApplicationStatsId = value;
	}

	public java.lang.String getApplicationStatsId() {
		return _ApplicationStatsId;
	}

	// This attribute is mandatory
	public void setApplicationStatsAverageResponseTime(java.lang.String value) {
		_ApplicationStatsAverageResponseTime = value;
	}

	public java.lang.String getApplicationStatsAverageResponseTime() {
		return _ApplicationStatsAverageResponseTime;
	}

	// This attribute is mandatory
	public void setApplicationStatsMinResponseTime(java.lang.String value) {
		_ApplicationStatsMinResponseTime = value;
	}

	public java.lang.String getApplicationStatsMinResponseTime() {
		return _ApplicationStatsMinResponseTime;
	}

	// This attribute is mandatory
	public void setApplicationStatsMaxResponseTime(java.lang.String value) {
		_ApplicationStatsMaxResponseTime = value;
	}

	public java.lang.String getApplicationStatsMaxResponseTime() {
		return _ApplicationStatsMaxResponseTime;
	}

	// This attribute is mandatory
	public void setApplicationStatsNumFailoverRequests(java.lang.String value) {
		_ApplicationStatsNumFailoverRequests = value;
	}

	public java.lang.String getApplicationStatsNumFailoverRequests() {
		return _ApplicationStatsNumFailoverRequests;
	}

	// This attribute is mandatory
	public void setApplicationStatsNumErrorRequests(java.lang.String value) {
		_ApplicationStatsNumErrorRequests = value;
	}

	public java.lang.String getApplicationStatsNumErrorRequests() {
		return _ApplicationStatsNumErrorRequests;
	}

	// This attribute is mandatory
	public void setApplicationStatsNumActiveRequests(java.lang.String value) {
		_ApplicationStatsNumActiveRequests = value;
	}

	public java.lang.String getApplicationStatsNumActiveRequests() {
		return _ApplicationStatsNumActiveRequests;
	}

	// This attribute is mandatory
	public void setApplicationStatsNumIdempotentUrlRequests(java.lang.String value) {
		_ApplicationStatsNumIdempotentUrlRequests = value;
	}

	public java.lang.String getApplicationStatsNumIdempotentUrlRequests() {
		return _ApplicationStatsNumIdempotentUrlRequests;
	}

	// This attribute is mandatory
	public void setApplicationStatsNumTotalRequests(java.lang.String value) {
		_ApplicationStatsNumTotalRequests = value;
	}

	public java.lang.String getApplicationStatsNumTotalRequests() {
		return _ApplicationStatsNumTotalRequests;
	}

	public void writeNode(java.io.Writer out, String nodeName, String indent) throws java.io.IOException {
		out.write(indent);
		out.write("<");
		out.write(nodeName);
		// id is an attribute
		if (_Id != null) {
			out.write(" id");	// NOI18N
			out.write("='");	// NOI18N
			com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _Id, true);
			out.write("'");	// NOI18N
		}
		// health is an attribute
		if (_Health != null) {
			out.write(" health");	// NOI18N
			out.write("='");	// NOI18N
			com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _Health, true);
			out.write("'");	// NOI18N
		}
		// num-total-requests is an attribute
		if (_NumTotalRequests != null) {
			out.write(" num-total-requests");	// NOI18N
			out.write("='");	// NOI18N
			com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _NumTotalRequests, true);
			out.write("'");	// NOI18N
		}
		// num-active-requests is an attribute
		if (_NumActiveRequests != null) {
			out.write(" num-active-requests");	// NOI18N
			out.write("='");	// NOI18N
			com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _NumActiveRequests, true);
			out.write("'");	// NOI18N
		}
		out.write(">\n");
		String nextIndent = indent + "	";
		for (java.util.Iterator it = _ApplicationStats.iterator(); 
			it.hasNext(); ) {
			boolean element = ((java.lang.Boolean)it.next()).booleanValue();
			if (element) {
				out.write(nextIndent);
				out.write("<application-stats");	// NOI18N
				// id is an attribute
				if (_ApplicationStatsId != null) {
					out.write(" id");	// NOI18N
					out.write("='");	// NOI18N
					com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _ApplicationStatsId, true);
					out.write("'");	// NOI18N
				}
				// average-response-time is an attribute
				if (_ApplicationStatsAverageResponseTime != null) {
					out.write(" average-response-time");	// NOI18N
					out.write("='");	// NOI18N
					com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _ApplicationStatsAverageResponseTime, true);
					out.write("'");	// NOI18N
				}
				// min-response-time is an attribute
				if (_ApplicationStatsMinResponseTime != null) {
					out.write(" min-response-time");	// NOI18N
					out.write("='");	// NOI18N
					com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _ApplicationStatsMinResponseTime, true);
					out.write("'");	// NOI18N
				}
				// max-response-time is an attribute
				if (_ApplicationStatsMaxResponseTime != null) {
					out.write(" max-response-time");	// NOI18N
					out.write("='");	// NOI18N
					com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _ApplicationStatsMaxResponseTime, true);
					out.write("'");	// NOI18N
				}
				// num-failover-requests is an attribute
				if (_ApplicationStatsNumFailoverRequests != null) {
					out.write(" num-failover-requests");	// NOI18N
					out.write("='");	// NOI18N
					com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _ApplicationStatsNumFailoverRequests, true);
					out.write("'");	// NOI18N
				}
				// num-error-requests is an attribute
				if (_ApplicationStatsNumErrorRequests != null) {
					out.write(" num-error-requests");	// NOI18N
					out.write("='");	// NOI18N
					com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _ApplicationStatsNumErrorRequests, true);
					out.write("'");	// NOI18N
				}
				// num-active-requests is an attribute
				if (_ApplicationStatsNumActiveRequests != null) {
					out.write(" num-active-requests");	// NOI18N
					out.write("='");	// NOI18N
					com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _ApplicationStatsNumActiveRequests, true);
					out.write("'");	// NOI18N
				}
				// num-idempotent-url-requests is an attribute
				if (_ApplicationStatsNumIdempotentUrlRequests != null) {
					out.write(" num-idempotent-url-requests");	// NOI18N
					out.write("='");	// NOI18N
					com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _ApplicationStatsNumIdempotentUrlRequests, true);
					out.write("'");	// NOI18N
				}
				// num-total-requests is an attribute
				if (_ApplicationStatsNumTotalRequests != null) {
					out.write(" num-total-requests");	// NOI18N
					out.write("='");	// NOI18N
					com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.writeXML(out, _ApplicationStatsNumTotalRequests, true);
					out.write("'");	// NOI18N
				}
				out.write("/>\n");	// NOI18N
			}
		}
		out.write(indent);
		out.write("</"+nodeName+">\n");
	}

	public void readNode(org.w3c.dom.Node node) {
		if (node.hasAttributes()) {
			org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
			org.w3c.dom.Attr attr;
			attr = (org.w3c.dom.Attr) attrs.getNamedItem("id");
			if (attr != null) {
				_Id = attr.getValue();
			}
			attr = (org.w3c.dom.Attr) attrs.getNamedItem("health");
			if (attr != null) {
				_Health = attr.getValue();
			}
			attr = (org.w3c.dom.Attr) attrs.getNamedItem("num-total-requests");
			if (attr != null) {
				_NumTotalRequests = attr.getValue();
			}
			attr = (org.w3c.dom.Attr) attrs.getNamedItem("num-active-requests");
			if (attr != null) {
				_NumActiveRequests = attr.getValue();
			}
		}
		org.w3c.dom.NodeList children = node.getChildNodes();
		int lastElementType = 0;
		for (int i = 0, size = children.getLength(); i < size; ++i) {
			org.w3c.dom.Node childNode = children.item(i);
			String childNodeName = (childNode.getLocalName() == null ? childNode.getNodeName().intern() : childNode.getLocalName().intern());
			String childNodeValue = "";
			if (childNode.getFirstChild() != null) {
				childNodeValue = childNode.getFirstChild().getNodeValue();
			}
			if (childNodeName == "application-stats") {
				boolean aApplicationStats;
				if (childNode.getFirstChild() == null)
					aApplicationStats = true;
				else
					aApplicationStats = java.lang.Boolean.valueOf(childNodeValue).booleanValue();
				if (childNode.hasAttributes()) {
					org.w3c.dom.NamedNodeMap attrs = childNode.getAttributes();
					org.w3c.dom.Attr attr;
					attr = (org.w3c.dom.Attr) attrs.getNamedItem("id");
					if (attr != null) {
						_ApplicationStatsId = attr.getValue();
					}
					attr = (org.w3c.dom.Attr) attrs.getNamedItem("average-response-time");
					if (attr != null) {
						_ApplicationStatsAverageResponseTime = attr.getValue();
					}
					attr = (org.w3c.dom.Attr) attrs.getNamedItem("min-response-time");
					if (attr != null) {
						_ApplicationStatsMinResponseTime = attr.getValue();
					}
					attr = (org.w3c.dom.Attr) attrs.getNamedItem("max-response-time");
					if (attr != null) {
						_ApplicationStatsMaxResponseTime = attr.getValue();
					}
					attr = (org.w3c.dom.Attr) attrs.getNamedItem("num-failover-requests");
					if (attr != null) {
						_ApplicationStatsNumFailoverRequests = attr.getValue();
					}
					attr = (org.w3c.dom.Attr) attrs.getNamedItem("num-error-requests");
					if (attr != null) {
						_ApplicationStatsNumErrorRequests = attr.getValue();
					}
					attr = (org.w3c.dom.Attr) attrs.getNamedItem("num-active-requests");
					if (attr != null) {
						_ApplicationStatsNumActiveRequests = attr.getValue();
					}
					attr = (org.w3c.dom.Attr) attrs.getNamedItem("num-idempotent-url-requests");
					if (attr != null) {
						_ApplicationStatsNumIdempotentUrlRequests = attr.getValue();
					}
					attr = (org.w3c.dom.Attr) attrs.getNamedItem("num-total-requests");
					if (attr != null) {
						_ApplicationStatsNumTotalRequests = attr.getValue();
					}
				}
				_ApplicationStats.add((aApplicationStats ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
				lastElementType = 4;
			}
			else {
				// Found extra unrecognized childNode
			}
		}
	}

	public void validate() throws com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException {
		boolean restrictionFailure = false;
		// Validating property id
		if (getId() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getId() == null", "id", this);	// NOI18N
		}
		// Validating property health
		if (getHealth() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getHealth() == null", "health", this);	// NOI18N
		}
		// Validating property numTotalRequests
		if (getNumTotalRequests() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getNumTotalRequests() == null", "numTotalRequests", this);	// NOI18N
		}
		// Validating property numActiveRequests
		if (getNumActiveRequests() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getNumActiveRequests() == null", "numActiveRequests", this);	// NOI18N
		}
		// Validating property applicationStatsId
		if (getApplicationStatsId() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getApplicationStatsId() == null", "applicationStatsId", this);	// NOI18N
		}
		// Validating property applicationStatsAverageResponseTime
		if (getApplicationStatsAverageResponseTime() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getApplicationStatsAverageResponseTime() == null", "applicationStatsAverageResponseTime", this);	// NOI18N
		}
		// Validating property applicationStatsMinResponseTime
		if (getApplicationStatsMinResponseTime() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getApplicationStatsMinResponseTime() == null", "applicationStatsMinResponseTime", this);	// NOI18N
		}
		// Validating property applicationStatsMaxResponseTime
		if (getApplicationStatsMaxResponseTime() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getApplicationStatsMaxResponseTime() == null", "applicationStatsMaxResponseTime", this);	// NOI18N
		}
		// Validating property applicationStatsNumFailoverRequests
		if (getApplicationStatsNumFailoverRequests() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getApplicationStatsNumFailoverRequests() == null", "applicationStatsNumFailoverRequests", this);	// NOI18N
		}
		// Validating property applicationStatsNumErrorRequests
		if (getApplicationStatsNumErrorRequests() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getApplicationStatsNumErrorRequests() == null", "applicationStatsNumErrorRequests", this);	// NOI18N
		}
		// Validating property applicationStatsNumActiveRequests
		if (getApplicationStatsNumActiveRequests() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getApplicationStatsNumActiveRequests() == null", "applicationStatsNumActiveRequests", this);	// NOI18N
		}
		// Validating property applicationStatsNumIdempotentUrlRequests
		if (getApplicationStatsNumIdempotentUrlRequests() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getApplicationStatsNumIdempotentUrlRequests() == null", "applicationStatsNumIdempotentUrlRequests", this);	// NOI18N
		}
		// Validating property applicationStatsNumTotalRequests
		if (getApplicationStatsNumTotalRequests() == null) {
			throw new com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException("getApplicationStatsNumTotalRequests() == null", "applicationStatsNumTotalRequests", this);	// NOI18N
		}
	}

	public void changePropertyByName(final String nameIn, Object value) {
		if (nameIn == null) return;
        
        /** The '==' comparisons below are correct;
            see the javadoc for String.intern().
          */
		final String name = nameIn.intern();
		if (name == "id")
			setId((java.lang.String)value);
		else if (name == "health")
			setHealth((java.lang.String)value);
		else if (name == "numTotalRequests")
			setNumTotalRequests((java.lang.String)value);
		else if (name == "numActiveRequests")
			setNumActiveRequests((java.lang.String)value);
		else if (name == "applicationStats")
			addApplicationStats(((java.lang.Boolean)value).booleanValue());
		else if (name == "applicationStats[]")
			setApplicationStats((boolean[]) value);
		else if (name == "applicationStatsId")
			setApplicationStatsId((java.lang.String)value);
		else if (name == "applicationStatsAverageResponseTime")
			setApplicationStatsAverageResponseTime((java.lang.String)value);
		else if (name == "applicationStatsMinResponseTime")
			setApplicationStatsMinResponseTime((java.lang.String)value);
		else if (name == "applicationStatsMaxResponseTime")
			setApplicationStatsMaxResponseTime((java.lang.String)value);
		else if (name == "applicationStatsNumFailoverRequests")
			setApplicationStatsNumFailoverRequests((java.lang.String)value);
		else if (name == "applicationStatsNumErrorRequests")
			setApplicationStatsNumErrorRequests((java.lang.String)value);
		else if (name == "applicationStatsNumActiveRequests")
			setApplicationStatsNumActiveRequests((java.lang.String)value);
		else if (name == "applicationStatsNumIdempotentUrlRequests")
			setApplicationStatsNumIdempotentUrlRequests((java.lang.String)value);
		else if (name == "applicationStatsNumTotalRequests")
			setApplicationStatsNumTotalRequests((java.lang.String)value);
		else
			throw new IllegalArgumentException(name+" is not a valid property name for InstanceStats");
	}

	public Object fetchPropertyByName(final String nameIn) {
        if ( nameIn == null ) {
            throw new IllegalArgumentException();
        }
        /** The '==' comparisons below are correct;
            see the javadoc for String.intern().
          */
        final String name = nameIn.intern();

		if (name == "id")
			return getId();
		if (name == "health")
			return getHealth();
		if (name == "numTotalRequests")
			return getNumTotalRequests();
		if (name == "numActiveRequests")
			return getNumActiveRequests();
		if (name == "applicationStats[]")
			return getApplicationStats();
		if (name == "applicationStatsId")
			return getApplicationStatsId();
		if (name == "applicationStatsAverageResponseTime")
			return getApplicationStatsAverageResponseTime();
		if (name == "applicationStatsMinResponseTime")
			return getApplicationStatsMinResponseTime();
		if (name == "applicationStatsMaxResponseTime")
			return getApplicationStatsMaxResponseTime();
		if (name == "applicationStatsNumFailoverRequests")
			return getApplicationStatsNumFailoverRequests();
		if (name == "applicationStatsNumErrorRequests")
			return getApplicationStatsNumErrorRequests();
		if (name == "applicationStatsNumActiveRequests")
			return getApplicationStatsNumActiveRequests();
		if (name == "applicationStatsNumIdempotentUrlRequests")
			return getApplicationStatsNumIdempotentUrlRequests();
		if (name == "applicationStatsNumTotalRequests")
			return getApplicationStatsNumTotalRequests();
		throw new IllegalArgumentException(name+" is not a valid property name for InstanceStats");
	}

	// Return an array of all of the properties that are beans and are set.
	public com.sun.enterprise.admin.monitor.stats.lb.CommonBean[] childBeans(boolean recursive) {
		java.util.List children = new java.util.LinkedList();
		childBeans(recursive, children);
		com.sun.enterprise.admin.monitor.stats.lb.CommonBean[] result = new com.sun.enterprise.admin.monitor.stats.lb.CommonBean[children.size()];
		return (com.sun.enterprise.admin.monitor.stats.lb.CommonBean[]) children.toArray(result);
	}

	// Put all child beans into the beans list.
	public void childBeans(boolean recursive, java.util.List beans) {
	}

    @Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof com.sun.enterprise.admin.monitor.stats.lb.InstanceStats))
			return false;
		com.sun.enterprise.admin.monitor.stats.lb.InstanceStats inst = (com.sun.enterprise.admin.monitor.stats.lb.InstanceStats) o;
		if (!(_Id == null ? inst._Id == null : _Id.equals(inst._Id)))
			return false;
		if (!(_Health == null ? inst._Health == null : _Health.equals(inst._Health)))
			return false;
		if (!(_NumTotalRequests == null ? inst._NumTotalRequests == null : _NumTotalRequests.equals(inst._NumTotalRequests)))
			return false;
		if (!(_NumActiveRequests == null ? inst._NumActiveRequests == null : _NumActiveRequests.equals(inst._NumActiveRequests)))
			return false;
		if (sizeApplicationStats() != inst.sizeApplicationStats())
			return false;
		// Compare every element.
		for (java.util.Iterator it = _ApplicationStats.iterator(), it2 = inst._ApplicationStats.iterator(); 
			it.hasNext() && it2.hasNext(); ) {
			boolean element = ((java.lang.Boolean)it.next()).booleanValue();
			boolean element2 = ((java.lang.Boolean)it2.next()).booleanValue();
			if (!(element == element2))
				return false;
		}
		if (!(_ApplicationStatsId == null ? inst._ApplicationStatsId == null : _ApplicationStatsId.equals(inst._ApplicationStatsId)))
			return false;
		if (!(_ApplicationStatsAverageResponseTime == null ? inst._ApplicationStatsAverageResponseTime == null : _ApplicationStatsAverageResponseTime.equals(inst._ApplicationStatsAverageResponseTime)))
			return false;
		if (!(_ApplicationStatsMinResponseTime == null ? inst._ApplicationStatsMinResponseTime == null : _ApplicationStatsMinResponseTime.equals(inst._ApplicationStatsMinResponseTime)))
			return false;
		if (!(_ApplicationStatsMaxResponseTime == null ? inst._ApplicationStatsMaxResponseTime == null : _ApplicationStatsMaxResponseTime.equals(inst._ApplicationStatsMaxResponseTime)))
			return false;
		if (!(_ApplicationStatsNumFailoverRequests == null ? inst._ApplicationStatsNumFailoverRequests == null : _ApplicationStatsNumFailoverRequests.equals(inst._ApplicationStatsNumFailoverRequests)))
			return false;
		if (!(_ApplicationStatsNumErrorRequests == null ? inst._ApplicationStatsNumErrorRequests == null : _ApplicationStatsNumErrorRequests.equals(inst._ApplicationStatsNumErrorRequests)))
			return false;
		if (!(_ApplicationStatsNumActiveRequests == null ? inst._ApplicationStatsNumActiveRequests == null : _ApplicationStatsNumActiveRequests.equals(inst._ApplicationStatsNumActiveRequests)))
			return false;
		if (!(_ApplicationStatsNumIdempotentUrlRequests == null ? inst._ApplicationStatsNumIdempotentUrlRequests == null : _ApplicationStatsNumIdempotentUrlRequests.equals(inst._ApplicationStatsNumIdempotentUrlRequests)))
			return false;
		if (!(_ApplicationStatsNumTotalRequests == null ? inst._ApplicationStatsNumTotalRequests == null : _ApplicationStatsNumTotalRequests.equals(inst._ApplicationStatsNumTotalRequests)))
			return false;
		return true;
	}

    @Override
	public int hashCode() {
		int result = 17;
		result = 37*result + (_Id == null ? 0 : _Id.hashCode());
		result = 37*result + (_Health == null ? 0 : _Health.hashCode());
		result = 37*result + (_NumTotalRequests == null ? 0 : _NumTotalRequests.hashCode());
		result = 37*result + (_NumActiveRequests == null ? 0 : _NumActiveRequests.hashCode());
		result = 37*result + ((_ApplicationStats).hashCode());
		result = 37*result + (_ApplicationStatsId == null ? 0 : _ApplicationStatsId.hashCode());
		result = 37*result + (_ApplicationStatsAverageResponseTime == null ? 0 : _ApplicationStatsAverageResponseTime.hashCode());
		result = 37*result + (_ApplicationStatsMinResponseTime == null ? 0 : _ApplicationStatsMinResponseTime.hashCode());
		result = 37*result + (_ApplicationStatsMaxResponseTime == null ? 0 : _ApplicationStatsMaxResponseTime.hashCode());
		result = 37*result + (_ApplicationStatsNumFailoverRequests == null ? 0 : _ApplicationStatsNumFailoverRequests.hashCode());
		result = 37*result + (_ApplicationStatsNumErrorRequests == null ? 0 : _ApplicationStatsNumErrorRequests.hashCode());
		result = 37*result + (_ApplicationStatsNumActiveRequests == null ? 0 : _ApplicationStatsNumActiveRequests.hashCode());
		result = 37*result + (_ApplicationStatsNumIdempotentUrlRequests == null ? 0 : _ApplicationStatsNumIdempotentUrlRequests.hashCode());
		result = 37*result + (_ApplicationStatsNumTotalRequests == null ? 0 : _ApplicationStatsNumTotalRequests.hashCode());
		return result;
	}

    @Override
	public String toString() {
		java.io.StringWriter sw = new java.io.StringWriter();
		try {
			writeNode(sw, "InstanceStats", "");
		} catch (java.io.IOException e) {
			// How can we actually get an IOException on a StringWriter?
			// We'll just ignore it.
		}
		return sw.toString();
	}
}


/*
		The following schema file has been used for generation:

<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : sun_loadbalancer_stats_1_0.dtd.dtd
    Created on : August 15, 2005, 3:22 PM
    Author     : hr124446
    Description:
        Purpose of the document follows.

    TODO define vocabulary identification data
    PUBLIC ID  : -//Sun Microsystems Inc.//DTD Application Server 9.0 LoadBalancer Stats//EN
    SYSTEM ID  : http://www.sun.com/software/appserver/dtds/sun_loadbalancer_stats_1_0.dtd
-->

<!-- Root element for load balancer. It contains all the statistics -->
   <!ELEMENT load-balancer-stats (cluster-stats*)>

   <!--
   Cluster Statistics
     id Cluster name 
   -->

   <!ELEMENT cluster-stats (instance-stats*)>
   <!ATTLIST cluster-stats
     id CDATA #REQUIRED>

   <!--
   Instance Statistics
     id Server instance name
     num-active-requests The number of active requests on this server.
     num-total-requests The number of total requests on this server.
   -->
   <!ELEMENT instance-stats (application-stats*)>
   <!ATTLIST instance-stats
     id CDATA #REQUIRED
     health CDATA #REQUIRED
     num-total-requests CDATA #REQUIRED
     num-active-requests CDATA #REQUIRED>

   <!--
   Application Statistics
     id web module or web service endpoint's uri.
     average-response-time Average response time in milli seconds.
     min-response-time Minimum response time observed in milli seconds.
     max-response-time Maximum response time observed in milli seconds.
     num-failover-requests The number of failed-over requests for this context root.
     num-error-requests The number of failed requests for this context root.
     num-active-requests The number of active requests for this context root.
     num-total-requests The number of total requests for this context root.
     num-idempotent-url-requests The number of times all the idempotent urls in 
	this application are accessesd.
   -->
   <!ELEMENT application-stats EMPTY>
   <!ATTLIST application-stats
     id CDATA #REQUIRED
     average-response-time CDATA #REQUIRED
     min-response-time CDATA #REQUIRED
     max-response-time CDATA #REQUIRED
     num-failover-requests CDATA #REQUIRED
     num-error-requests CDATA #REQUIRED
     num-active-requests CDATA #REQUIRED
     num-idempotent-url-requests CDATA #REQUIRED
     num-total-requests CDATA #REQUIRED>


*/
