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

import org.glassfish.sse.api.ServerSentEventHandler;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * ServerSentEventServlet class.
 *
 * @author Jitendra Kotamraju
 */
@WebServlet(asyncSupported=true)
public final class ServerSentEventServlet extends HttpServlet {

    @SuppressWarnings("UnusedDeclaration")
    @Inject
    private transient ServerSentEventCdiExtension extension;

    @SuppressWarnings("UnusedDeclaration")
    @Inject
    private transient BeanManager bm;

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

        // TODO CDI is not strictly required unless ServerSentEventHandlerContext
        // TODO needs to be injected
        if (extension == null) {
            throw new RuntimeException("Enable CDI by including empty WEB-INF/beans.xml");
        }

        Map<String, ServerSentEventApplication> applicationMap = extension.getApplicationMap();
        ServerSentEventApplication sseApp = applicationMap.get(req.getServletPath());
        Class<?> clazz = sseApp.getHandlerClass();
        ServerSentEventHandler sseh;
        CreationalContext cc;

        // Check if SSE handler can be instantiated via CDI
        Iterator<Bean<?>> it = bm.getBeans(clazz).iterator();
        if (it.hasNext()) {
            Bean bean = it.next();
            cc = bm.createCreationalContext(bean);
            sseh = (ServerSentEventHandler) bean.create(cc);
        } else {
            throw new RuntimeException("Cannot create ServerSentEventHandler using CDI");
        }

        ServerSentEventHandler.Status status = sseh.onConnecting(req);
        if (status == ServerSentEventHandler.Status.DONT_RECONNECT) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            cc.release();
            return;
        }

        if (status != ServerSentEventHandler.Status.OK) {
            throw new RuntimeException("Internal Error: need to handle status "+status);
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/event-stream");
        resp.flushBuffer();	// writes status code and headers
        AsyncContext ac = req.startAsync(req, resp);
        ac.setTimeout(0);	// no timeout. need config ?
        ServerSentEventConnectionImpl con = sseApp.createConnection(req, sseh, cc, ac);
        ac.addListener(con);
        con.init();
    }

}
