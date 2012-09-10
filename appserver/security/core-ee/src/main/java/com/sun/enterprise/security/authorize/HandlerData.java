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

package com.sun.enterprise.security.authorize;


import javax.servlet.http.HttpServletRequest;

import com.sun.enterprise.security.ee.PermissionCacheFactory;
import com.sun.enterprise.security.SecurityContext;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.internal.api.Globals;


/**
 * This class implements a thread scoped data used for PolicyContext.
 * @author Harry Singh
 * @author Jyri Virkki
 * @author Shing Wai Chan
 *
 */
public class HandlerData {
    
    private HttpServletRequest httpReq = null;
    private ComponentInvocation inv = null;
    private PolicyContextDelegate ejbDelegate = null;
    
    private HandlerData(){
        ejbDelegate = Globals.getDefaultHabitat().getService(PolicyContextDelegate.class, "EJB");
    }


    public static HandlerData getInstance(){
	return new HandlerData();
    }

    public void setHttpServletRequest(HttpServletRequest httpReq) {
	this.httpReq = httpReq;
    }

    public void setInvocation(ComponentInvocation inv) {
        this.inv = inv;
    }
    public Object get(String key){
	if (PolicyContextHandlerImpl.HTTP_SERVLET_REQUEST.equalsIgnoreCase(key)){
	    return httpReq;
	} else if (PolicyContextHandlerImpl.SUBJECT.equalsIgnoreCase(key)){
	    return SecurityContext.getCurrent().getSubject();
	} else if (PolicyContextHandlerImpl.REUSE.equalsIgnoreCase(key)) {
            PermissionCacheFactory.resetCaches();
            return Integer.valueOf(0);
        }

        if (inv == null) {
            return null;
        }

        if (PolicyContextHandlerImpl.SOAP_MESSAGE.equalsIgnoreCase(key)) {
            return (ejbDelegate != null) ? ejbDelegate.getSOAPMessage(inv) : null;
        } else if (PolicyContextHandlerImpl.ENTERPRISE_BEAN.equalsIgnoreCase(key)) {
            return (ejbDelegate != null) ? ejbDelegate.getEnterpriseBean(inv) : null;
        } else if (PolicyContextHandlerImpl.EJB_ARGUMENTS.equalsIgnoreCase(key)) {
            return (ejbDelegate != null) ? ejbDelegate.getEJbArguments(inv) : null;
        }
	return null;
    }

    void reset() {
       httpReq = null;
       inv = null;
       ejbDelegate = null;
    }
}
