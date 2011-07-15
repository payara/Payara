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
 * AgentAdapter.java
 */
package	com.sun.enterprise.admin.monitor.callflow;

/**
 * This	class provides a fallback implementation.
 */
public class AgentAdapter implements Agent {

    public void	requestStart(RequestType requestType) {}
    public void	addRequestInfo(RequestInfo requestInfo,	String value) {}
    public void	requestEnd() {}
    public void	startTime(ContainerTypeOrApplicationType type) {}
    public void	endTime() {}

    public void	ejbMethodStart(CallFlowInfo info) {}
    public void	ejbMethodEnd(CallFlowInfo info)	{}

    public void	webMethodStart(
	    String methodName, String applicationName, String moduleName,
	    String componentName, ComponentType	componentType,
	    String callerPrincipal) {}
    public void	webMethodEnd(Throwable exception) {}

    public void	entityManagerQueryStart(EntityManagerQueryMethod queryMethod) {}
    public void	entityManagerQueryEnd()	{}

    public void	entityManagerMethodStart(EntityManagerMethod entityManagerMethod) {}
    public void	entityManagerMethodEnd() {}

    public void	registerListener(Listener listener) {}
    public void	unregisterListener(Listener listener) {}

    public ThreadLocalData getThreadLocalData()	{
	return null;
    }

    public void	setEnable(boolean enable) {}

    public boolean isEnabled() {return false;}

    public void	setCallerIPFilter(String ipAddress) {}

    public void	setCallerPrincipalFilter(String	callerPrincipal) {}

    public String getCallerPrincipalFilter() {
	return null;
    }

    public String getCallerIPFilter() {
	return null;
    }

    public void	clearData() {}

    public boolean deleteRequestIds (String[] requestIds){
	return true;
    }

    public java.util.List<java.util.Map<String,	String>>
	    getRequestInformation() {
	return null;
    }

    public java.util.List<java.util.Map<String,	String>>
	    getCallStackForRequest(String requestId) {
	return null;
    }

    public java.util.Map<String, String> getPieInformation (String requestID) {
	return null;
    }

}
