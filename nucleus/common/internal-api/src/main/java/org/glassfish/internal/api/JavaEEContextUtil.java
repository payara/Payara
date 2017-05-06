/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.internal.api;

import org.jvnet.hk2.annotations.Contract;

/**
 * utility to create / push Java EE thread context
 * 
 * @author lprimak
 */
@Contract
public interface JavaEEContextUtil {
    /**
     * pushes Java EE invocation context
     *
     * @return old ClassLoader, or null if no invocation has been created
     */
    Context pushContext();

    /**
     * pushes invocation context onto the stack
     * Also creates Request scope
     *
     * @return new context that was created
     */
    RequestContext pushRequestContext();

    /**
     * context to pop from the stack
     *
     * @param ctx to be popped
     */
    void popContext(Context ctx);
    /**
     * context to pop from the stack
     *
     * @param context to be popped
     */
    void popRequestContext(RequestContext context);

    /**
     * @return application name or null if there is no invocation context
     */
    String getApplicationName();

    /**
     * set context class loader by appName
     *
     * @param appName
     */
    void setApplicationContext(String appName);

    /**
     * @return Class Loader that's associated with current invocation
     *         or null if there is no current invocation
     */
    ClassLoader getInvocationClassLoader();

    interface Context {};
    interface RequestContext {};
}
