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

package org.glassfish.admin.amx.util;

import java.lang.reflect.Constructor;
import java.util.Set;

/**
Maps a Throwable to another one in order to avoid the transfer
of non-standard (proprietary) Exception types, which could result in
ClassNotFoundException on remote clients.
<p>
Any Throwable which either is, or contains,
a Throwable which is not in the allowed packages is converted.
 */
public final class ThrowableMapper
{
    final Throwable mOriginal;

    /**
    By default, any Throwable whose package does not start with one
    of these packages must be mapped to something standard.
     */
    private final static Set<String> OK_PACKAGES =
            SetUtil.newUnmodifiableStringSet("java.", "javax.");

    public ThrowableMapper(final Throwable t)
    {
        mOriginal = t;
    }

    private static boolean shouldMap(final Throwable t)
    {
        final String tClass = t.getClass().getName();

        boolean shouldMap = true;

        for (final String prefix : OK_PACKAGES)
        {
            if (tClass.startsWith(prefix))
            {
                shouldMap = false;
                break;
            }
        }

        return (shouldMap);
    }

    public static Throwable map(final Throwable t)
    {
        Throwable result = t;

        if (t != null)
        {
            final Throwable tCause = t.getCause();
            final Throwable tCauseMapped = map(tCause);

            // if either this Exception or its cause needs/was mapped,
            // then we must form a new Exception

            if (shouldMap(t))
            {
                // the Throwable itself needs to be mapped
                final String msg = t.getMessage();

                if (t instanceof Error)
                {
                    result = new Error(msg, tCauseMapped);
                }
                else if (t instanceof RuntimeException)
                {
                    result = new RuntimeException(msg, tCauseMapped);
                }
                else if (t instanceof Exception)
                {
                    result = new Exception(msg, tCauseMapped);
                }
                else
                {
                    result = new Throwable(msg, tCauseMapped);
                }

                result.setStackTrace(t.getStackTrace());
            }
            else if (tCauseMapped != tCause)
            {
                // the Throwable doesn't need mapping, but its Cause does
                // create a Throwable of the same class, and insert its
                // cause and stack trace.
                try
                {
                    final Constructor<? extends Throwable> c =
                            t.getClass().getConstructor(String.class, Throwable.class);
                    result = c.newInstance(t.getMessage(), tCauseMapped);
                }
                catch (final Throwable t1)
                {
                    try
                    {
                        final Constructor<? extends Throwable> c =
                                t.getClass().getConstructor(String.class);
                        result = c.newInstance(t.getMessage());
                        result.initCause(tCauseMapped);
                    }
                    catch (final Throwable t2)
                    {
                        result = new Throwable(t.getMessage(), tCauseMapped);
                    }
                }

                result.setStackTrace(tCause.getStackTrace());
            }
            else
            {
                result = t;
            }
        }

        return (result);
    }

    /**
    Map the original Throwable to one that is non-proprietary (standard).
    Possible results include java.lang.Exception, java.lang.RuntimeException,
    java.lang.Error.  The original stack trace and exception chain is
    preserved, each element in that chain being mapped if necessary.

    @return a Throwable which uses only standard classes
     */
    public Throwable map()
    {
        return (map(mOriginal));
    }

}








