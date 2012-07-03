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
 *	This generated bean class LoadBalancerStats
 *	matches the schema element 'load-balancer-stats'.
 *
 *	Generated on Fri Aug 19 00:43:43 IST 2005
 *
 *	This class matches the root element of the DTD,
 *	and is the root of the bean graph.
 *
 * 	load-balancer-stats : LoadBalancerStats
 * 		cluster-stats : ClusterStats[0,n]
 * 			[attr: id CDATA #REQUIRED ]
 * 			instance-stats : InstanceStats[0,n]
 * 				[attr: id CDATA #REQUIRED ]
 * 				[attr: health CDATA #REQUIRED ]
 * 				[attr: num-total-requests CDATA #REQUIRED ]
 * 				[attr: num-active-requests CDATA #REQUIRED ]
 * 				application-stats : Boolean[0,n]
 * 					[attr: id CDATA #REQUIRED ]
 * 					[attr: average-response-time CDATA #REQUIRED ]
 * 					[attr: min-response-time CDATA #REQUIRED ]
 * 					[attr: max-response-time CDATA #REQUIRED ]
 * 					[attr: num-failover-requests CDATA #REQUIRED ]
 * 					[attr: num-error-requests CDATA #REQUIRED ]
 * 					[attr: num-active-requests CDATA #REQUIRED ]
 * 					[attr: num-idempotent-url-requests CDATA #REQUIRED ]
 * 					[attr: num-total-requests CDATA #REQUIRED ]
 * 					EMPTY : String
 *
 */

package com.sun.enterprise.admin.monitor.stats.lb;

public class LoadBalancerStats implements com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStatsInterface, com.sun.enterprise.admin.monitor.stats.lb.CommonBean {
	private java.util.List _ClusterStats = new java.util.ArrayList();	// List<ClusterStats>

	public LoadBalancerStats() {
	}

	// Deep copy
	public LoadBalancerStats(com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats source) {
		for (java.util.Iterator it = source._ClusterStats.iterator(); 
			it.hasNext(); ) {
			_ClusterStats.add(new com.sun.enterprise.admin.monitor.stats.lb.ClusterStats((com.sun.enterprise.admin.monitor.stats.lb.ClusterStats)it.next()));
		}
	}

	// This attribute is an array, possibly empty
	public void setClusterStats(com.sun.enterprise.admin.monitor.stats.lb.ClusterStats[] value) {
		if (value == null)
			value = new ClusterStats[0];
		_ClusterStats.clear();
		for (int i = 0; i < value.length; ++i) {
			_ClusterStats.add(value[i]);
		}
	}

	public void setClusterStats(int index, com.sun.enterprise.admin.monitor.stats.lb.ClusterStats value) {
		_ClusterStats.set(index, value);
	}

	public com.sun.enterprise.admin.monitor.stats.lb.ClusterStats[] getClusterStats() {
		ClusterStats[] arr = new ClusterStats[_ClusterStats.size()];
		return (ClusterStats[]) _ClusterStats.toArray(arr);
	}

	public java.util.List fetchClusterStatsList() {
		return _ClusterStats;
	}

	public com.sun.enterprise.admin.monitor.stats.lb.ClusterStats getClusterStats(int index) {
		return (ClusterStats)_ClusterStats.get(index);
	}

	// Return the number of clusterStats
	public int sizeClusterStats() {
		return _ClusterStats.size();
	}

	public int addClusterStats(com.sun.enterprise.admin.monitor.stats.lb.ClusterStats value) {
		_ClusterStats.add(value);
		return _ClusterStats.size()-1;
	}

	// Search from the end looking for @param value, and then remove it.
	public int removeClusterStats(com.sun.enterprise.admin.monitor.stats.lb.ClusterStats value) {
		int pos = _ClusterStats.indexOf(value);
		if (pos >= 0) {
			_ClusterStats.remove(pos);
		}
		return pos;
	}

	public void write(java.io.OutputStream out) throws java.io.IOException {
		write(out, null);
	}

	public void write(java.io.OutputStream out, String encoding) throws java.io.IOException {
		java.io.Writer w;
		if (encoding == null) {
			encoding = "UTF-8";	// NOI18N
		}
		w = new java.io.BufferedWriter(new java.io.OutputStreamWriter(out, encoding));
		write(w, encoding);
		w.flush();
	}

	// Print this Java Bean to @param out including an XML header.
	// @param encoding is the encoding style that @param out was opened with.
	public void write(java.io.Writer out, String encoding) throws java.io.IOException {
		out.write("<?xml version='1.0'");	// NOI18N
		if (encoding != null)
			out.write(" encoding='"+encoding+"'");	// NOI18N
		out.write(" ?>\n");	// NOI18N
		writeNode(out, "load-balancer-stats", "");	// NOI18N
	}

	public void writeNode(java.io.Writer out, String nodeName, String indent) throws java.io.IOException {
		out.write(indent);
		out.write("<");
		out.write(nodeName);
		out.write(">\n");
		String nextIndent = indent + "	";
		for (java.util.Iterator it = _ClusterStats.iterator(); 
			it.hasNext(); ) {
			com.sun.enterprise.admin.monitor.stats.lb.ClusterStats element = (com.sun.enterprise.admin.monitor.stats.lb.ClusterStats)it.next();
			if (element != null) {
				element.writeNode(out, "cluster-stats", nextIndent);
			}
		}
		out.write(indent);
		out.write("</"+nodeName+">\n");
	}

	public static LoadBalancerStats read(java.io.InputStream in) throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException, java.io.IOException {
		return read(new org.xml.sax.InputSource(in), false, null, null);
	}

	// Warning: in readNoEntityResolver character and entity references will
	// not be read from any DTD in the XML source.
	// However, this way is faster since no DTDs are looked up
	// (possibly skipping network access) or parsed.
	public static LoadBalancerStats readNoEntityResolver(java.io.InputStream in) throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException, java.io.IOException {
		return read(new org.xml.sax.InputSource(in), false,
			new org.xml.sax.EntityResolver() {
			public org.xml.sax.InputSource resolveEntity(String publicId, String systemId) {
				java.io.ByteArrayInputStream bin = new java.io.ByteArrayInputStream(new byte[0]);
				return new org.xml.sax.InputSource(bin);
			}
		}
			, null);
	}

	public static LoadBalancerStats read(org.xml.sax.InputSource in, boolean validate, org.xml.sax.EntityResolver er, org.xml.sax.ErrorHandler eh) throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException, java.io.IOException {
		javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		dbf.setValidating(validate);
		javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
		if (er != null)	db.setEntityResolver(er);
		if (eh != null)	db.setErrorHandler(eh);
		org.w3c.dom.Document doc = db.parse(in);
		return read(doc);
	}

	public static LoadBalancerStats read(org.w3c.dom.Document document) {
		LoadBalancerStats aLoadBalancerStats = new LoadBalancerStats();
		aLoadBalancerStats.readNode(document.getDocumentElement());
		return aLoadBalancerStats;
	}

	public void readNode(org.w3c.dom.Node node) {
		org.w3c.dom.NodeList children = node.getChildNodes();
		for (int i = 0, size = children.getLength(); i < size; ++i) {
			org.w3c.dom.Node childNode = children.item(i);
			String childNodeName = (childNode.getLocalName() == null ? childNode.getNodeName().intern() : childNode.getLocalName().intern());
			String childNodeValue = "";
			if (childNodeName == "cluster-stats") {
				ClusterStats aClusterStats = new com.sun.enterprise.admin.monitor.stats.lb.ClusterStats();
				aClusterStats.readNode(childNode);
				_ClusterStats.add(aClusterStats);
			}
			else {
				// Found extra unrecognized childNode
			}
		}
	}

	// Takes some text to be printed into an XML stream and escapes any
	// characters that might make it invalid XML (like '<').
	public static void writeXML(java.io.Writer out, String msg) throws java.io.IOException {
		writeXML(out, msg, true);
	}

	public static void writeXML(java.io.Writer out, String msg, boolean attribute) throws java.io.IOException {
		if (msg == null)
			return;
		int msgLength = msg.length();
		for (int i = 0; i < msgLength; ++i) {
			char c = msg.charAt(i);
			writeXML(out, c, attribute);
		}
	}

	public static void writeXML(java.io.Writer out, char msg, boolean attribute) throws java.io.IOException {
		if (msg == '&')
			out.write("&amp;");
		else if (msg == '<')
			out.write("&lt;");
		else if (msg == '>')
			out.write("&gt;");
		else if (attribute && msg == '"')
			out.write("&quot;");
		else if (attribute && msg == '\'')
			out.write("&apos;");
		else if (attribute && msg == '\n')
			out.write("&#xA;");
		else if (attribute && msg == '\t')
			out.write("&#x9;");
		else
			out.write(msg);
	}

	public static class ValidateException extends Exception {
		private com.sun.enterprise.admin.monitor.stats.lb.CommonBean failedBean;
		private String failedPropertyName;
		public ValidateException(String msg, String failedPropertyName, com.sun.enterprise.admin.monitor.stats.lb.CommonBean failedBean) {
			super(msg);
			this.failedBean = failedBean;
			this.failedPropertyName = failedPropertyName;
		}
		public String getFailedPropertyName() {return failedPropertyName;}
		public com.sun.enterprise.admin.monitor.stats.lb.CommonBean getFailedBean() {return failedBean;}
	}

	public void validate() throws com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats.ValidateException {
		boolean restrictionFailure = false;
		// Validating property clusterStats
		for (int _index = 0; _index < sizeClusterStats(); ++_index) {
			com.sun.enterprise.admin.monitor.stats.lb.ClusterStats element = getClusterStats(_index);
			if (element != null) {
				element.validate();
			}
		}
	}

	public void changePropertyByName(String name, Object value) {
		if (name == null) return;
		name = name.intern();
		if (name == "clusterStats")
			addClusterStats((ClusterStats)value);
		else if (name == "clusterStats[]")
			setClusterStats((ClusterStats[]) value);
		else
			throw new IllegalArgumentException(name+" is not a valid property name for LoadBalancerStats");
	}

	public Object fetchPropertyByName(final String name) {
		if (name.equals("clusterStats[]"))
			return getClusterStats();
		throw new IllegalArgumentException(name+" is not a valid property name for LoadBalancerStats");
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
		for (java.util.Iterator it = _ClusterStats.iterator(); 
			it.hasNext(); ) {
			com.sun.enterprise.admin.monitor.stats.lb.ClusterStats element = (com.sun.enterprise.admin.monitor.stats.lb.ClusterStats)it.next();
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
		if (!(o instanceof com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats))
			return false;
		com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats inst = (com.sun.enterprise.admin.monitor.stats.lb.LoadBalancerStats) o;
		if (sizeClusterStats() != inst.sizeClusterStats())
			return false;
		// Compare every element.
		for (java.util.Iterator it = _ClusterStats.iterator(), it2 = inst._ClusterStats.iterator(); 
			it.hasNext() && it2.hasNext(); ) {
			com.sun.enterprise.admin.monitor.stats.lb.ClusterStats element = (com.sun.enterprise.admin.monitor.stats.lb.ClusterStats)it.next();
			com.sun.enterprise.admin.monitor.stats.lb.ClusterStats element2 = (com.sun.enterprise.admin.monitor.stats.lb.ClusterStats)it2.next();
			if (!(element == null ? element2 == null : element.equals(element2)))
				return false;
		}
		return true;
	}

	public int hashCode() {
		int result = 17;
		result = 37*result + (_ClusterStats == null ? 0 : _ClusterStats.hashCode());
		return result;
	}

	public String toString() {
		java.io.StringWriter sw = new java.io.StringWriter();
		try {
			writeNode(sw, "LoadBalancerStats", "");
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
