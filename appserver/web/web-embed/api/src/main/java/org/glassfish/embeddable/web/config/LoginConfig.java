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
 * The class configures the authentication related parameters like,
 * authentication method, form login configuration, if authentication method
 * is form based authentication, the realm name and the realm type.
 *
 * <p/> Usage example:
 *
 * <pre>
 *      FormLoginConfig form = new FormLoginConfig("login.html", "error.html");
 *
 *      LoginConfig loginConfig = new LoginConfig();
 *      loginConfig.setAuthMethod(AuthMethod.FORM);
 *      loginConfig.setRealmName("userauth");
 *      loginConfig.setFormLoginConfig(form);
 * </pre>
 *
 * @see SecurityConfig
 *
 * @author Rajiv Mordani
 * @author Amy Roh
 */
public class LoginConfig {

    private AuthMethod authMethod;
    private FormLoginConfig formLoginConfig;
    private String realmName;

    public LoginConfig() {
    }

    public LoginConfig(AuthMethod authMethod, String name) {
        this.authMethod = authMethod;
        this.realmName = name;
    }

    /**
     * Set the authentication scheme to be used for a given
     * context
     *
     * @param authMethod one of the supported auth methods as
     * defined in <tt>AuthMethod</tt> enumeration
     */
    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }

    /**
     * Gets the auth method for the context
     * @return the authmethod for the context
     */
    public AuthMethod getAuthMethod() {
        return authMethod;
    }

    /**
     * Sets the realm name to be used for the context
     *
     * @param realmName the realm name for the context
     */
    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    /**
     * Gets the realm name set for the context
     *
     * @return the realm name for the context
     */
    public String getRealmName() {
        return realmName;
    }

    /**
     * Set the form login configuration, if the authentication
     * method is form based authentication
     *
     * @see FormLoginConfig
     * 
     * @param flc form login configuration
     */
    public void setFormLoginConfig(FormLoginConfig flc) {
        formLoginConfig = flc;
    }

    /**
     * Gets the form login config, or <tt>null</tt> if
     * the authentication scheme is not form based login.
     *
     * @see FormLoginConfig
     * 
     * @return form login configuration
     */
    public FormLoginConfig getFormLoginConfig() {
        return formLoginConfig;
    }

    /**
     * Returns a formatted string of the state.
     */
    public String toString() {
        StringBuffer toStringBuffer = new StringBuffer();
        toStringBuffer.append("LoginConfig: ");
        toStringBuffer.append(" authMethod: ").append(authMethod);
        toStringBuffer.append(" formLoginConfig: ").append(formLoginConfig);
        toStringBuffer.append(" realmName ").append(realmName);
        return toStringBuffer.toString();
    }

}
