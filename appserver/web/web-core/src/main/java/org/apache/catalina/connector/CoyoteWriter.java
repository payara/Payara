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

import org.apache.catalina.core.StandardServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ResourceBundle;


/**
 * Coyote implementation of the servlet writer.
 * 
 * @author Remy Maucherat
 * @author Kin-man Chung
 */
public class CoyoteWriter
    extends PrintWriter {

    private static final ResourceBundle rb = StandardServer.log.getResourceBundle();


    // -------------------------------------------------------------- Constants


    // No need for a do privileged block - every web app has permission to read
    // this by default
    private static final char[] LINE_SEP =
        System.getProperty("line.separator").toCharArray();



    // ----------------------------------------------------- Instance Variables


    protected OutputBuffer ob;
    protected boolean error = false;


    // ----------------------------------------------------------- Constructors


    public CoyoteWriter(OutputBuffer ob) {
        super(ob);
        this.ob = ob;
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
        ob = null;
    }

    /**
     * Recycle.
     */
    void recycle() {
        error = false;
    }


    // --------------------------------------------------------- Writer Methods


    public void flush() {

        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(CoyoteInputStream.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        if (error)
            return;

        try {
            ob.flush();
        } catch (IOException e) {
            error = true;
        }

    }


    public void close() {

        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(CoyoteInputStream.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        // We don't close the PrintWriter - super() is not called,
        // so the stream can be reused. We close ob.
        try {
            ob.close();
        } catch (IOException ex ) {
            // Ignore
        }
        error = false;

    }


    public boolean checkError() {
        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(CoyoteInputStream.OBJECT_INVALID_SCOPE_EXCEPTION));
        }
        flush();
        return error;
    }


    public void write(int c) {

        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(CoyoteInputStream.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        if (error)
            return;

        try {
            ob.write(c);
        } catch (IOException e) {
            error = true;
        }

    }


    public void write(char buf[], int off, int len) {

        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(CoyoteInputStream.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        if (error)
            return;

        try {
            ob.write(buf, off, len);
        } catch (IOException e) {
            error = true;
        }
    }


    public void write(char buf[]) {
	write(buf, 0, buf.length);
    }


    public void write(String s, int off, int len) {

        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(CoyoteInputStream.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        if (error)
            return;

        try {
            ob.write(s, off, len);
        } catch (IOException e) {
            error = true;
        }

    }


    public void write(String s) {
        write(s, 0, s.length());
    }


    public void write(byte[] buff, int off, int len) {

        // Disallow operation if the object has gone out of scope
        if (ob == null) {
            throw new IllegalStateException(rb.getString(CoyoteInputStream.OBJECT_INVALID_SCOPE_EXCEPTION));
        }

        if (error)
            return;

        try {
            ob.write(buff, off, len);
        } catch (IOException e) {
            error = true;
        }
    }


    // ---------------------------------------------------- PrintWriter Methods


    public void print(boolean b) {
        if (b) {
            write("true");
        } else {
            write("false");
        }
    }


    public void print(char c) {
        write(c);
    }


    public void print(int i) {
        write(String.valueOf(i));
    }


    public void print(long l) {
        write(String.valueOf(l));
    }


    public void print(float f) {
        write(String.valueOf(f));
    }


    public void print(double d) {
        write(String.valueOf(d));
    }


    public void print(char s[]) {
        write(s);
    }


    public void print(String s) {
        if (s == null) {
            s = "null";
        }
        write(s);
    }


    public void print(Object obj) {
        write(String.valueOf(obj));
    }


    public void println() {
        write(LINE_SEP);
    }


    public void println(boolean b) {
        print(b);
        println();
    }


    public void println(char c) {
        print(c);
        println();
    }


    public void println(int i) {
        print(i);
        println();
    }


    public void println(long l) {
        print(l);
        println();
    }


    public void println(float f) {
        print(f);
        println();
    }


    public void println(double d) {
        print(d);
        println();
    }


    public void println(char c[]) {
        print(c);
        println();
    }


    public void println(String s) {
        print(s);
        println();
    }


    public void println(Object o) {
        print(o);
        println();
    }
}
