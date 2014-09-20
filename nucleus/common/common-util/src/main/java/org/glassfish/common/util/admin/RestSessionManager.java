/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.common.util.admin;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.security.auth.Subject;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

/**
 * Manages Rest Sessions.
 * @author Mitesh Meswani
 */
@Service
@Singleton
public class RestSessionManager {
    private final SecureRandom randomGenerator = new SecureRandom();
    private Map<String, SessionData> activeSessions = new ConcurrentHashMap<String, SessionData>(); //To guard against parallel mutation corrupting the map

    // package/private to help minimize the chances of someone instantiating this directly
    RestSessionManager() {
    }

    //TODO createSession is public. this package should not be exported
    public String createSession(String remoteAddr, final Subject subject, final int sessionTimeoutInMins) {
        String sessionId;
        do {
            sessionId = new BigInteger(130, randomGenerator).toString(16);
        } while(isSessionExist(sessionId));

        saveSession(sessionId, remoteAddr, subject, sessionTimeoutInMins);
        return sessionId;
    }

    public Subject authenticate(final String sessionId, final String remoteAddress) {
        Subject result = null;
        boolean authenticated = false;
        purgeInactiveSessions();

        if(sessionId != null) {
            SessionData sessionData = activeSessions.get(sessionId);
            if(sessionData != null) {
                authenticated = sessionData.authenticate(remoteAddress);
                if(authenticated) {
                    // update last access time
                    sessionData.updateLastAccessTime();
                    result = sessionData.subject();
                } else {
                    activeSessions.remove(sessionId);
                }
            }
        }
        return result;
    }

    /**
     * Deletes Session corresponding to given <code> sessionId </code>
     * @param sessionId
     * @return
     */
    public boolean deleteSession(String sessionId) {
        boolean sessionDeleted = false;
        if(sessionId != null) {
            SessionData removedSession = activeSessions.remove(sessionId);
            sessionDeleted = (removedSession != null);
        }
        return sessionDeleted;
    }

    private void saveSession(String sessionId, String remoteAddr, final Subject subject, final int sessionTimeoutInMins) {
        purgeInactiveSessions();
        activeSessions.put(sessionId, new SessionData(sessionId, remoteAddr, subject, sessionTimeoutInMins) );
    }

    private void purgeInactiveSessions() {
        Set<Map.Entry<String, SessionData>> activeSessionsSet = activeSessions.entrySet();
        for (Map.Entry<String, SessionData> entry : activeSessionsSet) {
            if (!entry.getValue().isSessionActive()) {
                activeSessionsSet.remove(entry);
            }
        }
    }

    private boolean isSessionExist(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }

    private static class SessionData{
        /** IP address of client as obtained from Grizzly request */
        private String clientAddress;
        private long lastAccessedTime = System.currentTimeMillis();
        private final long inactiveSessionLifeTime;
        private final static String DISABLE_REMOTE_ADDRESS_VALIDATION_PROPERTY_NAME = "org.glassfish.admin.rest.disable.remote.address.validation";
        private final boolean disableRemoteAddressValidation = Boolean.getBoolean(DISABLE_REMOTE_ADDRESS_VALIDATION_PROPERTY_NAME);
        private final Subject subject;

        public SessionData(String sessionId, String remoteAddr, final Subject subject, final int sessionTimeoutInMins) {
            this.clientAddress = remoteAddr;
            this.subject = subject;
            inactiveSessionLifeTime = sessionTimeoutInMins * 60000L; // minutes * 60 seconds * 1000 millis
        }

        /**
         * @return true if the session has not timed out. false otherwise
         */
        public boolean isSessionActive() {
            return lastAccessedTime + inactiveSessionLifeTime > System.currentTimeMillis();
        }

        /**
         * Update the last accessed time to current time
         */
        public void updateLastAccessTime() {
            lastAccessedTime = System.currentTimeMillis();
        }

        /**
         * @return true if session is still active and the request is from same remote address as the one session was
         * initiated from
         */
        public boolean authenticate(final String remoteAddress) {
            return isSessionActive() && (clientAddress.equals(remoteAddress) || disableRemoteAddressValidation );
        }

        /**
         * Returns the Subject associated with the session.
         * @return the Subject
         */
        public Subject subject() {
            return subject;
        }
    }
}
