/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * Agent.java
 * $Id: Agent.java,v 1.19 2006/11/08 20:55:16 harpreet Exp $
 * $Date: 2006/11/08 20:55:16 $
 * $Revision: 1.19 $
 */

package	com.sun.enterprise.admin.monitor.callflow;

import java.util.List;
import java.util.Map;

/**
 * This	interface exposes the call flow	agent API.
 *
 * This	is intended to be called by various container trap points. An
 * implementation of the call flow agent would collect the data	supplied, and
 * persist it, for later querying and analysis.
 *
 * Further, it is possible to set filters, based on client side	attributes
 * such	as caller IP address and caller	security principal (USER).
 *
 * The trap point call sequence	has a specific order:
 *
 *	{
 *	  requestStart,	addRequestInfo*, requestInfoComplete,
 *	  (startTime, (webMethodStart|ejbMethodStart))*,
 *	  ((ejbMethodEnd|webMethodEnd),	endTime)*,
 *	  requestEnd
 *	}
 *
 * Data	schema:	Tables
 *
 * RequestStart: { RequestId, Timestamp, CallerIPAddress, RemoteUser }
 * RequestEnd  : { RequestId, Timestamp	}
 * StartTime : { RequestId, TimeStamp, ContainerTypeOrApplicationType }
 * EndTime : { RequestId, TimeStamp, ContainerTypeOrApplicationType }
 * MethodStart : { MethodName, RequestId, Timestamp, ComponentType, ThreadId,
 *		   AppId, ModuleId, ComponentId, TransactionId,	SecurityId }
 * MethodEnd   : { RequestId, Timestamp, Exception }
 *
 * @author Ram Jeyaraman, Harpreet Singh, Nazrul Islam,	Siraj Ghaffar
 * @date March 21, 2005
 */
public interface Agent {

    /**
     * Call flow trap points.
     */

    /**
     * This method is called by	a request processor thread, dispatched by the
     * container to process a new request on behalf of a client, before	any
     * request processing activity begins.
     *
     * Allowed request types are:
     *
     * 1. Remote HTTP Web request.
     * 2. Remote EJB request.
     * 3. MDB request.
     * 4. Timer	EJB.
     *
     * Upon being called, this method allocates	a unique request id, and
     * associates it with the thread local state.
     *
     * @param requestType Type of the request.
     */
    public void	requestStart(RequestType requestType);

    /**
     * This method may be called by the	container during request dispatch,
     * to add request information such as caller IP address, and remote
     * user name. This method is not required to be called by the container.
     *
     * This method may be called many times by the container, but only before
     * <code>startTime</code> is called.
     */
    public void	addRequestInfo(RequestInfo requestInfo,	String value);

    /**
     * This method is called by	a request processor thread, after completion
     * of request processing. Upon being called, this method releases the
     * thread local state.
     */
    public void	requestEnd();

    /**
     * This method is called by	a request processor thread, when the thread
     * of execution transitions	into the container code. That is,
     * this method is called from a method preInvoke point, before the
     * container begins	method call setup activity.
     *
     * @param type Describes the type of the container or application.
     */
    public void	startTime(ContainerTypeOrApplicationType type);

    /**
     * This method is called by	a request processor thread, when the thread
     * of execution transitions	out of the container code. That	is,
     * this method is called from a method postInvoke point, after the
     * container has completed all method call related cleanup activity.
     */
    public void	endTime();

    // Method trap points

    /**
     * This method is called by	a request processor thread, before invoking a
     * business	method on a target EJB.	This trap point	must be	called after
     * the transaction and security context for	the invocation has been
     * established.
     *
     * This trap point collects	information about the target EJB invocation.
     *
     * @param info This	object encapsulates information	such as	method name,
     * component type, application name, module	name, component	name,
     * transaction id, and security id.
     */
    public void	ejbMethodStart(CallFlowInfo info);

    /**
     * This method is called by	a request processor thread, after invoking a
     * business	method on a target EJB.	This trap point	gathers	information
     * about the outcome of the	invocation such	as exception, if any.
     *
     * @param info This	object encapsulates information	about the outcome of
     * the invocation such as exception, if any.
     */
    public void	ejbMethodEnd(CallFlowInfo info);

    /**
     * This method is called by	a request processor thread, before invoking a
     * target method on	a filter or servlet. This trap point must be called
     * after the invocation context for	the invocation is established.
     *
     * This trap point collects	information such as method name, component
     * name, component type, application name, module name, callerPrincipal.
     */
    public void	webMethodStart(
	    String methodName, String applicationName, String moduleName,
	    String componentName, ComponentType	componentType,
	    String callerPrincipal);

