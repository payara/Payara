/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package org.glassfish.internal.api;

import java.io.Serializable;
import org.glassfish.internal.data.ModuleInfo;

/**
 * Utility to create / push Jakarta EE and CDI thread contexts
 *
 * Example:
 * @Inject ContextProducer producer;
 * // EJB/CDI thread:
 * Instance saved = producer.currentInvocation*();
 * // insure 'saved' is not leaked when application undeployed,
 * // otherwise use producer.fromComponentId(producer.getInvocationComponentId())
 * // and in another, non EJB/CDI thread:
 * try (Context ctx = saved.pushRequestContext()) {
 *     // runs with EJB / CDI context
 * }
 *
 * @author lprimak
 */
public interface ContextProducer {
    /**
     * Creates an empty instance, i.e. if the empty context is pushed
     * on top of another context, the other context will be 'suppressed'
     * for the duration of this context
     *
     * @return new empty instance
     */
    Instance empty();

    /**
     * captures current invocation and returns it as an instance
     *
     * @return new captured instance
     */
    Instance currentInvocation() throws IllegalStateException;

    /**
     * @return Class Loader that's associated with current invocation or null if
     * there is no current invocation
     */
    ClassLoader getInvocationClassLoader();

    /**
     * @return component ID for the current invocation or null
     */
    String getInvocationComponentId();

    /**
     * This is different from class loaded, as there are some situations
     * where class is loaded but initialization is not complete,
     * such as CDI initializations, extensions start, etc.
     *
     * @return true if current invocation exists and is loaded / ready
     */
    boolean isInvocationLoaded();

    /**
     * Checks if the supplied module matches the component / application ID
     * Works for both EAR application IDs or other module (WAR, JAR) IDs
     *
     * @param moduleInfo
     * @param appOrComponentId
     * @return true if matches
     */
    boolean moduleMatches(ModuleInfo moduleInfo, String appOrComponentId);

    /**
     * specific, immutable, thread-safe instance of the context
     */
    interface Instance extends Serializable {
        /**
         * pushes Java EE invocation context onto the invocation stack use
         * try-with-resources to pop the context
         * no-op if non-running context
         *
         * @return the new context that was created
         */
        Context pushContext();

        /**
         * pushes invocation context onto the stack Also creates Request scope
         * use try-with-resources to pop the context
         * no-op if non-running context
         *
         * @return new context that was created
         */
        Context pushRequestContext();

        /**
         * set context class loader by component id of this instance
         * for empty or unloaded component, class loader remains unset and the
         * context is a no-op (no re-set gets done) so it's a no-op
         *
         * @return context so class loader can be reset
         */
        Context setApplicationClassLoader();

        /**
         * @return component ID for the current instance, or null if empty instance
         */
        String getInstanceComponentId();

        /**
         * This is different from class loaded, as there are some situations
         * where class is loaded but initialization is not complete, such as CDI
         * initializations, extensions start, etc.
         *
         * @return true if component is loaded and starting
         */
        boolean isLoaded();

        /**
         * @return true if this is an empty context
         */
        boolean isEmpty();

        /**
         * remove cached invocation from this instance, in case the
         * underlying app unloaded but component ID remains, just in case the
         * app is reloaded
         */
        void clearInstanceInvocation();
    }

    interface Context extends Closeable {
        boolean isValid();
    };

    interface Closeable extends AutoCloseable {
        @Override
        public void close();
    }
}
