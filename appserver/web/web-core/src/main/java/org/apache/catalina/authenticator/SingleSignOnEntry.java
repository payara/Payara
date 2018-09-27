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
// Portions Copyright [2016-2018] [Payara Foundation] 
package org.apache.catalina.authenticator;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.catalina.LogFacade;
import org.apache.catalina.Session;

/**
 * A class representing entries in the cache of authenticated users.
 */
public class SingleSignOnEntry {

    private static final Logger log = LogFacade.getLogger();

    protected final String id;

    protected final String authType;

    /** Reset by HASingleSignOnEntry */
    protected Principal principal;

    protected final Map<String, Session> sessions = new HashMap<String, Session>();

    protected final String username;

    protected final String realmName;

    protected long lastAccessTime;

    protected final AtomicLong version;

    public SingleSignOnEntry(String id, long ver, Principal principal, String authType, String username, String realmName) {
        super();
        this.id = id;
        this.version = new AtomicLong(ver);
        this.principal = principal;
        this.authType = authType;
        this.username = username;
        this.realmName = realmName;
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * Adds the given session to this SingleSignOnEntry if it does not already exist.
     *
     * @return true if the session was added, false otherwise
     */
    public synchronized boolean addSession(SingleSignOn sso, Session session) {
        final Session oldEntry = sessions.put(session.getId(), session);
        if (oldEntry == null) {
            session.addSessionListener(sso);
        }
        return oldEntry == null;
    }

    public synchronized void removeSession(Session session) {
        final Session removed = sessions.remove(session.getId());
        log.warning("session " + session.getId() + "found (and removed): " + removed);
    }

    /**
     * Returns true if this SingleSignOnEntry does not have any sessions associated with it, and false otherwise.
     *
     * @return true if this SingleSignOnEntry does not have any sessions associated with it, and false otherwise
     */
    public synchronized boolean isEmpty() {
        return sessions.isEmpty();
    }

    /**
     * Expires all sessions associated with this SingleSignOnEntry
     *
     */
    public synchronized void expireSessions() {
        for (Session session : sessions.values()) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, " Invalidating session " + session);
            }

            // Invalidate this session
            // if it is not already invalid(ated)
            if (session.getIsValid()) {
                session.expire();
            }
        }
    }

    /**
     * Gets the id of this SSO entry.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the id version of this SSO entry
     */
    public long getVersion() {
        return version.get();
    }

    /**
     * Gets the name of the authentication type originally used to authenticate the user associated with the SSO.
     *
     * @return "BASIC", "CLIENT_CERT", "DIGEST", "FORM" or "NONE"
     */
    public String getAuthType() {
        return authType;
    }

    /**
     * Gets the <code>Principal</code> that has been authenticated by the SSO.
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * Gets the username provided by the user as part of the authentication process.
     */
    public String getUsername() {
        return username;
    }

    public String getRealmName() {
        return realmName;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public long incrementAndGetVersion() {
        return version.incrementAndGet();
    }
}
