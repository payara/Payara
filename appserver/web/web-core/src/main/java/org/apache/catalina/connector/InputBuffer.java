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

import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ReadListener;

import org.apache.catalina.util.StringManager;
import org.glassfish.grizzly.ReadHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.util.ByteChunk.ByteInputChannel;
import org.glassfish.grizzly.http.util.CharChunk;

/**
 * The buffer used by Tomcat request. This is a derivative of the Tomcat 3.3
 * OutputBuffer, adapted to handle input instead of output. This allows 
 * complete recycling of the facade objects (the ServletInputStream and the
 * BufferedReader).
 *
 * @author Remy Maucherat
 */
public class InputBuffer extends Reader
    implements ByteInputChannel, CharChunk.CharInputChannel,
               CharChunk.CharOutputChannel {

    private static final Logger log = Logger.getLogger(InputBuffer.class.getName());

    /**
     * The string manager for this package.
     */
    private static final StringManager sm =
        StringManager.getManager(Constants.Package);


    // -------------------------------------------------------------- Constants


    public static final int DEFAULT_BUFFER_SIZE = 8*1024;
    static final int debug = 0;


    // ----------------------------------------------------- Instance Variables


    /**
     * Associated Grizzly request.
     */
    private Request grizzlyRequest;

    private org.glassfish.grizzly.http.server.io.InputBuffer grizzlyInputBuffer;

    private ReadHandler readHandler = null;
    private boolean hasSetReadListener = false;
    private boolean prevIsReady = true;
    private static final ThreadLocal<Boolean> IS_READY_SCOPE = new ThreadLocal<Boolean>();

    // ----------------------------------------------------------- Constructors


    /**
     * Default constructor. Allocate the buffer with the default buffer size.
     */
    public InputBuffer() {

        this(DEFAULT_BUFFER_SIZE);

    }


    /**
     * Alternate constructor which allows specifying the initial buffer size.
     *
     * @param size Buffer size to use
     */
    public InputBuffer(int size) {

//        this.size = size;
//        bb = new ByteChunk(size);
//        bb.setLimit(size);
//        bb.setByteInputChannel(this);
    }

    // ------------------------------------------------------------- Properties


    /**
     * Associated Grizzly request.
     * 
     * @param grizzlyRequest Associated Grizzly request
     */
    public void setRequest(Request grizzlyRequest) {
	this.grizzlyRequest = grizzlyRequest;
        this.grizzlyInputBuffer = grizzlyRequest.getInputBuffer();
    }


    /**
     * Get associated Grizzly request.
     * 
     * @return the associated Grizzly request
     */
    public Request getRequest() {
        return this.grizzlyRequest;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Recycle the output buffer.
     */
    public void recycle() {

        if (log.isLoggable(Level.FINEST))
            log.finest("recycle()");

        grizzlyInputBuffer = null;
        grizzlyRequest = null;
        readHandler = null;
        hasSetReadListener = false;
        prevIsReady = true;

    }


    /**
     * Close the input buffer.
     * 
     * @throws IOException An underlying IOException occurred
     */
    public void close()
        throws IOException {
        grizzlyInputBuffer.close();
    }


    public int available()
        throws IOException {
        return grizzlyInputBuffer.readyData();
    }


    // ------------------------------------------------- Bytes Handling Methods


    /** 
     * Reads new bytes in the byte chunk.
     * 
     * @param cbuf Byte buffer to be written to the response
     * @param off Offset
     * @param len Length
     * 
     * @throws IOException An underlying IOException occurred
     */
    public int realReadBytes(byte cbuf[], int off, int len)
	throws IOException {
        return grizzlyInputBuffer.read(cbuf, off, len);
    }


    public int readByte()
        throws IOException {
        if (grizzlyInputBuffer.isClosed())
            throw new IOException(sm.getString("inputBuffer.streamClosed"));

        return grizzlyInputBuffer.readByte();
    }


    public int read(final byte[] b, final int off, final int len)
        throws IOException {
        if (grizzlyInputBuffer.isClosed())
            throw new IOException(sm.getString("inputBuffer.streamClosed"));

        return grizzlyInputBuffer.read(b, off, len);
    }


    public boolean isFinished() {
        return grizzlyInputBuffer.isFinished();
    }


    public boolean isReady() {
        if (!prevIsReady) {
            return false;
        }

        boolean result = (grizzlyInputBuffer.available() > 0);
        if (!result) {
            if (hasSetReadListener) {
                prevIsReady = false; // Not data available
                IS_READY_SCOPE.set(Boolean.TRUE);
                try {
                    grizzlyInputBuffer.notifyAvailable(readHandler);
                } finally {
                    IS_READY_SCOPE.remove();
                }
                
            } else {
                prevIsReady = true;  // Allow next .isReady() call to check underlying inputStream
            }
        }

        return result;
    }


    public void setReadListener(ReadListener readListener) {
        if (hasSetReadListener) {
            throw new IllegalStateException(
                sm.getString("inputBuffer.alreadySetReadListener"));
        }

        readHandler = new ReadHandlerImpl(readListener);
        hasSetReadListener = true;
    }

    // ------------------------------------------------- Chars Handling Methods


    /**
     * Since the converter will use append, it is possible to get chars to
     * be removed from the buffer for "writing". Since the chars have already
     * been read before, they are ignored. If a mark was set, then the
     * mark is lost.
     */
    public void realWriteChars(char c[], int off, int len) 
        throws IOException {
        // START OF SJSAS 6231069
//        initChar();
        // END OF SJSAS 6231069
//        markPos = -1;
    }


    public void setEncoding(final String encoding) {
        grizzlyInputBuffer.setDefaultEncoding(encoding);
    }


    public int realReadChars(final char cbuf[], final int off, final int len)
        throws IOException {

        return grizzlyInputBuffer.read(cbuf, off, len);

    }


    public int read()
        throws IOException {

        if (grizzlyInputBuffer.isClosed())
            throw new IOException(sm.getString("inputBuffer.streamClosed"));

        return grizzlyInputBuffer.readChar();
    }


    public int read(char[] cbuf)
        throws IOException {

        return read(cbuf, 0, cbuf.length);
    }


    public int read(char[] cbuf, int off, int len)
        throws IOException {

        if (grizzlyInputBuffer.isClosed())
            throw new IOException(sm.getString("inputBuffer.streamClosed"));

        return grizzlyInputBuffer.read(cbuf, off, len);
    }


    public long skip(long n)
        throws IOException {

        if (grizzlyInputBuffer.isClosed())
            throw new IOException(sm.getString("inputBuffer.streamClosed"));

        if (n < 0) {
            throw new IllegalArgumentException();
        }
        return grizzlyInputBuffer.skip(n, true);

    }


    public boolean ready()
        throws IOException {

        if (grizzlyInputBuffer.isClosed())
            throw new IOException(sm.getString("inputBuffer.streamClosed"));

        return grizzlyInputBuffer.ready();
    }


    public boolean markSupported() {
        return true;
    }


    public void mark(int readAheadLimit)
        throws IOException {
        grizzlyInputBuffer.mark(readAheadLimit);
    }


    public void reset()
        throws IOException {

        if (grizzlyInputBuffer.isClosed())
            throw new IOException(sm.getString("inputBuffer.streamClosed"));
        grizzlyInputBuffer.reset();
    }


    public void checkConverter() 
        throws IOException {

        grizzlyInputBuffer.processingChars();

    }


    class ReadHandlerImpl implements ReadHandler {
        private ReadListener readListener = null;
        private Object lk = new Object();

        private ReadHandlerImpl(ReadListener listener) {
            readListener = listener;
        }

        @Override
        public void onDataAvailable() {
            if (!Boolean.TRUE.equals(IS_READY_SCOPE.get())) {
                processDataAvailable();
            } else {
                AsyncContextImpl.pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        processDataAvailable();
                    }
                });
            }
        }

        private void processDataAvailable() {
            synchronized(lk) {
                prevIsReady = true;
                readListener.onDataAvailable();
            }
        }

        @Override
        public void onAllDataRead() {
            if (!Boolean.TRUE.equals(IS_READY_SCOPE.get())) {
                processAllDataRead();
            } else {
                AsyncContextImpl.pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        processAllDataRead();
                    }
                });
            }
        }

        private void processAllDataRead() {
            synchronized(lk) {
                prevIsReady = true;
                readListener.onAllDataRead();
            }
        }

        @Override
        public void onError(final Throwable t) {
            if (!Boolean.TRUE.equals(IS_READY_SCOPE.get())) {
                processError(t);
            } else {
                AsyncContextImpl.pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        processError(t);
                    }
                });
            }
        }

        private void processError(final Throwable t) {
            synchronized(lk) {
                readListener.onError(t);
            }
        }
    }
}
