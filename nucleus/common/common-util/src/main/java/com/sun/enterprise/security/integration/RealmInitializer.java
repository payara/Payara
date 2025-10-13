/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019-2020] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.integration;

/**
 * Interface to facilitate Initialization of the injected Realm Instance with Application Descriptor info.
 *
 * <p>
 * See com.sun.enterprise.web.WebContainer and com.sun.web.security.RealmAdapter
 */
public interface RealmInitializer {

    /**
     * Initializes the internal state of this instance with provided parameters.
     *
     * @param bundleDescriptor - contains bundle-specific configuration
     * @param isSystemApp - realm may have different behavior for system applications
     * @param defaultRealmName - this realm name will be used as a default if there is not
     *            any other configured in the descriptor.
     */
    void initializeRealm(Object bundleDescriptor, boolean isSystemApp, String defaultRealmName);


    /**
     * Sets the realm's virtual server container.
     *
     * @param container
     */
    void setVirtualServer(Object container);

    /**
     * Reinitializes the web security manager.
     */
    void updateWebSecurityManager();

    /**
     * Clean up security and policy context.
     */
    void logout();
}
