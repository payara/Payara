/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.sse.impl;

import javax.enterprise.context.spi.CreationalContext;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.sse.api.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * ServerSentEventClientImpl class.
 *
 * @author Jitendra Kotamraju
 */
final class ServerSentEventConnectionImpl extends ServerSentEventConnection implements AsyncListener {
    final HttpServletRequest request;
    final ServerSentEventHandler sseh;
    final AsyncContext asyncContext;
    final CreationalContext<?> cc;
    private final ServerSentEventApplication owner;
    private boolean closed;

    ServerSentEventConnectionImpl(ServerSentEventApplication owner, HttpServletRequest request,
                ServerSentEventHandler sseh, CreationalContext<?> cc, AsyncContext asyncContext) {
        this.owner = owner;
        this.request = request;
        this.sseh = sseh;
        this.cc = cc;
        this.asyncContext = asyncContext;
    }

    void init() {
        // Call onConnected() callback on handler
        sseh.onConnected(this);
    }

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public void sendMessage(String eventData) throws IOException {
        // Can avoid creating ServerSentEventData for performance(if required)
        sendMessage(new ServerSentEventData().data(eventData));
    }

    @Override
    public void sendMessage(ServerSentEventData eventData) throws IOException {
        if (closed) {
            throw new IllegalStateException("sendMessage cannot be called after the connection is closed.");
        }
        synchronized (sseh) {       // so that events don't interleave
            try {
                // Write message on response and flush
                HttpServletResponse res = (HttpServletResponse) asyncContext.getResponse();
                ServletOutputStream sos = res.getOutputStream();
                sos.write(eventData.toString().getBytes("UTF-8"));
                sos.write('\n');
                sos.flush();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        destroy();
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
        // no-op
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        // no-op
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        destroy();
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
        // no-op
    }

    private void destroy() {
        cc.release();
        owner.destroyConnection(this);
        asyncContext.complete();        // calls onComplete()
    }
}
