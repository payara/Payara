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

package com.sun.enterprise.security.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jvnet.hk2.annotations.Contract;

/**
 *
 * @author kumar.jayanti
 */
@Contract
public interface ProgrammaticLoginInterface {

    /**
     * Attempt to login.
     *
     * <P>Upon successful return from this method the SecurityContext will
     * be set in the name of the given user as its Subject.
     *
     * <p>On client side, realm and errors parameters will be ignored and
     * the actual login will not occur until we actually access a resource
     * requiring a login.  And a java.rmi.AccessException with
     * COBRA NO_PERMISSION will occur when actual login is failed.
     *
     * <P>This method is intented primarily for EJBs wishing to do
     * programmatic login. If servlet code used this method the established
     * identity will be propagated to EJB calls but will not be used for
     * web container manager authorization. In general servlets should use
     * the servlet-specific version of login instead.
     *
     * @param user User name.
     * @param password Password for user.
     * @param realm the realm name in which the user should be logged in.
     * @param errors errors=true, propagate any exception encountered to the user
     * errors=false, no exceptions are propagated.
     * @return Boolean containing true or false to indicate success or
     * failure of login.
     * @throws Exception any exception encountered during Login.
     */
    Boolean login(final String user, final String password, final String realm, boolean errors) throws Exception;

    /**
     * Attempt to login.
     *
     * <P>Upon successful return from this method the SecurityContext will
     * be set in the name of the given user as its Subject.
     *
     * <p>On client side, the actual login will not occur until we actually
     * access a resource requiring a login.  And a java.rmi.AccessException
     * with COBRA NO_PERMISSION will occur when actual login is failed.
     *
     * <P>This method is intented primarily for EJBs wishing to do
     * programmatic login. If servlet code used this method the established
     * identity will be propagated to EJB calls but will not be used for
     * web container manager authorization. In general servlets should use
     * the servlet-specific version of login instead.
     *
     * @param user User name.
     * @param password Password for user.
     * @return Boolean containing true or false to indicate success or
     * failure of login.
     */
    Boolean login(final String user, final String password);

    /**
     * Attempt to login. This method is specific to servlets (and JSPs).
     *
     * <P>Upon successful return from this method the SecurityContext will
     * be set in the name of the given user as its Subject. In addition, the
     * principal stored in the request is set to the user name. If a session
     * is available, its principal is also set to the user provided.
     *
     * @returns Boolean containing true or false to indicate success or
     * failure of login.
     * @param realm
     * @param errors
     * @param user User name.
     * @param password Password for user.
     * @param request HTTP request object provided by caller application. It
     * should be an instance of HttpRequestFacade.
     * @param response HTTP response object provided by called application. It
     * should be an instance of HttpServletResponse.
     * @throws Exception any exceptions encountered during login
     * @return Boolean indicating true for successful login and false otherwise
     */
    Boolean login(final String user, final String password, final String realm, final HttpServletRequest request, final HttpServletResponse response, boolean errors) throws Exception;

    /**
     * Attempt to login. This method is specific to servlets (and JSPs).
     *
     * <P>Upon successful return from this method the SecurityContext will
     * be set in the name of the given user as its Subject. In addition, the
     * principal stored in the request is set to the user name. If a session
     * is available, its principal is also set to the user provided.
     *
     * @param user User name.
     * @param password Password for user.
     * @param request HTTP request object provided by caller application. It
     * should be an instance of HttpRequestFacade.
     * @param response HTTP response object provided by called application. It
     * should be an instance of HttpServletResponse.
     * @return Boolean containing true or false to indicate success or
     * failure of login.
     *
     */
    Boolean login(final String user, final String password, final HttpServletRequest request, final HttpServletResponse response);

    /**
     * Attempt to logout.
     * @returns Boolean containing true or false to indicate success or
     * failure of logout.
     *
     */
    Boolean logout();

    /**
     * Attempt to logout.
     * @param errors, errors = true, the method will propagate the exceptions
     * encountered while logging out, errors=false will return a Boolean value
     * of false indicating failure of logout
     * @return Boolean containing true or false to indicate success or
     * failure of logout.
     * @throws Exception encountered while logging out, if errors==false
     *
     */
    Boolean logout(boolean errors) throws Exception;

    /**
     * Attempt to logout. Also removes principal from request (and session
     * if available).
     *
     * @returns Boolean containing true or false to indicate success or
     * failure of logout.
     *
     */
    Boolean logout(final HttpServletRequest request, final HttpServletResponse response);

    /**
     * Attempt to logout. Also removes principal from request (and session
     * if available).
     * @param errors, errors = true, the method will propagate the exceptions
     * encountered while logging out, errors=false will return a Boolean value
     * of false indicating failure of logout
     *
     * @return Boolean containing true or false to indicate success or
     * failure of logout.
     * @throws Exception, exception encountered while logging out and if errors
     * == true
     */
    Boolean logout(final HttpServletRequest request, final HttpServletResponse response, boolean errors) throws Exception;

}
