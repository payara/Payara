/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.security.SecurityPermission;
import javax.security.jacc.PolicyContextHandler;

/**
 * This class is created by the container and handed over to the 
 * JACC provider. This lets the jacc provider to use the information
 * in making authorization decisions, if it wishes to do so.
 * @author Harpreet Singh
 * @author Shing Wai Chan
 */
public class PolicyContextHandlerImpl implements PolicyContextHandler {

    public static final String HTTP_SERVLET_REQUEST =
        "javax.servlet.http.HttpServletRequest";
    public static final String SOAP_MESSAGE = "javax.xml.soap.SOAPMessage";
    public static final String ENTERPRISE_BEAN = "javax.ejb.EnterpriseBean";
    public static final String EJB_ARGUMENTS = "javax.ejb.arguments";
    public static final String SUBJECT = "javax.security.auth.Subject.container";
    public static final String REUSE = "java.security.Policy.supportsReuse";

    private static PolicyContextHandlerImpl pchimpl = null;

    private ThreadLocal thisHandlerData = new ThreadLocal();

    private PolicyContextHandlerImpl() {
    }

    private synchronized static PolicyContextHandlerImpl _getInstance() {
        if (pchimpl == null) {
            pchimpl = new PolicyContextHandlerImpl();
        }
        return pchimpl;
    }

    public static PolicyContextHandler getInstance() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }
    
        return _getInstance();
    }

    public boolean supports(String key) {
	String[] s = getKeys();
	for (int i = 0; i<s.length; i++) {
	    if (s[i].equalsIgnoreCase(key)) {
		return true;
            }
	}
        return false;
    }
    
    public String[] getKeys(){
	String[] s = {
	    HTTP_SERVLET_REQUEST,
	    SOAP_MESSAGE,
	    ENTERPRISE_BEAN,
	    SUBJECT,
	    EJB_ARGUMENTS,
            REUSE
	};
	return s;
    }
    
    public Object getContext(String key, Object data) {
        // ignore data Object
        return getHandlerData().get(key);
    }

    public HandlerData getHandlerData() {
        HandlerData handlerData = (HandlerData)thisHandlerData.get();
        if (handlerData == null) {
            handlerData = HandlerData.getInstance();
            thisHandlerData.set(handlerData);
        }
        return handlerData;
    }

    public void reset() {
         HandlerData handlerData = (HandlerData)thisHandlerData.get();
         if (handlerData != null) {
            handlerData.reset();
         }
         thisHandlerData.set(null);
    }
}
