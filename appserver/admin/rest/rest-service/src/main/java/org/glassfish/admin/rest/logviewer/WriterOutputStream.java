/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.logviewer;

import java.io.OutputStream;
import java.io.Writer;
import java.io.IOException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CoderResult;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.*;
import java.nio.ByteBuffer;

/**
 * {@link OutputStream} that writes to {@link Writer}
 * by assuming the platform default encoding.
 *
 * @author Kohsuke Kawaguchi
 */
public class WriterOutputStream extends OutputStream {
    private final Writer writer;
    private final CharsetDecoder decoder;

    private java.nio.ByteBuffer buf = ByteBuffer.allocate(1024);
    private CharBuffer out = CharBuffer.allocate(1024);

    public WriterOutputStream(Writer out, Charset charset) {
        this.writer = out;
        decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    public WriterOutputStream(Writer out) {
        this(out,DEFAULT_CHARSET);
    }

    public void write(int b) throws IOException {
        if(buf.remaining()==0)
            decode(false);
        buf.put((byte)b);
    }

    public void write(byte b[], int off, int len) throws IOException {
        while(len>0) {
            if(buf.remaining()==0)
                decode(false);
            int sz = Math.min(buf.remaining(),len);
            buf.put(b,off,sz);
            off += sz;
            len -= sz;
        }
    }

    public void flush() throws IOException {
        decode(false);
        flushOutput();
        writer.flush();
    }

    private void flushOutput() throws IOException {
        writer.write(out.array(),0,out.position());
        out.clear();
    }

    public void close() throws IOException {
        decode(true);
        flushOutput();
        writer.close();

        buf.rewind();
    }

    /**
     * Decodes the contents of {@link #buf} as much as possible to {@link #out}.
     * If necessary {@link #out} is further sent to {@link #writer}.
     *
     * <p>
     * When this method returns, the {@link #buf} is back to the 'accumulation'
     * mode.
     *
     * @param last
     *      if true, tell the decoder that all the input bytes are ready.
     */
    private void decode(boolean last) throws IOException {
        buf.flip();
        while(true) {
            CoderResult r = decoder.decode(buf, out, last);
            if(r==CoderResult.OVERFLOW) {
                flushOutput();
                continue;
            }
            if(r==CoderResult.UNDERFLOW) {
                buf.compact();
                return;
            }
            // otherwise treat it as an error
            r.throwException();
        }
    }

    private static final Charset DEFAULT_CHARSET = getDefaultCharset();

    private static Charset getDefaultCharset() {
        try {
            String encoding = System.getProperty("file.encoding");
            return Charset.forName(encoding);
        } catch (UnsupportedCharsetException e) {
            return Charset.forName("UTF-8");
        }
    }
}
