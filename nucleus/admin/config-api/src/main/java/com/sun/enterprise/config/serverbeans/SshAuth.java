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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.glassfish.api.Param;

import java.beans.PropertyVetoException;

/**
 * A cluster defines a homogeneous set of server instances that share the same
 * applications, resources, and configuration.
 */
@Configured
public interface SshAuth  extends ConfigBeanProxy {
 
    
    /**
     * points to a named host. 
     *
     * @return a named host name
     */
    @Attribute(defaultValue="${user.name}")
    String getUserName();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="sshuser", optional=true,defaultValue="${user.name}")
    void setUserName(String value) throws PropertyVetoException;

    /**
     * points to a named host.
     *
     * @return a named host name
     */

    @Attribute
    String getKeyfile();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="sshkeyfile", optional=true)
    void setKeyfile(String value) throws PropertyVetoException;

    /**
     * SSH Password
     *
     * @return SSH Password which may be a password alias of the form
     *         ${ALIAS=aliasname}
     */

    @Attribute
    String getPassword();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="sshpassword", optional=true)
    void setPassword(String value) throws PropertyVetoException;

    /**
     * SSH Keyfile passphrase
     *
     * @return SSH keyfile encryption passphrase which may be a password alias
     * of the form ${ALIAS=aliasname}
     */

    @Attribute
    String getKeyPassphrase();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="sshkeypassphrase", optional=true)
    void setKeyPassphrase(String value) throws PropertyVetoException;

}
