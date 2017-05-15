/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.catalina.session;

import org.apache.catalina.LogFacade;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import javax.servlet.http.*;

/**
 * Session manager for cookie-based persistence, where cookies carry session state.
 *
 * With cookie-based persistence, only session attribute values of type String are supported.
 */

public class CookiePersistentManager extends StandardManager {

    private static final String LAST_ACCESSED_TIME = "lastAccessedTime=";
    private static final String MAX_INACTIVE_INTERVAL = "maxInactiveInterval=";
    private static final String IS_VALID = "isValid=";

    private final Set<String> sessionIds = new HashSet<String>();

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    // The name of the cookies that carry session state
    private String cookieName;

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public void add(Session session) {
        synchronized (sessionIds) {
            if (!sessionIds.add(session.getIdInternal())) {
                throw new IllegalArgumentException("Session with id " + session.getIdInternal() +
                        " already present");
            }
            int size = sessionIds.size();
            if (size > maxActive) {
                maxActive = size;
            }
        }
    }

    @Override
    public Session findSession(String id, HttpServletRequest request) throws IOException {
        if (cookieName == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String value = null;
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return parseSession(cookie.getValue(), request.getRequestedSessionId());
            }
        }
        return null;
    }

    @Override
    public void clearSessions() {
        synchronized (sessionIds) {
            sessionIds.clear();
        }
    }

    @Override
    public Session[] findSessions() {
        return null;
    }

    @Override
    public void remove(Session session) {
        synchronized (sessionIds) {
            sessionIds.remove(session.getIdInternal());
        }
    }

    @Override
    public Cookie toCookie(Session session) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(IS_VALID + session.isValid() + ';');
        sb.append(LAST_ACCESSED_TIME + session.getLastAccessedTime() + ';');
        sb.append(MAX_INACTIVE_INTERVAL + session.getMaxInactiveInterval() + ';');
        synchronized (session.getAttributes()) {
            Set<Map.Entry<String, Object>> entries = session.getAttributes().entrySet();
            int numElements = entries.size();
            int i = 0;
            for (Map.Entry<String,Object> entry : entries) {
                sb.append(entry.getKey() + "=" + entry.getValue());
                if (i++ < numElements-1) {
                    sb.append(',');
                }
            }
        }

        remove(session);

        return new Cookie(cookieName, sb.toString());
    }

    @Override
    public void checkSessionAttribute(String name, Object value) {
        if (!(value instanceof String)) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SET_SESSION_ATTRIBUTE_EXCEPTION), name);
            throw new IllegalArgumentException(msg);
        }
    }

    /*
     * Parses the given string into a session, and returns it.
     * 
     * The given string is supposed to contain a session encoded using toCookie().
     */
    private Session parseSession(String value, String sessionId) throws IOException {
        String[] components = value.split(";");
        if (components.length != 4) {
            throw new IllegalArgumentException("Invalid session encoding");
        }

        StandardSession session = (StandardSession) createSession(sessionId);

        // 1st component: isValid
        int index = components[0].indexOf('=');
        if (index < 0) {
            throw new IllegalArgumentException("Missing separator for isValid");
        }
        session.setValid(Boolean.parseBoolean(components[0].substring(index+1, components[0].length())));

        // 2nd component: lastAccessedTime
        index = components[1].indexOf('=');
        if (index < 0) {
            throw new IllegalArgumentException("Missing separator for lastAccessedTime");
        }
        session.setLastAccessedTime(Long.parseLong(components[1].substring(index+1, components[1].length())));

        // 3rd component: maxInactiveInterval
        index = components[2].indexOf('=');
        if (index < 0) {
            throw new IllegalArgumentException("Missing separator for maxInactiveInterval");
        }
        session.setMaxInactiveInterval(Integer.parseInt(
                components[2].substring(index+1, components[2].length())));

        // 4th component: comma-separated sequence of name-value pairs
        String[] entries = components[3].split(",");
        for (String entry : entries) {
            index = entry.indexOf('=');
            if (index < 0) {
                throw new IllegalArgumentException("Missing session attribute key-value separator");
            }
            session.setAttribute(entry.substring(0, index), entry.substring(index+1, entry.length()));
        }

        return session;
    }
}
