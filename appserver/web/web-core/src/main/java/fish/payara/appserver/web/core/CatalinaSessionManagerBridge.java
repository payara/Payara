/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.appserver.web.core;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

import org.apache.catalina.Manager;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Session;
import org.glassfish.grizzly.http.server.SessionManager;

class CatalinaSessionManagerBridge implements SessionManager {
    private static final String GRIZZLY_SESSION_NOTE = "fish.payara.web.GrizzlySession";

    CatalinaSessionManagerBridge() {

    }

    private Manager catalinaManager(Request request) {
        var catalinaRequest = request.getNote(GrizzlyCatalinaBridge.CATALINA_REQUEST);
        if (catalinaRequest.getContext() == null) {
            return null;
        }
        return catalinaRequest.getContext().getManager();
    }

    @Override
    public Session getSession(Request request, String id) {
        var manager = catalinaManager(request);
        if (manager == null) {
            return null;
        }
        try {
            var catalinaSession = manager.findSession(id);
            return wrap(catalinaSession);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Session wrap(org.apache.catalina.Session catalinaSession) {
        var facade = (Session) catalinaSession.getNote(GRIZZLY_SESSION_NOTE);
        if (facade == null) {
            facade = new CatalinaSessionBridge(catalinaSession);
            catalinaSession.setNote(GRIZZLY_SESSION_NOTE, facade);
        }
        return facade;
    }

    @Override
    public Session createSession(Request request) {
        var manager = catalinaManager(request);
        var session = manager.createEmptySession();
        return wrap(session);
    }

    @Override
    public String changeSessionId(Request request, Session session) {
        var manager = catalinaManager(request);
        if (manager == null) {
            return null;
        }
        if (session instanceof CatalinaSessionBridge) {
            return manager.rotateSessionId(((CatalinaSessionBridge) session).catalinaSession);
        }
        throw new IllegalArgumentException("Incorrect session type");
    }

    @Override
    public void configureSessionCookie(Request request, Cookie cookie) {

    }

    @Override
    public void setSessionCookieName(String s) {

    }

    @Override
    public String getSessionCookieName() {
        return "JSESSIONID"; // TODO configurable?
    }


    static class CatalinaSessionBridge extends Session {
        private final org.apache.catalina.Session catalinaSession;

        private CatalinaSessionBridge(org.apache.catalina.Session catalinaSession) {
            this.catalinaSession = catalinaSession;
        }

        @Override
        public boolean isValid() {
            return catalinaSession.isValid();
        }

        @Override
        public void setValid(boolean isValid) {
            catalinaSession.setValid(isValid);
        }

        @Override
        public boolean isNew() {
            return catalinaSession.getSession().isNew();
        }

        @Override
        public String getIdInternal() {
            return catalinaSession.getIdInternal();
        }

        @Override
        protected void setIdInternal(String id) {
            // we cannot actually change it
            super.setIdInternal(id);
        }

        @Override
        public void setAttribute(String key, Object value) {
            catalinaSession.getSession().setAttribute(key, value);
        }

        @Override
        public Object getAttribute(String key) {
            return catalinaSession.getSession().getAttribute(key);
        }

        @Override
        public Object removeAttribute(String key) {
            catalinaSession.getSession().removeAttribute(key);
            return null; // we don't need the overhead of fetching the removal
        }

        @Override
        public ConcurrentMap<String, Object> attributes() {
            throw new UnsupportedOperationException("Grizzly map view onto catalina session is not supported");
        }

        @Override
        public long getCreationTime() {
            return catalinaSession.getCreationTime();
        }

        @Override
        public long getSessionTimeout() {
            return catalinaSession.getMaxInactiveInterval() * 1000L;
        }

        @Override
        public void setSessionTimeout(long sessionTimeout) {
            catalinaSession.setMaxInactiveInterval((int) (sessionTimeout / 1000));
        }

        @Override
        public long getTimestamp() {
            return catalinaSession.getLastAccessedTime();
        }

        @Override
        public void setTimestamp(long timestamp) {

        }

        @Override
        public long access() {
            catalinaSession.access();
            return catalinaSession.getLastAccessedTime();
        }
    }
}
