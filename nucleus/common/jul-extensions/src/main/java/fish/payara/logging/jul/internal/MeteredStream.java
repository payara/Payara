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
package fish.payara.logging.jul.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is copied from java.util.logging.FileHandler
 * A metered stream is a subclass of OutputStream that
 * <ul>
 * <li>forwards all its output to a target stream
 * <li>keeps track of how many bytes have been written
 * </ul>
 */
public final class MeteredStream extends OutputStream {

    private final OutputStream out;
    private final AtomicLong written;
    private volatile boolean isOpen;

    public MeteredStream(OutputStream out, long written) {
        this.out = out;
        this.written = new AtomicLong(written);
        isOpen = true;
    }

    /**
     * @return count of bytes written by this stream instance.
     */
    public long getBytesWritten() {
        return this.written.get();
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        written.incrementAndGet();
    }

    @Override
    public void write(byte[] buff) throws IOException {
        out.write(buff);
        written.addAndGet(buff.length);
    }

    @Override
    public void write(byte[] buff, int off, int len) throws IOException {
        out.write(buff, off, len);
        written.addAndGet(len);
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
