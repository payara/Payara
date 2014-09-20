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
 *	This generated bean class ClusterStats
 *	matches the schema element 'cluster-stats'.
 *
 *	Generated on Fri Aug 19 00:43:43 IST 2005
 */

package com.sun.enterprise.admin.monitor.stats.lb;

public class ClusterStats implements com.sun.enterprise.admin.monitor.stats.lb.ClusterStatsInterface, com.sun.enterprise.admin.monitor.stats.lb.CommonBean {
	private java.lang.String _Id;
	private final java.util.List<InstanceStats> _InstanceStats = new java.util.ArrayList();	// List<InstanceStats>

	public ClusterStats() {
		_Id = "";
	}

	// Deep copy
	public ClusterStats(com.sun.enterprise.admin.monitor.stats.lb.ClusterStats source) {
		_Id = source._Id;
		for (java.util.Iterator it = source._InstanceStats.iterator(); 
			it.hasNext(); ) {
			_InstanceStats.add(new com.sun.enterprise.admin.monitor.stats.lb.InstanceStats((com.sun.enterprise.admin.monitor.stats.lb.InstanceStats)it.next()));
		}
	}

	// This attribute is mandatory
	public void setId(java.lang.String value) {
		_Id = value;
	}

	public java.lang.String getId() {
		return _Id;
	}

	// This attribute is an array, possibly empty
	public void setInstanceStats(com.sun.enterprise.admin.monitor.stats.lb.InstanceStats[] value) {
		if (value == null)
			value = new InstanceStats[0];
		_InstanceStats.clear();
		for (int i = 0; i < value.length; ++i) {
			_InstanceStats.add(value[i]);
		}
	}

	public void setInstanceStats(int index, com.sun.enterprise.admin.monitor.stats.lb.InstanceStats value) {
		_InstanceStats.set(index, value);
	}

	public com.sun.enterprise.admin.monitor.stats.lb.InstanceStats[] getInstanceStats() {
		InstanceStats[] arr = new InstanceStats[_InstanceStats.size()];
		return (InstanceStats[]) _InstanceStats.toArray(arr);
	}

	public java.util.List fetchInstanceStatsList() {
		return _InstanceStats;
	}

	public com.sun.enterprise.admin.monitor.stats.lb.InstanceStats getInstanceStats(int index) {
		return (InstanceStats)_InstanceStats.get(index);
	}

	// Return the number of instanceStats
	public int sizeInstanceStats() {
		return _InstanceStats.size();
	}

	public int addInstanceStats(com.sun.enterprise.admin.monitor.stats.lb.InstanceStats value) {
		_InstanceStats.add(value);
		return _InstanceStats.size()-1;
	}

	// Search from the end looking for @param value, and then remove it.
	public int removeInstanceStats(com.sun.enterprise.admin.monitor.stats.lb.InstanceStats value) {
		int pos = _InstanceStats.indexOf(value);
		if (pos >= 0) {
			_InstanceStats.remove(pos);
		}
		return pos;
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
		out.write(">\n");
		String nextIndent = indent + "	";
		for (java.util.Iterator it = _InstanceStats.iterator(); 
			it.hasNext(); ) {
			com.sun.enterprise.admin.monitor.stats.lb.InstanceStats element = (com.sun.enterprise.admin.monitor.stats.lb.InstanceStats)it.next();
			if (element != null) {
				element.writeNode(out, "instance-stats", nextIndent);
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
		}
		org.w3c.dom.NodeList children = node.getChildNodes();
		for (int i = 0, size = children.getLength(); i < size; ++i) {
			org.w3c.dom.Node childNode = children.item(i);
			String childNodeName = (childNode.getLocalName() == null ? childNode.getNodeName().intern() : childNode.getLocalName().intern());
			if (childNodeName == "instance-stats") {
				InstanceStats aInstanceStats = new com.sun.enterprise.admin.monitor.stats.lb.InstanceStats();
				aInstanceStats.readNode(childNode);
				_InstanceStats.add(aInstanceStats);
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
		// Validating property instanceStats
		for (int _index = 0; _index < sizeInstanceStats(); ++_index) {
			com.sun.enterprise.admin.monitor.stats.lb.InstanceStats element = getInstanceStats(_index);
			if (element != null) {
				element.validate();
			}
		}
	}

	public void changePropertyByName(String name, Object value) {
		if (name == null) return;
		name = name.intern();
		if (name.equals("id"))
			setId((java.lang.String)value);
		else if (name.equals("instanceStats"))
			addInstanceStats((InstanceStats)value);
		else if (name.equals("instanceStats[]"))
			setInstanceStats((InstanceStats[]) value);
		else
			throw new IllegalArgumentException(name+" is not a valid property name for ClusterStats");
	}

	public Object fetchPropertyByName(String name) {
		if (name.equals("id"))
			return getId();
		if (name.equals("instanceStats[]"))
			return getInstanceStats();
		throw new IllegalArgumentException(name+" is not a valid property name for ClusterStats");
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
		for (java.util.Iterator it = _InstanceStats.iterator(); 
			it.hasNext(); ) {
			com.sun.enterprise.admin.monitor.stats.lb.InstanceStats element = (com.sun.enterprise.admin.monitor.stats.lb.InstanceStats)it.next();
			if (element != null) {
				if (recursive) {
					element.childBeans(true, beans);
				}
				beans.add(element);
			}
		}
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof com.sun.enterprise.admin.monitor.stats.lb.ClusterStats))
			return false;
		com.sun.enterprise.admin.monitor.stats.lb.ClusterStats inst = (com.sun.enterprise.admin.monitor.stats.lb.ClusterStats) o;
		if (!(_Id == null ? inst._Id == null : _Id.equals(inst._Id)))
			return false;
		if (sizeInstanceStats() != inst.sizeInstanceStats())
			return false;
		// Compare every element.
		for (java.util.Iterator it = _InstanceStats.iterator(), it2 = inst._InstanceStats.iterator(); 
			it.hasNext() && it2.hasNext(); ) {
			com.sun.enterprise.admin.monitor.stats.lb.InstanceStats element = (com.sun.enterprise.admin.monitor.stats.lb.InstanceStats)it.next();
			com.sun.enterprise.admin.monitor.stats.lb.InstanceStats element2 = (com.sun.enterprise.admin.monitor.stats.lb.InstanceStats)it2.next();
			if (!(element == null ? element2 == null : element.equals(element2)))
				return false;
		}
		return true;
	}

	public int hashCode() {
		int result = 17;
		result = 37*result + (_Id == null ? 0 : _Id.hashCode());
		result = 37*result + (_InstanceStats == null ? 0 : _InstanceStats.hashCode());
		return result;
	}

	public String toString() {
		java.io.StringWriter sw = new java.io.StringWriter();
		try {
			writeNode(sw, "ClusterStats", "");
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
