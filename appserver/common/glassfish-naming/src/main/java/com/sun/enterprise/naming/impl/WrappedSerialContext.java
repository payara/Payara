/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.naming.impl;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import java.util.Hashtable;

/**
 * This is a Wrapper for {@link SerialContext}.
 * This is used by {@link SerialInitContextFactory} when NamingManager is set
 * up with an InitialContextFactoryBuilder. The reason for having this class
 * is described below:
 * When there is no builder setup, {@link InitialContext} uses a discovery
 * mechanism to handle URL strings as described in
 * {@link NamingManager#getURLContext(String, java.util.Hashtable)}. But,
 * when a builder is set up, it by-passes this logic and delegates to whatever
 * Context is returned by
 * builder.createInitialContextFactory(env).getInitialContext(env).
 * In our case, this results in SerialContext, which does not know how to handle
 * all kinds of URL strings. So, we want to returns a WrapperSerialContext
 * that delegates to appropriate URLContext whenever possible.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class WrappedSerialContext extends InitialContext
{
    /*
     * Implementation Note:
     * It extends InitialContext and overrides getURLOrDefaultInitCtx methods.
     * This is a very sensitive class. Take extreme precautions while changing.
     */

    // Not for public use.
    /* prackage */ WrappedSerialContext(Hashtable environment,
                                        SerialContext serialContext)
                                        throws NamingException
    {
        super(environment);
        defaultInitCtx = serialContext; // this is our default context
        gotDefault = true;
    }

    @Override
    protected void init(Hashtable environment) throws NamingException
    {
        // Don't bother merging with application resources  or system
        // properties, as that has already happened when user called
        // new InitialContext. So, just store it.
        myProps = environment;
    }

    @Override
    protected Context getDefaultInitCtx() throws NamingException
    {
        return defaultInitCtx;
    }

    @Override
    protected Context getURLOrDefaultInitCtx(String name) throws NamingException
    {
        String scheme = getURLScheme(name);
        if (scheme != null)
        {
            Context ctx = NamingManager.getURLContext(scheme, myProps);
            if (ctx != null)
            {
                return ctx;
            }
        }
        return getDefaultInitCtx();
    }

    @Override
    protected Context getURLOrDefaultInitCtx(Name name) throws NamingException
    {
        if (name.size() > 0)
        {
            String first = name.get(0);
            String scheme = getURLScheme(first);
            if (scheme != null)
            {
                Context ctx = NamingManager.getURLContext(scheme, myProps);
                if (ctx != null)
                {
                    return ctx;
                }
            }
        }
        return getDefaultInitCtx();
    }

    /**
     * Return URL scheme component from this string. Returns null if there
     * is no scheme.
     *
     * @param str
     * @return
     * @see javax.naming.spi.NamingManager#getURLScheme
     */
    private static String getURLScheme(String str)
    {
        // Implementation is copied from
        // javax.naming.spi.NamingManager#getURLScheme
        int colon_posn = str.indexOf(':');
        int slash_posn = str.indexOf('/');

        if (colon_posn > 0 && (slash_posn == -1 || colon_posn < slash_posn))
        {
            return str.substring(0, colon_posn);
        }
        return null;
    }

}
