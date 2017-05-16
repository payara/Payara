/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.connector;

import org.apache.catalina.LogFacade;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coyote implementation of the buffered reader.
 * 
 * @author Remy Maucherat
 */
public class CoyoteReader
    extends BufferedReader {


    // -------------------------------------------------------------- Constants


    private static final char[] LINE_SEP = { '\r', '\n' };
    private static final int MAX_LINE_LENGTH = 4096;

    private static final Logger log = LogFacade.getLogger();
    private static final ResourceBundle rb = log.getResourceBundle();

    // ----------------------------------------------------- Instance Variables


    protected InputBuffer ib;

    protected char[] lineBuffer = null;


    // ----------------------------------------------------------- Constructors


    public CoyoteReader(InputBuffer ib) {
        super(ib, 1);
        this.ib = ib;
    }

    // --------------------------------------------------------- Public Methods


    /**
    * Prevent cloning the facade.
    */
    protected Object clone()
        throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
    
    // -------------------------------------------------------- Package Methods


    /**
     * Clear facade.
     */
    void clear() {
        ib = null;
    }


    // --------------------------------------------------------- Reader Methods


    public void close()
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ib == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }
        ib.close();
    }


    public int read()
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ib == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }
        return ib.read();
    }


    public int read(char[] cbuf)
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ib == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }
        return ib.read(cbuf, 0, cbuf.length);
    }


    public int read(char[] cbuf, int off, int len)
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ib == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }
        return ib.read(cbuf, off, len);
    }


    public long skip(long n)
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ib == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }
        return ib.skip(n);
    }


    public boolean ready()
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ib == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }
        return ib.ready();
    }


    public boolean markSupported() {
        // Disallow operation if the object has gone out of scope
        if (ib == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }
        return true;
    }


    public void mark(int readAheadLimit)
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ib == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }
        ib.mark(readAheadLimit);
    }


    public void reset()
        throws IOException {
        // Disallow operation if the object has gone out of scope
        if (ib == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }
        ib.reset();
    }


    public String readLine()
        throws IOException {

        // Disallow operation if the object has gone out of scope
        if (ib == null) {
            throw new IllegalStateException(rb.getString(LogFacade.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        if (lineBuffer == null) {
            lineBuffer = new char[MAX_LINE_LENGTH];
        }

        String result = null;

        int pos = 0;
        int end = -1;
        int skip = -1;
        StringBuilder aggregator = null;
        while (end < 0) {
            mark(MAX_LINE_LENGTH);
            while ((pos < MAX_LINE_LENGTH) && (end < 0)) {
                int nRead = read(lineBuffer, pos, MAX_LINE_LENGTH - pos);
                if (nRead < 0) {
                    if (pos == 0 && aggregator == null) {
                        return null;
                    }
                    end = pos;
                    skip = pos;
                }
                for (int i = pos; (i < (pos + nRead)) && (end < 0); i++) {
                    if (lineBuffer[i] == LINE_SEP[0]) {
                        end = i;
                        skip = i + 1;
                        char nextchar;
                        if (i == (pos + nRead - 1)) {
                            nextchar = (char) read();
                        } else {
                            nextchar = lineBuffer[i+1];
                        }
                        if (nextchar == LINE_SEP[1]) {
                            skip++;
                        }
                    } else if (lineBuffer[i] == LINE_SEP[1]) {
                        end = i;
                        skip = i + 1;
                    }
                }
                if (nRead > 0) {
                    pos += nRead;
                }
            }
            if (end < 0) {
                if (aggregator == null) {
                    aggregator = new StringBuilder();
                }
                aggregator.append(lineBuffer);
                pos = 0;
            } else {
                reset();
                if (skip(skip) != skip && log.isLoggable(Level.WARNING)) {
                    log.log(Level.WARNING, LogFacade.FAILED_SKIP_CHARS_IN_BUFFER, skip);
                }
            }
        }

        if (aggregator == null) {
            result = new String(lineBuffer, 0, end);
        } else {
            aggregator.append(lineBuffer, 0, end);
            result = aggregator.toString();
        }

        return result;

    }


}
