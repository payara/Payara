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
import java.io.Writer;
import java.nio.channels.InterruptedByTimeoutException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.ContainerEvent;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.util.RequestUtil;
import org.glassfish.grizzly.WriteHandler;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * The buffer used by Tomcat response. This is a derivative of the Tomcat 3.3
 * OutputBuffer, with the removal of some of the state handling (which in 
 * Coyote is mostly the Processor's responsibility).
 *
 * @author Costin Manolache
 * @author Remy Maucherat
 */
public class OutputBuffer extends Writer
    implements ByteChunk.ByteOutputChannel {

    private static final Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "The WriteListener has already been set.",
            level = "WARNING"
    )
    public static final String WRITE_LISTENER_BEEN_SET = "AS-WEB-CORE-00049";

    @LogMessageInfo(
            message = "Cannot set WriteListener for non-async or non-upgrade request",
            level = "WARNING"
    )
    public static final String NON_ASYNC_UPGRADE_EXCEPTION = "AS-WEB-CORE-00050";

    @LogMessageInfo(
            message = "Error in invoking WriteListener.onWritePossible",
            level = "WARNING"
    )
    public static final String WRITE_LISTENER_ON_WRITE_POSSIBLE_ERROR = "AS-WEB-CORE-00051";

    // -------------------------------------------------------------- Constants

    private static final String SET_COOKIE_HEADER = "Set-Cookie";
    public static final String DEFAULT_ENCODING = 
        org.glassfish.grizzly.http.util.Constants.DEFAULT_HTTP_CHARACTER_ENCODING;
    public static final int DEFAULT_BUFFER_SIZE = 8*1024;
    static final int debug = 0;

    // ----------------------------------------------------- Instance Variables


    /**
     * Number of bytes written.
     */
    private int bytesWritten = 0;


    /**
     * Number of chars written.
     */
    private int charsWritten = 0;


    /**
     * Associated Coyote response.
     */
    private Response response;
    
    private org.glassfish.grizzly.http.server.Response grizzlyResponse;
    private org.glassfish.grizzly.http.io.OutputBuffer grizzlyOutputBuffer;

    private WriteHandler writeHandler = null;
    private boolean prevIsReady = true;
    private static final ThreadLocal<Boolean> CAN_WRITE_SCOPE = new ThreadLocal<Boolean>();

    /**
     * Suspended flag. All output bytes will be swallowed if this is true.
     */
    private boolean suspended = false;

    private int size;
    
    private org.glassfish.grizzly.http.io.OutputBuffer.LifeCycleListener sessionCookieChecker =
            new SessionCookieChecker();
    // ----------------------------------------------------------- Constructors


    /**
     * Default constructor. Allocate the buffer with the default buffer size.
     */
    public OutputBuffer() {

        this(DEFAULT_BUFFER_SIZE);

    }


    /**
     * Alternate constructor which allows specifying the initial buffer size.
     * 
     * @param size Buffer size to use
     */
    public OutputBuffer(int size) {
        // START S1AS8 4861933
        /*
        bb = new ByteChunk(size);
        bb.setLimit(size);
        bb.setByteOutputChannel(this);
        cb = new CharChunk(size);
        cb.setCharOutputChannel(this);
        cb.setLimit(size);
        */
        this.size = size;
        // END S1AS8 4861933
    }


    // ------------------------------------------------------------- Properties


    public void setCoyoteResponse(Response coyoteResponse) {
        this.response = coyoteResponse;
        this.grizzlyResponse = coyoteResponse.getCoyoteResponse();
        this.grizzlyOutputBuffer = grizzlyResponse.getOutputBuffer();
        grizzlyOutputBuffer.setBufferSize(size);
        grizzlyOutputBuffer.registerLifeCycleListener(sessionCookieChecker);
        // @TODO set chunkingDisabled
    }

    /**
     * Is the response output suspended ?
     * 
     * @return suspended flag value
     */
    public boolean isSuspended() {
        return this.suspended;
    }


    /**
     * Set the suspended flag.
     * 
     * @param suspended New suspended flag value
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Recycle the output buffer.
     */
    public void recycle() {

	if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "recycle()");

        bytesWritten = 0;
        charsWritten = 0;

        suspended = false;
        grizzlyResponse = null;
        grizzlyOutputBuffer = null;
        writeHandler = null;
        prevIsReady = true;
        response = null;

    }


    /**
     * Close the output buffer. This tries to calculate the response size if 
     * the response has not been committed yet.
     * 
     * @throws IOException An underlying IOException occurred
     */
    public void close()
        throws IOException {

        if (suspended)
            return;

        grizzlyOutputBuffer.close();

    }


    /**
     * Flush bytes or chars contained in the buffer.
     * 
     * @throws IOException An underlying IOException occurred
     */
    public void flush()
        throws IOException {
        doFlush(true);
    }


    /**
     * Flush bytes or chars contained in the buffer.
     * 
     * @throws IOException An underlying IOException occurred
     */
    protected void doFlush(boolean realFlush)
        throws IOException {

        if (suspended)
            return;

        if (realFlush || !grizzlyResponse.isCommitted()) {
            grizzlyOutputBuffer.flush();
        }

    }


    // ------------------------------------------------- Bytes Handling Methods


    /** 
     * Sends the buffer data to the client output, checking the
     * state of Response and calling the right interceptors.
     * 
     * @param buf Byte buffer to be written to the response
     * @param off Offset
     * @param cnt Length
     * 
     * @throws IOException An underlying IOException occurred
     */
    public void realWriteBytes(byte buf[], int off, int cnt)
	throws IOException {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "realWrite(b, " + off + ", " + cnt + ") " + grizzlyResponse);

        if (grizzlyResponse == null)
            return;
        
        if (grizzlyOutputBuffer.isClosed())
            return;

        // If we really have something to write
        if (cnt > 0) {
            try {
                grizzlyOutputBuffer.write(buf, off, cnt);
            } catch (IOException e) {
                // An IOException on a write is almost always due to
                // the remote client aborting the request.  Wrap this
                // so that it can be handled better by the error dispatcher.
                throw new ClientAbortException(e);
            }
        }

    }


    public void write(byte b[], int off, int len) throws IOException {

        if (suspended)
            return;

        writeBytes(b, off, len);

    }


    private void writeBytes(byte b[], int off, int len) 
        throws IOException {

        if (grizzlyOutputBuffer.isClosed())
            return;
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "write(b,off,len)");

        grizzlyOutputBuffer.write(b, off, len);
        bytesWritten += len;

    }


    // XXX Char or byte ?
    public void writeByte(int b)
        throws IOException {

        if (suspended)
            return;

        grizzlyOutputBuffer.writeByte(b);
        bytesWritten++;
    }


    // ------------------------------------------------- Chars Handling Methods


    public void write(int c)
        throws IOException {

        if (suspended)
            return;

        grizzlyOutputBuffer.writeChar(c);
        charsWritten++;
        
    }


    public void write(char c[])
        throws IOException {

        if (suspended)
            return;

        write(c, 0, c.length);

    }


    public void write(char c[], int off, int len)
        throws IOException {

        if (suspended)
            return;

        grizzlyOutputBuffer.write(c, off, len);
        charsWritten += len;
    }


    /**
     * Append a string to the buffer
     */
    public void write(String s, int off, int len)
        throws IOException {

        if (suspended)
            return;

        charsWritten += len;
        if (s==null)
            s="null";
        grizzlyOutputBuffer.write(s, off, len);
    }


    public void write(String s)
        throws IOException {

        if (suspended)
            return;

        if (s == null)
            s = "null";
        grizzlyOutputBuffer.write(s);
    } 


    public void checkConverter()
        throws IOException {
        
        grizzlyOutputBuffer.prepareCharacterEncoder();

    }

    
    // --------------------  BufferedOutputStream compatibility


    /**
     * Real write - this buffer will be sent to the client
     */
    public void flushBytes()
        throws IOException {

        grizzlyOutputBuffer.flush();

    }


    public int getBytesWritten() {
        return bytesWritten;
    }


    public int getCharsWritten() {
        return charsWritten;
    }


    public int getContentWritten() {
        return bytesWritten + charsWritten;
    }


    /** 
     * True if this buffer hasn't been used ( since recycle() ) -
     * i.e. no chars or bytes have been added to the buffer.  
     */
    public boolean isNew() {
        return (bytesWritten == 0) && (charsWritten == 0);
    }


    public void setBufferSize(int size) {
        if (size > grizzlyOutputBuffer.getBufferSize()) {
            grizzlyOutputBuffer.setBufferSize(size);
        }
    }


    public void reset() {

        grizzlyOutputBuffer.reset();
        bytesWritten = 0;
        charsWritten = 0;

    }


    public int getBufferSize() {
        return grizzlyOutputBuffer.getBufferSize();
    }


    public boolean isReady() {
        if (!prevIsReady) {
            return false;
        }
        
        boolean result = grizzlyOutputBuffer.canWrite();
        if (!result) {
            if (writeHandler != null) {
                prevIsReady = false; // Not can write
                CAN_WRITE_SCOPE.set(Boolean.TRUE);
                try {
                    grizzlyOutputBuffer.notifyCanWrite(writeHandler);
                } finally {
                    CAN_WRITE_SCOPE.remove();
                }
                
            } else {
                prevIsReady = true;  // Allow next .isReady() call to check underlying outputStream
            }
        }
        
        return result;
    }

    public void setWriteListener(WriteListener writeListener) {
        if (writeHandler != null) {
            throw new IllegalStateException(rb.getString(WRITE_LISTENER_BEEN_SET));
        }

        Request req = (Request)response.getRequest();
        if (!(req.isAsyncStarted() || req.isUpgrade())) {
            throw new IllegalStateException(rb.getString(NON_ASYNC_UPGRADE_EXCEPTION));
        }

        writeHandler = new WriteHandlerImpl(writeListener);

        if (isReady()) {
            try {
                writeHandler.onWritePossible();
            } catch(Throwable t) {
                log.log(Level.WARNING, WRITE_LISTENER_ON_WRITE_POSSIBLE_ERROR, t);
            }
        }
    }

    void disableWriteHandler() {
        if (writeHandler != null) {
            synchronized(writeHandler) {
                writeHandler.onError(new InterruptedByTimeoutException());
            }
        }
    }


    private void addSessionCookies() throws IOException {
        Request req = (Request) response.getRequest();
        if (req.isRequestedSessionIdFromURL()) {
            return;
        }

        StandardContext ctx = (StandardContext) response.getContext();
        if (ctx == null || !ctx.getCookies()) {
            // cookies disabled
            return;
        }

        Session sess = req.getSessionInternal(false);
        if (sess != null) {
            addSessionVersionCookie(req, ctx);
            addSessionCookieWithJvmRoute(req, ctx, sess);
            addSessionCookieWithJReplica(req, ctx, sess);
            addPersistedSessionCookie(req, ctx, sess);
            addJrouteCookie(req, ctx, sess);
            addSsoVersionCookie(req, ctx);
        }
    }

    /**
     * Adds a session version cookie to the response if necessary.
     */
    private void addSessionVersionCookie(Request request,
                                         StandardContext context) {
        Map<String, String> sessionVersions =
            request.getSessionVersionsRequestAttribute();
        if (sessionVersions != null) {
            Cookie cookie = new Cookie(
                Globals.SESSION_VERSION_COOKIE_NAME,
                RequestUtil.createSessionVersionString(sessionVersions));
            request.configureSessionCookie(cookie);
            if (request.isRequestedSessionIdFromCookie()) {
                /*
                 * Have the JSESSIONIDVERSION cookie inherit the 
                 * security setting of the JSESSIONID cookie to avoid
                 * session loss when switching from HTTPS to HTTP,
                 * see IT 7414
                 */
                cookie.setSecure(
                    request.isRequestedSessionIdFromSecureCookie());
            }
            grizzlyResponse.addHeader(SET_COOKIE_HEADER,
                               response.getCookieString(cookie));
        }
    }

    /**
     * Adds JSESSIONID cookie whose value includes jvmRoute if necessary.
     */
    private void addSessionCookieWithJvmRoute(Request request, StandardContext ctx,
            Session sess) {

        if (ctx.getJvmRoute() == null || sess == null) {
            return;
        }

        // Create JSESSIONID cookie that includes jvmRoute
        Cookie cookie = new Cookie(ctx.getSessionCookieName(),
                sess.getIdInternal() + "." + ctx.getJvmRoute());
        request.configureSessionCookie(cookie);
        grizzlyResponse.addHeader(SET_COOKIE_HEADER,
                response.getCookieString(cookie));
    }

    /**
     * Adds JSESSIONID cookie whose value includes jvmRoute if necessary.
     */
    private void addSessionCookieWithJReplica(Request request, StandardContext ctx,
            Session sess) {

        String replicaLocation = null;

        if (sess != null) {
            replicaLocation = (String)sess.getNote(Globals.JREPLICA_SESSION_NOTE);
            sess.removeNote(Globals.JREPLICA_SESSION_NOTE);
        }

        if (replicaLocation != null) {
            Cookie cookie = new Cookie(
                Globals.JREPLICA_COOKIE_NAME, replicaLocation);
            request.configureSessionCookie(cookie);
            if (request.isRequestedSessionIdFromCookie()) {
                cookie.setSecure(
                    request.isRequestedSessionIdFromSecureCookie());
            }
            grizzlyResponse.addHeader(SET_COOKIE_HEADER,
                               response.getCookieString(cookie));
        }

    }

    /**
     * Adds JSESSIONSSOVERSION cookie
     */
    private void addSsoVersionCookie(Request request, StandardContext ctx) {

        Long ssoVersion = (Long)request.getNote(
                org.apache.catalina.authenticator.Constants.REQ_SSO_VERSION_NOTE);
        if (ssoVersion != null) {
            Cookie cookie = new Cookie(
                    org.apache.catalina.authenticator.Constants.SINGLE_SIGN_ON_VERSION_COOKIE,
                    ssoVersion.toString());
            cookie.setMaxAge(-1);
            cookie.setPath("/");
            StandardHost host = (StandardHost) ctx.getParent();
            HttpServletRequest hreq =
                    (HttpServletRequest)request.getRequest();
            if (host != null) {
                host.configureSingleSignOnCookieSecure(cookie, hreq);
                host.configureSingleSignOnCookieHttpOnly(cookie);
            } else {
                cookie.setSecure(hreq.isSecure());
            }

            grizzlyResponse.addHeader(SET_COOKIE_HEADER,
                    response.getCookieString(cookie));
        }
    }

    private void addPersistedSessionCookie(Request request, StandardContext ctx,
            Session sess) throws IOException {

        if (sess == null) {
            return;
        }
        Cookie cookie = ctx.getManager().toCookie(sess);
        if (cookie != null) {
            request.configureSessionCookie(cookie);
            grizzlyResponse.addHeader(SET_COOKIE_HEADER,
                    response.getCookieString(cookie));
        }
    }

    private void addJrouteCookie(Request request, StandardContext ctx,
            Session sess) {

        String jrouteId = request.getHeader(Constants.PROXY_JROUTE);

        if (jrouteId == null) {
            // Load-balancer plugin is not front-ending this instance
            return;
        }

        if (sess == null) {
            // No session exists
            return;
        }

        if (request.getJrouteId() == null
                || !request.getJrouteId().equals(jrouteId)) {
            // Initial request or failover
            Cookie cookie = new Cookie(Constants.JROUTE_COOKIE, jrouteId);
            request.configureSessionCookie(cookie);
            if (request.isRequestedSessionIdFromCookie()) {
                /*
                 * Have the JSESSIONIDVERSION cookie inherit the
                 * security setting of the JSESSIONID cookie to avoid
                 * session loss when switching from HTTPS to HTTP,
                 * see IT 7414
                 */
                cookie.setSecure(
                        request.isRequestedSessionIdFromSecureCookie());
            }
            grizzlyResponse.addHeader(SET_COOKIE_HEADER,
                    response.getCookieString(cookie));
        }

    }

    // START PWC 6512276
    /**
     * Are there any pending writes waiting to be flushed?
     */
    public boolean hasData() {
        
        return !suspended && (!grizzlyResponse.isCommitted() ||
                grizzlyOutputBuffer.getBufferedDataSize() > 0);
    }
    // END PWC 6512276
    
    private class SessionCookieChecker implements org.glassfish.grizzly.http.io.OutputBuffer.LifeCycleListener {

        @Override
        public void onCommit() throws IOException {
            grizzlyOutputBuffer.removeLifeCycleListener(this);
            addSessionCookies();
        }
    }
    
    class WriteHandlerImpl implements WriteHandler {
        private WriteListener writeListener = null;
        private volatile boolean disable = false;

        private WriteHandlerImpl(WriteListener listener) {
            writeListener = listener;
        }

        public void onWritePossible() {
            if (disable) {
                return;
            }
            if (!Boolean.TRUE.equals(CAN_WRITE_SCOPE.get())) {
                processWritePossible();
            } else {
                AsyncContextImpl.getExecutorService().execute(new Runnable() {
                    @Override
                    public void run() {
                        processWritePossible();
                    }
                });
            }
        }

        private void processWritePossible() {
            ClassLoader oldCL;
            if (Globals.IS_SECURITY_ENABLED) {
                PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
                oldCL = AccessController.doPrivileged(pa);
            } else {
                oldCL = Thread.currentThread().getContextClassLoader();
            }

            try {
                Context context = response.getContext();
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
                            ContainerEvent.BEFORE_WRITE_LISTENER_ON_WRITE_POSSIBLE, writeListener);
                        writeListener.onWritePossible();
                    } catch(Throwable t) {
                        disable = true;
                        writeListener.onError(t);
                    } finally {
                        context.fireContainerEvent(
                            ContainerEvent.AFTER_WRITE_LISTENER_ON_WRITE_POSSIBLE, writeListener);
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

        public void onError(final Throwable t) {
            if (disable) {
                return;
            }
            disable = true;

            if (!Boolean.TRUE.equals(CAN_WRITE_SCOPE.get())) {
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
                Context context = response.getContext();
                ClassLoader newCL = context.getLoader().getClassLoader();
                if (Globals.IS_SECURITY_ENABLED) {
                    PrivilegedAction<Void> pa = new PrivilegedSetTccl(newCL);
                    AccessController.doPrivileged(pa);
                } else {
                    Thread.currentThread().setContextClassLoader(newCL);
                }

                synchronized(this) {
                    try {
                        context.fireContainerEvent(
                            ContainerEvent.BEFORE_WRITE_LISTENER_ON_ERROR, writeListener);
                        writeListener.onError(t);
                    } finally {
                        context.fireContainerEvent(
                            ContainerEvent.AFTER_WRITE_LISTENER_ON_ERROR, writeListener);
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
