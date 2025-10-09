/*
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *   Copyright (c) [2019] Payara Foundation and/or its affiliates.
 *   All rights reserved.
 *
 *   The contents of this file are subject to the terms of either the GNU
 *   General Public License Version 2 only ("GPL") or the Common Development
 *   and Distribution License("CDDL") (collectively, the "License").  You
 *   may not use this file except in compliance with the License.  You can
 *   obtain a copy of the License at
 *   https://github.com/payara/Payara/blob/main/LICENSE.txt
 *   See the License for the specific
 *   language governing permissions and limitations under the License.
 *
 *   When distributing the software, include this License Header Notice in each
 *   file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *   GPL Classpath Exception:
 *   The Payara Foundation designates this particular file as subject to the
 *   "Classpath" exception as provided by the Payara Foundation in the GPL
 *   Version 2 section of the License file that accompanied this code.
 *
 *   Modifications:
 *   If applicable, add the following below the License Header, with the fields
 *   enclosed by brackets [] replaced by your own identifying information:
 *   "Portions Copyright [year] [name of copyright owner]"
 *
 *   Contributor(s):
 *   If you wish your version of this file to be governed by only the CDDL or
 *   only the GPL Version 2, indicate your decision by adding "[Contributor]
 *   elects to include this software in this distribution under the [CDDL or GPL
 *   Version 2] license."  If you don't indicate a single choice of license, a
 *   recipient has the option to distribute your version of this file under
 *   either the CDDL, the GPL Version 2 or to extend the choice of license to
 *   its licensees as provided above.  However, if you add GPL Version 2 code
 *   and therefore, elected the GPL Version 2 license, then the option applies
 *   only if the new code is made subject to such option by the copyright
 *   holder.
 */

/**
 * This package contains much of the integration code for JASPIC.
 * 
 * <p>
 * JASPIC is the EE standard for taking care of the authentication aspects
 * of security. It allows for users to supply pluggable custom authentication mechanisms called SAMs
 * {@link jakarta.security.auth.message.module.ServerAuthModule}.
 * 
 * <p>
 * Unlike Servlet or EE Security there are no default authentication mechanisms in JASPIC.
 * 
 * <p>
 * Code in this package builds upon the general (server independent) JASPIC Provider 
 * Framework Reference Implementation (<code>org.glassfish.main.security:jaspic.provider.framework</code>).
 * It adds to this framework by implementing the Payara specific bits.
 * 
 * <p>
 * For Web/Servlet requests the authentication code is called from Catalina (Tomcat) via
 * <code>com.sun.web.security.RealmAdapter</code> and then <code>com.sun.web.security.realmadapter.JaspicRealm</code>.
 * 
 * <p>
 * The pluggable authentication mechanisms are managed by the 
 * {@link jakarta.security.auth.message.config.AuthConfigFactory}. The Payara specific implementation of this is
 * {@link com.sun.enterprise.security.jaspic.config.GFAuthConfigFactory}, which is installed by
 * <code>com.sun.enterprise.security.ee.JavaEESecurityLifecycle</code>.
 *
 */
package com.sun.enterprise.security.jaspic;