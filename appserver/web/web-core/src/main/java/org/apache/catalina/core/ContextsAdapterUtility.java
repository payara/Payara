/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.apache.catalina.core;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

/**
 * Utility class to adapt:
 * {@link Context} to {@link org.glassfish.grizzly.http.server.naming.NamingContext} and
 * {@link DirContext} to {@link org.glassfish.grizzly.http.server.naming.DirContext}.
 */
public class ContextsAdapterUtility {

    /**
     * Wraps {@link Context} and returns corresponding Grizzly
     * {@link org.glassfish.grizzly.http.server.naming.NamingContext}.
     *
     * @param namingContext {@link Context} to wrap.
     * @return {@link org.glassfish.grizzly.http.server.naming.NamingContext}
     */
    public static org.glassfish.grizzly.http.server.naming.NamingContext wrap(
            final Context namingContext) {
        if (namingContext == null) {
            return null;
        }
        
        return new NamingContextAdapter(namingContext);
    }

    /**
     * Unwraps Grizzly
     * {@link org.glassfish.grizzly.http.server.naming.NamingContext} and returns
     * internal {@link Context}.
     *
     * @param grizzlyNamingContext {@link org.glassfish.grizzly.http.server.naming.NamingContext}
     * @return {@link Context}
     * @throws IllegalArgumentException if passed Grizzly
     * {@link final org.glassfish.grizzly.http.server.naming.NamingContext} is
     * of unknown type (wasn't wrapped by this utility class).
     */
    public static Context unwrap(
            final org.glassfish.grizzly.http.server.naming.NamingContext grizzlyNamingContext) {
        if (grizzlyNamingContext == null) {
            return null;
        }
        
        if (!(grizzlyNamingContext instanceof NamingContextAdapter)) {
            throw new IllegalArgumentException("Unknown NamingContext type: " +
                    grizzlyNamingContext.getClass().getName());
        }
        return ((NamingContextAdapter) grizzlyNamingContext).getJmxNamingContext();
    }
    
    /**
     * Wraps {@link DirContext} and returns corresponding Grizzly
     * {@link org.glassfish.grizzly.http.server.naming.DirContext}.
     *
     * @param dirContext {@link DirContext} to wrap.
     * @return {@link org.glassfish.grizzly.http.server.naming.DirContext}
     */
    public static org.glassfish.grizzly.http.server.naming.DirContext wrap(
            final DirContext dirContext) {
        if (dirContext == null) {
            return null;
        }
        
        return new DirContextAdapter(dirContext);
    }

    /**
     * Unwraps Grizzly
     * {@link org.glassfish.grizzly.http.server.naming.DirContext} and returns
     * internal {@link DirContext}.
     *
     * @param grizzlyDirContext {@link org.glassfish.grizzly.http.server.naming.DirContext}
     * @return {@link DirContext}
     * @throws IllegalArgumentException if passed Grizzly
     * {@link final org.glassfish.grizzly.http.server.naming.DirContext} is not
     * of unknown type (wasn't wrapped by this utility class).
     */
    public static DirContext unwrap(
            final org.glassfish.grizzly.http.server.naming.DirContext grizzlyDirContext) {
        
        if (grizzlyDirContext == null) {
            return null;
        }
        
        if (!(grizzlyDirContext instanceof DirContextAdapter)) {
            throw new IllegalArgumentException("Unknown DirContext type: " +
                    grizzlyDirContext.getClass().getName());
        }
        return ((DirContextAdapter) grizzlyDirContext).getJmxDirContext();
    }

    private static Object wrapIfNeeded(final Object resource) {
        if (resource == null) {
            return null;
        } else if (resource instanceof DirContext) {
            return wrap((DirContext) resource);
        } else if (resource instanceof Context) {
            return wrap((Context) resource);
        }

        return resource;
    }
    
    private static class NamingContextAdapter
            implements org.glassfish.grizzly.http.server.naming.DirContext {
        private final Context jmxNamingContext;

        private NamingContextAdapter(final Context jmxNamingContext) {
            this.jmxNamingContext = jmxNamingContext;
        }

        public Context getJmxNamingContext() {
            return jmxNamingContext;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object lookup(final String pathStr)
                throws org.glassfish.grizzly.http.server.naming.NamingException {
            try {
                return wrapIfNeeded(jmxNamingContext.lookup(pathStr));
            } catch (NamingException e) {
                throw new org.glassfish.grizzly.http.server.naming.NamingException(e);
            }
        }
    }
    
    private static class DirContextAdapter
            implements org.glassfish.grizzly.http.server.naming.DirContext {
        private final DirContext jmxDirContext;

        private DirContextAdapter(final DirContext jmxDirContext) {
            this.jmxDirContext = jmxDirContext;
        }

        public DirContext getJmxDirContext() {
            return jmxDirContext;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object lookup(final String pathStr)
                throws org.glassfish.grizzly.http.server.naming.NamingException {
            try {
                return wrapIfNeeded(jmxDirContext.lookup(pathStr));
            } catch (NamingException e) {
                throw new org.glassfish.grizzly.http.server.naming.NamingException(e);
            }
        }
    }
}
