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

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class JMXChunkedOutputStream extends OutputStream {
    private OutputStream out = null;
    private byte[] buffer = null;
    private int bufCount = 0;

    public JMXChunkedOutputStream(OutputStream out) {
        this.out = out;
        buffer = new byte[8192];
    }

    public void close() throws IOException {
        if (bufCount > 0)
            flush();
        out.close();
    }

    public void flush() throws IOException {
        if (bufCount > 0)
            flushBuffer();
        else
            out.flush();
    }

    private void flushBuffer() throws IOException {
        writeObject(buffer, 0, bufCount);
        bufCount = 0;
    }

    public void writeEOF(int padLen) throws IOException {
        DataOutputStream dO = new DataOutputStream(out);
        dO.writeInt(0);
        // Kludge:: For some wierd reason, the StreamingOutputStream of
        //          HttpURLConnection is not counting the requestmessage object's
        //          length as the number of bytes written.
        //          Hence, we will send some padding bytes at the end to fool
        //          StreamingOutputStream.
        dO.write(new byte[padLen],0,padLen);
        dO.flush();
    }

    public void write(byte[] b) throws IOException {
        if (b == null)
            throw (new NullPointerException("byte array is null"));
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
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
            return;
        if (bufCount > 0 && (bufCount+len) >= 8192) {
            flushBuffer();
        }
        if (len >= 8192) {
            writeObject(b, off, len);
            return;
        }
        writeBuffer(b, off, len);
    }

    public void write(int by) throws IOException {
        byte b = (byte) by;
        if (bufCount > 0 && (bufCount+1) >= 8192) {
            flushBuffer();
        }
        buffer[bufCount] = b;
        bufCount++;
    }

    private void writeBuffer(byte[] b, int off, int len) {
        System.arraycopy(b, off, buffer, bufCount, len);
        bufCount += len;
    }

    private void writeObject(byte[] b, int off, int len) 
            throws IOException {
        DataOutputStream dO = new DataOutputStream(out);
        dO.writeInt(len);
        dO.write(b, off, len);
        dO.flush();
    }
}

