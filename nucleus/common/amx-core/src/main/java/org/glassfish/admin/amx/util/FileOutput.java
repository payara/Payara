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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.glassfish.admin.amx.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
Directs output to a file. Lazy initialization; the file
is not actually opened until output is sent.
 */
public final class FileOutput implements Output
{
    private volatile PrintStream mOut;

    private final File mFile;

    private final boolean mAppend;

    public FileOutput(final File f)
    {
        this(f, false);
    }

    public FileOutput(final File f, boolean append)
    {
        mOut = null;
        mFile = f;
        mAppend = append;
    }

    private void lazyInit()
    {
        if (mOut == null)
        {
            synchronized (this)
            {
                if (mOut == null)
                {
                    try
                    {
                        if ((!mAppend) && mFile.exists() && !mFile.delete()) {
                                throw new IOException("cannot delete file: " + mFile);
                        }

                        mOut = new PrintStream(new FileOutputStream(mFile, mAppend));
                    }
                    catch (Exception e)
                    {
                        // don't use System.out/err; possible infinite recursion
                        throw new RuntimeException("Can't create file: " + mFile +
                                                   ", exception = " + e);
                    }
                }
            }
        }
    }

    @Override
    public void print(final Object o)
    {
        lazyInit();
        mOut.print(o.toString());
    }

    @Override
    public void println(Object o)
    {
        lazyInit();
        mOut.println(o.toString());
    }

    @Override
    public void printError(final Object o)
    {
        lazyInit();
        println("ERROR: " + o);
    }

    public boolean getDebug()
    {
        lazyInit();
        return (false);
    }

    @Override
    public void printDebug(final Object o)
    {
        lazyInit();
        println("DEBUG: " + o);
    }

    @Override
    public void close()
    {
        if (mOut != null)
        {
            try
            {
                mOut.close();
            }
            finally
            {
                mOut = null;
            }
        }
    }

}
