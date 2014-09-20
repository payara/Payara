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

/**
 */
public class DebugOutImpl implements DebugOut
{
    private final String mID;

    private boolean mDebug;

    private DebugSink mSink;

    public DebugOutImpl(
            final String id,
            final boolean debug,
            final DebugSink sink)
    {
        mID = id;
        mDebug = debug;

        mSink = sink == null ? new DebugSinkImpl(System.out) : sink;
    }

    public DebugOutImpl(
            final String id,
            final boolean debug)
    {
        this(id, debug, null);
    }

    public String getID()
    {
        return mID;
    }

    public boolean getDebug()
    {
        return mDebug;
    }

    public void print(final Object o)
    {
        mSink.print("" + o);
    }

    public void println(Object o)
    {
        mSink.println("" + o);
    }

    public String toString(final Object... args)
    {
        return StringUtil.toString(", ", args);
    }

    public void setDebug(final boolean debug)
    {
        mDebug = debug;
    }

    public void debug(final Object... args)
    {
        if (getDebug())
        {
            mSink.println(toString(args));
        }
    }

    public void debugMethod(
            final String methodName,
            final Object... args)
    {
        if (getDebug())
        {
            debug(methodString(methodName, args));
        }
    }

    public void debugMethod(
            final String msg,
            final String methodName,
            final Object... args)
    {
        if (getDebug())
        {
            debug(methodString(methodName, args) + ": " + msg);
        }
    }

    public static String methodString(
            final String name,
            final Object... args)
    {
        String result = null;

        if (args == null || args.length == 0)
        {
            result = name + "()";
        }
        else
        {
            final String argsString = StringUtil.toString(", ", args);
            result = StringUtil.toString("", name, "(", argsString, ")");
        }

        return result;
    }

}




























