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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package	com.sun.enterprise.admin.monitor.callflow;

import java.util.List;
import java.util.Map;

/**
 * This	class provides a fallback implementation.
 */
public class AgentAdapter implements Agent {

    @Override
    public void	requestStart(RequestType requestType) {}
    @Override
    public void	addRequestInfo(RequestInfo requestInfo,	String value) {}
    @Override
    public void	requestEnd() {}
    @Override
    public void	startTime(ContainerTypeOrApplicationType type) {}
    @Override
    public void	endTime() {}

    @Override
    public void	ejbMethodStart(CallFlowInfo info) {}
    @Override
    public void	ejbMethodEnd(CallFlowInfo info)	{}

    @Override
    public void	webMethodStart(
	    String methodName, String applicationName, String moduleName,
	    String componentName, ComponentType	componentType,
	    String callerPrincipal) {}
    @Override
    public void	webMethodEnd(Throwable exception) {}

    @Override
    public void	entityManagerQueryStart(EntityManagerQueryMethod queryMethod) {}
    @Override
    public void	entityManagerQueryEnd()	{}

    @Override
    public void	entityManagerMethodStart(EntityManagerMethod entityManagerMethod) {}
    @Override
    public void	entityManagerMethodEnd() {}

    @Override
    public void	registerListener(Listener listener) {}
    @Override
    public void	unregisterListener(Listener listener) {}

    @Override
    public ThreadLocalData getThreadLocalData()	{
	return null;
    }

    @Override
    public void	setEnable(boolean enable) {}

    @Override
    public boolean isEnabled() {return false;}

    @Override
    public void	setCallerIPFilter(String ipAddress) {}

    @Override
    public void	setCallerPrincipalFilter(String	callerPrincipal) {}

    @Override
    public String getCallerPrincipalFilter() {
	return null;
    }

    @Override
    public String getCallerIPFilter() {
	return null;
    }

    @Override
    public void	clearData() {}

    @Override
    public boolean deleteRequestIds (String[] requestIds){
	return true;
    }

    @Override
    public List<Map<String, String>> getRequestInformation() {
	return null;
    }

    @Override
    public List<Map<String, String>> getCallStackForRequest(String requestId) {
	return null;
    }

    @Override
    public Map<String, String> getPieInformation (String requestID) {
	return null;
    }

}
