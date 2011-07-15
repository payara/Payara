/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.jmx.remote.streams;

import java.io.InputStream;
import java.io.IOException;
import java.io.DataInputStream;

public class JMXChunkedInputStream extends InputStream {
    private InputStream in = null;
    private byte[] buffer = null;
    private int remainderInBuf = 0;
    private int remainderInChannel = 0;
    private int idx = 0;
    private boolean EOF = false;

    public JMXChunkedInputStream(InputStream in) {
        this.in = in;
    }

    public void close() throws IOException {
        in.close();
    }

    public int available() {
        return remainderInBuf;
    }

    public boolean markSupported() {
        return false;
    }

    public void mark(int readLimit) {
        return;
    }

    public void reset() throws IOException {
        throw (new IOException("markSupported = false"));
    }

    private void readObject() throws IOException {
        try {
            DataInputStream dI = new DataInputStream(in);
            int len = 0;
            if (remainderInChannel > 0)
                len = remainderInChannel;
            else {
                remainderInChannel = len = dI.readInt();
                if (len <= 0) {
                    setEOF();
                    return;
                }
            }

            buffer = new byte[len];
            remainderInBuf = dI.read(buffer, 0, len);
            if (remainderInBuf == -1) {
                setEOF();
                return;
            }
            remainderInChannel -= remainderInBuf;
            idx = 0;
        } catch (IOException ioe) {
            if (ioe instanceof java.io.EOFException ||
                ioe.getMessage().indexOf("EOF") != -1) {
                setEOF();
                return;
            } else
                throw ioe;
        }
    }

    private void setEOF() {
        EOF = true;
        buffer = null;
        remainderInBuf = 0;
        idx = 0;
    }

    public int read() throws IOException {
        if (EOF)
            return -1;
        byte ret = -1;
        if (remainderInBuf == 0)
            readObject();
        if (EOF)
            return -1;
        ret = buffer[idx++];
        remainderInBuf--;
        return ret;
    }

    public int read(byte[] b) throws IOException {
        if (b == null)
            throw (new NullPointerException("byte array is null"));
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (EOF)
            return -1;
        if (b == null)
            throw (new NullPointerException("byte array is null"));
        if (off < 0 || len < 0 || (off+len) > b.length)
            throw (new IndexOutOfBoundsException(
                                    "offset="+off+
                                    ", len="+len+
                                    ", (off+len)="+(off+len)+
                                    ", b.length="+b.length+
                                    ", (off+len)>b.length="+
                                        ((off+len)>b.length)));
        if (len == 0)
            return 0;

        if (remainderInBuf == 0)
            readObject();
        if (EOF)
            return -1;
        int len1 = (len > remainderInBuf) ? remainderInBuf : len;
        System.arraycopy(buffer, idx, b, off, len1);
        idx += len1;
        remainderInBuf -= len1;
        return len1;
    }

    public long skip(int n) throws IOException {
        if (EOF)
            return 0;
        if (n <= 0)
            return 0;
        if (remainderInBuf >= n) {
            idx += n;
            remainderInBuf -= n;
            return n;
        }
        int skipped = remainderInBuf;
        n -= skipped;
        remainderInBuf = 0;
        idx = 0;
        readObject();
        if (EOF)
            return skipped;
        idx += n;
        remainderInBuf -= n;
        skipped += n;
        return skipped;
    }
}

