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

import org.glassfish.admin.amx.util.Output;
import org.glassfish.admin.amx.util.StringUtil;

/**
Convenient wrapper around {@link AMXDebug}.
Can be made non-final if necessary; declared as 'final' until needed.
Note that the "-DAMX-DEBUG=true" must be set in order to see any output.
 */
public final class AMXDebugHelper
{
    private final Output mOutput;

    private final String mName;

    volatile boolean mEchoToStdOut;

    public AMXDebugHelper(final String name)
    {
        mOutput = AMXDebug.getInstance().getOutput(name);
        mName = name;

        mEchoToStdOut = false;
    }

    public AMXDebugHelper()
    {
        this("debug");
    }

    public boolean getEchoToStdOut(final boolean echo)
    {
        return mEchoToStdOut;
    }

    public void setEchoToStdOut(final boolean echo)
    {
        mEchoToStdOut = echo;
    }

    public boolean getDebug()
    {
        return AMXDebug.getInstance().getDebug(mName);
    }

    public void setDebug(final boolean debug)
    {
        AMXDebug.getInstance().setDebug(mName, debug);
    }

    private void printlnWithTime(final String s)
    {
        final long now = System.currentTimeMillis();
        final String msg = now + ": " + s;

        mOutput.println(msg);
        if (mEchoToStdOut)
        {
            System.out.println(msg);
        }
    }

    public void println(final Object o)
    {
        if (getDebug())
        {
            printlnWithTime("" + StringUtil.toString(o));
        }
    }

    public void println()
    {
        println("");
    }

    /**
    This form is preferred for multiple arguments so that String concatenation
    can be avoided when no message will actually be output. For example, use:
    <pre>println( a, b, c)</pre>
    instead of:
    <pre>println( a + b + c )</pre>
     */
    public void println(final Object... items)
    {
        if (getDebug() && items != null)
        {
            String msg = null;

            if (items.length == 1)
            {
                msg = StringUtil.toString(items[0]);
            }
            else
            {
                msg = StringUtil.toString("", items);
            }
            printlnWithTime(msg);
        }
    }

    public void dumpStack(final String msg)
    {
        if (getDebug())
        {
            println();
            println("STACK DUMP FOLLOWS: " + msg);
            println(StringUtil.toString(new Exception("not a real exception")));
            println();
        }
    }

}




