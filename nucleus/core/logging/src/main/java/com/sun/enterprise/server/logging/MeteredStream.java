/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

// Portions Copyright [2016-2020] [Payara Foundation and/or affiliates]
package com.sun.enterprise.server.logging;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is copied from java.util.logging.FileHandler
 * A metered stream is a subclass of OutputStream that
 * <ul>
 * <li>forwards all its output to a target stream
 * <li>keeps track of how many bytes have been written
 * </ul>
 */
final class MeteredStream extends OutputStream {

    private volatile boolean isOpen;
    OutputStream out;
    long written;

    MeteredStream(OutputStream out, long written) {
        this.out = out;
        this.written = written;
        isOpen = true;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        written++;
    }

    @Override
    public void write(byte[] buff) throws IOException {
        out.write(buff);
        written += buff.length;
    }

    @Override
    public void write(byte[] buff, int off, int len) throws IOException {
        out.write(buff, off, len);
        written += len;
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (isOpen) {
            isOpen = false;
            flush();
            out.close();
        }
    }
}
