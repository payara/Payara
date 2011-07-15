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

package com.sun.enterprise.connectors.util;

import com.sun.enterprise.connectors.ConnectorRuntime;

import java.io.*;
import java.util.Locale;

/**
 * PrintWriter adapter that will be used by resource adapters
 */
public class PrintWriterAdapter extends PrintWriter implements Externalizable {

    private transient PrintWriter writer;

    public PrintWriterAdapter(PrintWriter writer) {
        super(writer); // since all the methods of super-class is overridden, writer will not be used.
        this.writer = writer;
    }

    /**
     * Used during de-serialization.
     */
    public PrintWriterAdapter() {
        this(getResourceAdapterLogWriter());
    }

    private static PrintWriter getResourceAdapterLogWriter() {
        return ConnectorRuntime.getRuntime().getResourceAdapterLogWriter();
    }

    public void initialize() {
        if (writer == null) {
            writer = getResourceAdapterLogWriter();
        }
    }

    public void flush() {
        initialize();
        writer.flush();
    }

    public void close() {
        initialize();
        writer.close();
    }

    public boolean checkError() {
        initialize();
        return writer.checkError();
    }

    public void write(int c) {
        initialize();
        writer.write(c);
    }

    public void write(char[] buf, int off, int len) {
        initialize();
        writer.write(buf, off, len);
    }

    public void write(char[] buf) {
        initialize();
        writer.write(buf);
    }

    public void write(String s, int off, int len) {
        initialize();
        writer.write(s, off, len);
    }

    public void write(String s) {
        initialize();
        writer.write(s);
    }

    public void print(boolean b) {
        initialize();
        writer.print(b);
    }

    public void print(char c) {
        initialize();
        writer.print(c);
    }

    public void print(int i) {
        initialize();
        writer.print(i);
    }

    public void print(long l) {
        initialize();
        writer.print(l);
    }

    public void print(float f) {
        initialize();
        writer.print(f);
    }

    public void print(double d) {
        initialize();
        writer.print(d);
    }

    public void print(char[] s) {
        initialize();
        writer.print(s);
    }

    public void print(String s) {
        initialize();
        writer.print(s);
    }

    public void print(Object obj) {
        initialize();
        writer.print(obj);
    }

    public void println() {
        initialize();
        writer.println();
    }

    public void println(boolean x) {
        initialize();
        writer.println(x);
    }

    public void println(char x) {
        initialize();
        writer.println(x);
    }

    public void println(int x) {
        initialize();
        writer.println(x);
    }

    public void println(long x) {
        initialize();
        writer.println(x);
    }

    public void println(float x) {
        initialize();
        writer.println(x);
    }

    public void println(double x) {
        initialize();
        writer.println(x);
    }

    public void println(char[] x) {
        initialize();
        writer.println(x);
    }

    public void println(String x) {
        initialize();
        writer.println(x);
    }

    public void println(Object x) {
        initialize();
        writer.println(x);
    }

    public PrintWriter printf(String format, Object... args) {
        initialize();
        return writer.printf(format, args);
    }

    public PrintWriter printf(Locale l, String format, Object... args) {
        initialize();
        return writer.printf(l, format, args);
    }

    public PrintWriter format(String format, Object... args) {
        initialize();
        return writer.format(format, args);
    }

    public PrintWriter format(Locale l, String format, Object... args) {
        initialize();
        return writer.format(l, format, args);
    }

    public PrintWriter append(CharSequence csq) {
        initialize();
        return writer.append(csq);
    }

    public PrintWriter append(CharSequence csq, int start, int end) {
        initialize();
        return writer.append(csq, start, end);
    }

    public PrintWriter append(char c) {
        initialize();
        return writer.append(c);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        writer = null;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        initialize();
    }
}