    /**
     * This method is called by	a request processor thread, after invoking a
     * target method on	a filter or servlet. This trap point gathers information
     * on the outcome of the invocation	such as	exception, if any.
     */
    public void	webMethodEnd(Throwable exception);

    /**
     * This method is called the persistence container on a request processor
     * thread, before invoking a method	on the
     * @see javax.persistence.EntityManager EntityManager interface
     */
    public void	entityManagerMethodStart (EntityManagerMethod entityManagerMethod);

    /**
     * This method is called the persistence container on a request processor
     * thread, after invoking a	method on the
     * @see javax.persistence.EntityManager EntityManager interface
     */
    public void	entityManagerMethodEnd ();

    /**
     * This method is called the persistence container on a request processor
     * thread, before invoking a method	on the
     * @see javax.persistence.Query Query interface
     */
    public void	entityManagerQueryStart	(EntityManagerQueryMethod queryMethod);

    /**
     * This method is called the persistence container on a request processor
     * thread, after invoking a	method on the
     * @see javax.persistence.Query Query interface
     */
    public void	entityManagerQueryEnd ();

    /**
     * Data accessors.
     */

    /**
     * @return Callflow	thread local data.
     */
    public ThreadLocalData getThreadLocalData();

    /**
     * Support for notifications.
     */

    /**
     * Register	a listener.
     *
     * Registered listeners are	notified during	the following call trap	points:
     * { requestStart, requestEnd, methodStart,	methodEnd }.
     */
    public void	registerListener(Listener listener);

    /**
     * Unregister a listener.
     */
    public void	unregisterListener(Listener listener);

    /**
     * API to support AMX MBean	calls.
     */

    // Enablement APIs

    /**
     * Enable or disable call flow. This is typically called from AMX MBean.
     *
     * @param enable true, to turn on call flow.
     */
    public void	setEnable(boolean enable);

    /**
     * Get enabled information of call flow's persistent logging. Only user of
     * this API	is Web Services	Managament. Please check with author before
     * using this API.
     *
     * @return true if persistent logging is enabled, false otherwise.
     */
    public boolean isEnabled();

    // Filters

    /**
     * Specifies the IP	address	of the client, only for	which, call flow
     * information would be collected. That is,	call flow information is
     * gathered, only for calls	originating from the client IP address.
     * Others calls are	ignored.
     *
     * Note, there may be other	filters	that may be further applied to narrow
     * the scope of call flow data collection.
     */
    public void	setCallerIPFilter(String ipAddress);

    /**
     * Gets the	IP address of the client, only for which, call flow
     * information would be collected.
     */
    public String getCallerIPFilter ();

    /**
     * Specifies the caller principal, only for	which, call flow information
     * would be	collected. That	is, call flow information is gathered, only for
     * calls originating from the caller principal. Others calls are ignored.
     *
     * Note, there may be other	filters	that may be further applied to narrow
     * the scope of call flow data collection.
     */
    public void	setCallerPrincipalFilter(String	callerPrincipal);

    /**
     * Gets the	caller principal, only for which, call flow information
     * would be	collected.
     */
    public String getCallerPrincipalFilter();

    /**
     * Clear all accumulated data collected in the previous CallFlow runs
     * from the	database. This is only called from AMX APIs.
     */
    public void	clearData ();

    /**
     * Delete request ids from the database
     */
    public boolean deleteRequestIds (String[] requestIds);
    /**
     * @return a list of Map objects. Each entry in the	list contains
     * information pertaining to a unique request. Refer to AMX	MBean API
     * com.sun.appserv.management.monitor.CallFlowMonitor for more details.
     */
    public List<Map<String, String>> getRequestInformation();

    /**
     * @return a list of Map objects. The list contains	the ordered call stack
     * flow stack information. Refer to	AMX MBean API
     * com.sun.appserv.management.monitor.CallFlowMonitor for more details.
     */
    public List<Map<String, String>> getCallStackForRequest(String requestId);

    /**
     * @return a Map object containing time information	for a request,
     * showing the time	distribution across application	code and container code.
     * Refer to	AMX MBean API com.sun.appserv.management.monitor.CallFlowMonitor
     * for more	details.
     */
    public Map<String, String> getPieInformation(String	requestID);
}
