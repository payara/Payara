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

package org.glassfish.embeddable.web.config;


/**
 * Class that is used for configuring form based login, when
 * the authentication method is set to <tt>FORM</tt> in <tt>LoginConfig</tt>.
 *
 * @see LoginConfig
 * @see AuthMethod
 *
 * @author Rajiv Mordani
 * @author Amy Roh
 */
public class FormLoginConfig {

    private String loginPage;
    private String errorPage;

    /**
     * Creates an instance of the <tt>FormLoginConfig</tt> with the specified <tt>loginPage</tt> and
     * <tt>errorPage</tt>
     * 
     * @param loginPage the login page
     * @param errorPage the form error page
     */
    public FormLoginConfig(String loginPage, String errorPage) {
        this.loginPage = loginPage;
        this.errorPage = errorPage;
    }

    /**
     * Gets the login page
     *
     * @return the login page for form based authentication as a <tt>String</tt>
     */
    public String getFormLoginPage() {
        return this.loginPage;
    }

    /**
     * Get the form error page
     *
     * @return the error page for form based authentication as a <tt>String</tt>
     */
    public String getFormErrorPage() {
        return this.errorPage;
    }

    /**
     * Returns a formatted string of the state.
     */
    @Override
    public String toString() {
        StringBuffer toStringBuffer = new StringBuffer();
        toStringBuffer.append("FormLoginConfig: ");
        toStringBuffer.append(" loginPage: ").append(loginPage);
        toStringBuffer.append(" errorPage: ").append(errorPage);
        return toStringBuffer.toString();
    }

}
