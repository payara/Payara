/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
Useful utilities for Exceptions
 */
public final class ExceptionUtil
{
    private ExceptionUtil()
    {
        // disallow instantiation
    }

    public static String getStackTrace()
    {
        return toString(new Exception("STACK TRACE"));
    }

    public static String toString(final Throwable t)
    {
        final String SEP = System.getProperty("line.separator");

        final Throwable rootCause = getRootCause(t);

        return rootCause.getClass().getName() + ": " +
               StringUtil.quote(rootCause.getMessage()) + SEP +
               getStackTrace(rootCause);
    }

    /** String from t.getMessage() */
    public static final String MESSAGE_KEY = "MessageKey";

    /** Classname of the exception */
    public static final String CLASSNAME_KEY = "ClassnameKey";

    /** StackTraceElement[] */
    public static final String STACK_TRACE_KEY = "StackTraceKey";

    /** String version of the stack trace */
    public static final String STACK_TRACE_STRING_KEY = "StackTraceStringKey";

    /** java.lang.Throwable:  value is present iff the class is in packages found in {@link #OVER_THE_WIRE_PACKAGE_PREFIXES} */
    public static final String EXCEPTION_KEY = "ExceptionKey";

    /**
    Package prefixes acceptable for passing back a Throwable object so as to avoid
    a ClassNotFoundException on a client.
     */
    public static final Set<String> OVER_THE_WIRE_PACKAGE_PREFIXES = SetUtil.newUnmodifiableStringSet(
            "java.",
            "javax.",
            "org.omg.");

    public static boolean isAcceptableOverTheWire(final Throwable t)
    {
        boolean goodForOverTheWire = false;
        final String classname = t.getClass().getName();

        for (final String prefix : OVER_THE_WIRE_PACKAGE_PREFIXES)
        {
            if (classname.startsWith(prefix))
            {
                goodForOverTheWire = true;
                break;
            }
        }
        return goodForOverTheWire;
    }

    /**
    Return a Map with constituent parts including:
    <ul>
    <li>{@link #MESSAGE_KEY}</li>
    <li>{@link #STACK_TRACE_KEY}</li>
    <li>{@link #STACK_TRACE_STRING_KEY}</li>
    <li>{@link #EXCEPTION_KEY}</li>
    </ul>
    Caller should generally use Exceptionutil.toMap( ExceptionUtil.getRootCause(t) )
     */
    public static Map<String, Object> toMap(final Throwable t)
    {
        final Map<String, Object> m = new HashMap<String, Object>();

        final Throwable rootCause = getRootCause(t);

        final String classname = rootCause.getClass().getName();
        m.put(CLASSNAME_KEY, classname);

        // include the root cause Throwable if it's an acceptable class for over-the-wire
        if (isAcceptableOverTheWire(rootCause))
        {
            m.put(EXCEPTION_KEY, rootCause);
        }

        String msg = rootCause.getMessage();
        if (msg == null || msg.length() == 0)
        {
            msg = classname;
        }

        m.put(MESSAGE_KEY, msg);

        m.put(STACK_TRACE_KEY, rootCause.getStackTrace());
        m.put(STACK_TRACE_STRING_KEY, getStackTrace(rootCause));

        return m;
    }

    /**
    Get the chain of exceptions via getCause(). The first element is the
    Exception passed.

    @param start	the Exception to traverse
    @return		a Throwable[] or an Exception[] as appropriate
     */
    public static Throwable[] getCauses(final Throwable start)
    {
        final ArrayList<Throwable> list = new ArrayList<Throwable>();

        boolean haveNonException = false;

        Throwable t = start;
        while (t != null)
        {
            list.add(t);

            if (!(t instanceof Exception))
            {
                haveNonException = true;
            }

            final Throwable temp = t.getCause();
            if (temp == null)
            {
                break;
            }
            t = temp;
        }

        final Throwable[] results = haveNonException ? new Throwable[list.size()] : new Exception[list.size()];

        list.toArray(results);

        return (results);
    }

    /**
    Get the original troublemaker.

    @param e	the Exception to dig into
    @return		the original Throwable that started the problem
     */
    public static Throwable getRootCause(final Throwable e)
    {
        final Throwable[] causes = getCauses(e);

        return (causes[causes.length - 1]);
    }

    /**
    Get the stack trace as a String.

    @param t	the Throwabe whose stack trace should be gotten
    @return		a String containing the stack trace
     */
    public static String getStackTrace(Throwable t)
    {
        final StringBuffer buf = new StringBuffer();
        final StackTraceElement[] elems = t.getStackTrace();

        for (int i = 0; i < elems.length; ++i)
        {
            buf.append(elems[i]);
            buf.append("\n");
        }


        return (buf.toString());
    }

}

