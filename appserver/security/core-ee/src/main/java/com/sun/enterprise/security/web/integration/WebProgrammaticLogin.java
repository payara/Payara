/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.enterprise.security.web.integration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jvnet.hk2.annotations.Contract;

/**
 * Web specific Programmatic Login
 * An implementation of this will be injected into 
 * com.sun.appserv.security.api.ProgrammaticLogin
 */
@Contract
public interface WebProgrammaticLogin {
    
    /** 
     * Login and set up principal in request and session. This implements
     * programmatic login for servlets. 
     *
     * <P>Due to a number of bugs in RI the security context is not
     * shared between web container and ejb container. In order for an
     * identity established by programmatic login to be known to both
     * containers, it needs to be set not only in the security context but
     * also in the current request and, if applicable, the session object.
     * If a session does not exist this method does not create one.
     *
     * <P>See bugs 4646134, 4688449 and other referenced bugs for more
     * background.
     * 
     * <P>Note also that this login does not hook up into SSO.
     *
     * @param user User name to login.
     * @param password User password.
     * @param request HTTP request object provided by caller application. It
     *     should be an instance of HttpRequestFacade.
     * @param response HTTP response object provided by called application. It
     *     should be an instance of HttpServletResponse. This is not used
     *     currently.
     * @param realm the realm name to be authenticated to. If the realm is null, 
     * authentication takes place in default realm
     * @returns A Boolean object; true if login succeeded, false otherwise.
     * @see com.sun.enterprise.security.ee.auth.login.ProgrammaticLogin
     * @throws Exception on login failure.
     *
     */
     public Boolean login(String user, char[] password, String realm,
                                HttpServletRequest request,
                                HttpServletResponse response);


     /** 
     * Logout and remove principal in request and session.
     *
     * @param request HTTP request object provided by caller application. It
     *     should be an instance of HttpRequestFacade.
     * @param response HTTP response object provided by called application. It
     *     should be an instance of HttpServletResponse. This is not used
     *     currently.
     * @returns A Boolean object; true if login succeeded, false otherwise.
     * @see com.sun.enterprise.security.ee.auth.login.ProgrammaticLogin
     * @throws Exception any exception encountered during logout operation
     */
     public Boolean logout(HttpServletRequest request,
                                 HttpServletResponse response) throws Exception; 

}
