/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.InterruptedByTimeoutException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ReadListener;
import javax.servlet.http.WebConnection;

import org.apache.catalina.ContainerEvent;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.core.StandardServer;
import org.glassfish.grizzly.ReadHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.util.ByteChunk.ByteInputChannel;
import org.glassfish.grizzly.http.util.CharChunk;
import org.glassfish.logging.annotation.LogMessageInfo;

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

    private static final Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "Stream closed",
            level = "WARNING"
    )
    public static final String STREAM_CLOSED = "AS-WEB-CORE-00045";

    @LogMessageInfo(
            message = "Already set read listener",
            level = "WARNING"
    )
    public static final String ALREADY_SET_READ_LISTENER = "AS-WEB-CORE-00046";

    @LogMessageInfo(
            message = "Cannot set ReaderListener for non-async or non-upgrade request",
            level = "WARNING"
    )
    public static final String NON_ASYNC_UPGRADE_EXCEPTION = "AS-WEB-CORE-00047";

    @LogMessageInfo(
            message = "Error in invoking ReadListener.onDataAvailable",
            level = "WARNING"
     )
    public static final String READ_LISTENER_ON_DATA_AVAILABLE_ERROR = "AS-WEB-CORE-00048";

    // -------------------------------------------------------------- Constants


    public static final int DEFAULT_BUFFER_SIZE = 8*1024;


    // ----------------------------------------------------- Instance Variables


    /**
     * Associated Grizzly request.
     */
    private Request grizzlyRequest;

    private org.glassfish.grizzly.http.io.InputBuffer grizzlyInputBuffer;

    private org.apache.catalina.connector.Request request;

    private ReadHandler readHandler = null;
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


    public void setRequest(org.apache.catalina.connector.Request request) {
        this.request = request;
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
            log.log(Level.FINEST, "recycle()");

        grizzlyInputBuffer = null;
        grizzlyRequest = null;
        readHandler = null;
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
            throw new IOException(rb.getString(STREAM_CLOSED));

        return grizzlyInputBuffer.readByte();
    }


    public int read(final byte[] b, final int off, final int len)
        throws IOException {
        if (grizzlyInputBuffer.isClosed())
            throw new IOException(rb.getString(STREAM_CLOSED));

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
            if (readHandler != null) {
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
        if (readHandler != null) {
            throw new IllegalStateException(rb.getString(ALREADY_SET_READ_LISTENER));
        }

        if (!(request.isAsyncStarted() || request.isUpgrade())) {
            throw new IllegalStateException(rb.getString(NON_ASYNC_UPGRADE_EXCEPTION));
        }

        readHandler = new ReadHandlerImpl(readListener);

        if (isReady()) {
            try {
                readHandler.onDataAvailable();
            } catch(Throwable t) {
                log.log(Level.WARNING, READ_LISTENER_ON_DATA_AVAILABLE_ERROR, t);
            }
        }
    }

    void disableReadHandler() {
        if (readHandler != null) {
            synchronized(readHandler) {
                readHandler.onError(new InterruptedByTimeoutException());
            }
        }
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
            throw new IOException(rb.getString(STREAM_CLOSED));

        return grizzlyInputBuffer.readChar();
    }


    public int read(char[] cbuf)
        throws IOException {

        return read(cbuf, 0, cbuf.length);
    }


    public int read(char[] cbuf, int off, int len)
        throws IOException {

        if (grizzlyInputBuffer.isClosed())
            throw new IOException(rb.getString(STREAM_CLOSED));

        return grizzlyInputBuffer.read(cbuf, off, len);
    }


    public long skip(long n)
        throws IOException {

        if (grizzlyInputBuffer.isClosed())
            throw new IOException(rb.getString(STREAM_CLOSED));

        if (n < 0) {
            throw new IllegalArgumentException();
        }
        return grizzlyInputBuffer.skip(n, true);

    }


    public boolean ready()
        throws IOException {

        if (grizzlyInputBuffer.isClosed())
            throw new IOException(rb.getString(STREAM_CLOSED));

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
            throw new IOException(rb.getString(STREAM_CLOSED));
        grizzlyInputBuffer.reset();
    }


    public void checkConverter() 
        throws IOException {

        grizzlyInputBuffer.processingChars();

    }


    class ReadHandlerImpl implements ReadHandler {
        private ReadListener readListener = null;
        private volatile boolean disable = false;

        private ReadHandlerImpl(ReadListener listener) {
            readListener = listener;
        }

        @Override
        public void onDataAvailable() {
            if (disable) {
                return;
            }
            if (!Boolean.TRUE.equals(IS_READY_SCOPE.get())) {
                processDataAvailable();
            } else {
                AsyncContextImpl.getExecutorService().execute(new Runnable() {
                    @Override
                    public void run() {
                        processDataAvailable();
                    }
                });
            }
        }

        private void processDataAvailable() {
            ClassLoader oldCL;
            if (Globals.IS_SECURITY_ENABLED) {
                PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
                oldCL = AccessController.doPrivileged(pa);
            } else {
                oldCL = Thread.currentThread().getContextClassLoader();
            }

            try {
                Context context = request.getContext();
                ClassLoader newCL = context.getLoader().getClassLoader();
                if (Globals.IS_SECURITY_ENABLED) {
                    PrivilegedAction<Void> pa = new PrivilegedSetTccl(newCL);
                    AccessController.doPrivileged(pa);
                } else {
                    Thread.currentThread().setContextClassLoader(newCL);
                }

                synchronized(this) {
                    prevIsReady = true;
                    try {
                        context.fireContainerEvent(
                            ContainerEvent.BEFORE_READ_LISTENER_ON_DATA_AVAILABLE, readListener);
                        readListener.onDataAvailable();
                    } catch(Throwable t) {
                        disable = true;
                        readListener.onError(t);
                    } finally {
                        context.fireContainerEvent(
                            ContainerEvent.AFTER_READ_LISTENER_ON_DATA_AVAILABLE, readListener);
                    }
                }
            } finally {
                if (Globals.IS_SECURITY_ENABLED) {
                    PrivilegedAction<Void> pa = new PrivilegedSetTccl(oldCL);
                    AccessController.doPrivileged(pa);
                } else {
                    Thread.currentThread().setContextClassLoader(oldCL);
                }
            }
        }

        @Override
        public void onAllDataRead() {
            if (disable) {
                return;
            }
            if (!Boolean.TRUE.equals(IS_READY_SCOPE.get())) {
                processAllDataRead();
            } else {
                AsyncContextImpl.getExecutorService().execute(new Runnable() {
                    @Override
                    public void run() {
                        processAllDataRead();
                    }
                });
            }
        }

        private void processAllDataRead() {
            ClassLoader oldCL;
            if (Globals.IS_SECURITY_ENABLED) {
                PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
                oldCL = AccessController.doPrivileged(pa);
            } else {
                oldCL = Thread.currentThread().getContextClassLoader();
            }

            try {
                Context context = request.getContext();
                ClassLoader newCL = context.getLoader().getClassLoader();
                if (Globals.IS_SECURITY_ENABLED) {
                    PrivilegedAction<Void> pa = new PrivilegedSetTccl(newCL);
                    AccessController.doPrivileged(pa);
                } else {
                    Thread.currentThread().setContextClassLoader(newCL);
                }

                synchronized(this) {
                    prevIsReady = true;
                    try {
                        context.fireContainerEvent(
                            ContainerEvent.BEFORE_READ_LISTENER_ON_ALL_DATA_READ, readListener);
                        readListener.onAllDataRead();
                    } catch(Throwable t) {
                        disable = true;
                        readListener.onError(t);
                    } finally {
                        context.fireContainerEvent(
                            ContainerEvent.AFTER_READ_LISTENER_ON_ALL_DATA_READ, readListener);
                    }
                }
            } finally {
                if (Globals.IS_SECURITY_ENABLED) {
                    PrivilegedAction<Void> pa = new PrivilegedSetTccl(oldCL);
                    AccessController.doPrivileged(pa);
                } else {
                    Thread.currentThread().setContextClassLoader(oldCL);
                }
            }
        }

        @Override
        public void onError(final Throwable t) {
            if (disable) {
                return;
            }
            disable = true;

            if (!Boolean.TRUE.equals(IS_READY_SCOPE.get())) {
                processError(t);
            } else {
                AsyncContextImpl.getExecutorService().execute(new Runnable() {
                    @Override
                    public void run() {
                        processError(t);
                    }
                });
            }
        }

        private void processError(final Throwable t) {
            ClassLoader oldCL;
            if (Globals.IS_SECURITY_ENABLED) {
                PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
                oldCL = AccessController.doPrivileged(pa);
            } else {
                oldCL = Thread.currentThread().getContextClassLoader();
            }

            try {
                Context context = request.getContext();
                ClassLoader newCL = context.getLoader().getClassLoader();
                if (Globals.IS_SECURITY_ENABLED) {
                    PrivilegedAction<Void> pa = new PrivilegedSetTccl(newCL);
                    AccessController.doPrivileged(pa);
                } else {
                    Thread.currentThread().setContextClassLoader(newCL);
                }

                synchronized(this) {
                    // Get isUpgrade and WebConnection before calling onError
                    // Just in case onError will complete the async processing.
                    final boolean isUpgrade = request.isUpgrade();
                    final WebConnection wc = request.getWebConnection();

                    try {
                        context.fireContainerEvent(
                            ContainerEvent.BEFORE_READ_LISTENER_ON_ERROR, readListener);
                        readListener.onError(t);
                    } finally {
                        if (isUpgrade && wc != null) {
                            try {
                                wc.close();
                            } catch (Exception ignored) {
                            }
                        }
                        context.fireContainerEvent(
                            ContainerEvent.AFTER_READ_LISTENER_ON_ERROR, readListener);

                    }
                }
            } finally {
                if (Globals.IS_SECURITY_ENABLED) {
                    PrivilegedAction<Void> pa = new PrivilegedSetTccl(oldCL);
                    AccessController.doPrivileged(pa);
                } else {
                    Thread.currentThread().setContextClassLoader(oldCL);
                }
            }
        }
    }

    private static class PrivilegedSetTccl implements PrivilegedAction<Void> {

        private ClassLoader cl;

        PrivilegedSetTccl(ClassLoader cl) {
            this.cl = cl;
        }

        @Override
        public Void run() {
            Thread.currentThread().setContextClassLoader(cl);
            return null;
        }
    }

    private static class PrivilegedGetTccl
            implements PrivilegedAction<ClassLoader> {

        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    }
}
