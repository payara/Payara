/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config;

import org.glassfish.grizzly.http.server.HttpHandler;

/**
 * Class represents context-root associated information
 */
public final class ContextRootInfo {

    /**
     * The interface, which is responsible for holding <tt>ContextRootInfo</tt>,
     * which makes possible to initialize <tt>ContextRootInfo<tt> lazily.
     */
    public static interface Holder {

        /**
         * Gets the Grizzly {@link HttpHandler}, associated with the context.
         *
         * @return the Grizzly {@link HttpHandler}, associated with the context.
         */
        public HttpHandler getHttpHandler();

        /**
         * Gets the application container, associated with the context.
         *
         * @return the application container, associated with the context.
         */
        public Object getContainer();
    }
    private final Holder holder;

    /**
     * Create <tt>ContextRootInfo</tt> using prepared {@link HttpHandler} and
     * application container parameters.
     * 
     * @param handler Grizzly {@link HttpHandler}, associated with the context.
     * @param container application container, associated with the context.
     */
    public ContextRootInfo(final HttpHandler handler,
            final Object container) {
        holder = new SimpleHolder(handler, container);
    }

    /**
     * Create <tt>ContextRootInfo</tt> using passed {@link Holder} object, which
     * might be initialized lazily.
     * 
     * @param holder context info {@link Holder}.
     */
    public ContextRootInfo(final Holder holder) {
        this.holder = holder;
    }

    /**
     * Gets the Grizzly {@link HttpHandler}, associated with the context.
     *
     * @return the Grizzly {@link HttpHandler}, associated with the context.
     */
    public HttpHandler getHttpHandler() {
        return holder.getHttpHandler();
    }

    /**
     * Gets the application container, associated with the context.
     *
     * @return the application container, associated with the context.
     */
    public Object getContainer() {
        return holder.getContainer();
    }

    private static class SimpleHolder implements Holder {

        private final HttpHandler handler;
        private final Object container;

        public SimpleHolder(HttpHandler handler, Object container) {
            this.handler = handler;
            this.container = container;
        }

        @Override
        public HttpHandler getHttpHandler() {
            return handler;
        }

        @Override
        public Object getContainer() {
            return container;
        }
    }
}
