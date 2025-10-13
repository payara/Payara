/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.util;

import java.util.ArrayList;

/**
 * Useful utilities for Exceptions
 * Subset of methods copied from org.glassfish.admin.amx.util
 */
public final class ExceptionUtil
{
    private final static String DS_FAILURE_MESSAGE = "java.sql.SQLException: Error in allocating a connection. Cause: Connection could not be allocated";

    private ExceptionUtil()
    {
        // disallow instantiation
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
        final StringBuilder buf = new StringBuilder();
        final StackTraceElement[] elems = t.getStackTrace();

        for (StackTraceElement elem : elems) {
            buf.append(elem);
            buf.append("\n");
        }


        return (buf.toString());
    }

    public static boolean isDSFailure(Exception ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains(DS_FAILURE_MESSAGE)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

}

