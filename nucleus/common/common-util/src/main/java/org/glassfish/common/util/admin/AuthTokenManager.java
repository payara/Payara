/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.CULoggerInfo;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Singleton;
import javax.security.auth.Subject;
import org.jvnet.hk2.annotations.Service;

/**
 * Coordinates generation and consumption of very-limited-use authentication tokens.
 * <p>
 * Some DAS commands submit admin commands to be run elsewhere - either in
 * another process on the same host or, via ssh, to another host.  Given that it
 * is already executing, the DAS command in progress has already been authenticated (if
 * required).  Therefore we want the soon-to-be submitted commands to also
 * be authenticated, but we do not want to send the username and/or password
 * information that was used to authenticate the currently-running DAS command
 * to the other process for it to use.
 * <p>
 * Instead, the currently-running DAS command can use this service to obtain
 * a one-time authentication token.  The DAS command then includes the token,
 * rather than username/password credentials, in the submitted command.
 * <p>
 * This service records which tokens have been given out but not yet used up.
 * When an admin request arrives with a token, the AdminAdapter consults this
 * service to see if the token is valid and, if so, the AdminAdapter
 * allows the request to run.
 * <p>
 * We allow each token to be used twice, once for retrieving the command
 * metadata and then the second time to execute the command. (Also see the note below.)
 * <p>
 * Tokens have a limited life as measured in time also.  If a token is created
 * but not fully consumed before it expires, then this manager considers the
 * token invalid and removes it from the collection of known valid tokens.
 *
 *                              NOTE
 *
 * Commands that trigger other commands on multiple hosts - such as
 * start-cluster - will need to reuse the authentication token more than twice.
 * For such purposes the code using the token can append a "+" to the token.
 * When such a token is used, this manager does NOT decrement the remaining
 * number of uses.  Rather, it only refreshes the token's expiration time.
 *
 * @author Tim Quinn
 */
@Service
@Singleton
public class AuthTokenManager {

    public static final String AUTH_TOKEN_OPTION_NAME = "_authtoken";

    private static final String SUPPRESSED_TOKEN_OUTPUT = "????";

    private final static int TOKEN_SIZE = 10;

    private final static long DEFAULT_TOKEN_LIFETIME = 60 * 1000;
    private final static long TOKEN_EXPIRATION_IN_MS = 360 * 1000;

    private final SecureRandom rng = new SecureRandom();

    private final Map<String,TokenInfo> liveTokens = new HashMap<String,TokenInfo>();

    private final static Logger logger = CULoggerInfo.getLogger();

    private final static char REUSE_TOKEN_MARKER = '+';

    private static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(AuthTokenManager.class);

    /* hex conversion stolen shamelessly from Bill's LocalPasswordImpl - maybe refactor to share later */
    private static final char[] hex = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static class TokenInfo {
        private final String token;
        private int usesRemaining = 2; // each token is used once to get metadata, once to execute
        private long expiration;
        private final long lifetime;
        private final Subject subject;
        
        private TokenInfo(final Subject subject, final String value, final long lifetime) {
            this.subject = subject;
            this.token = value;
            this.lifetime = lifetime;
            this.expiration = System.currentTimeMillis() + (lifetime);
        }

        private synchronized boolean isOKTouse(final long now) {
            return  ! isUsedUp(now);
        }
        
        private synchronized boolean use(final boolean isBeingReused, final long now) {
            if (isUsedUp(now)) {
                if (logger.isLoggable(Level.FINER)) {
                    final String msg = localStrings.getLocalString("AuthTokenInvalid",
                        "Use of auth token {2} attempted but token is invalid; usesRemaining = {0,number,integer}, expired = {1}",
                        Integer.valueOf(usesRemaining), Boolean.toString(expiration <= now),
                        token);
                    logger.log(Level.FINER, msg);
                }
                return false;
            }
            if ( ! isBeingReused) {
                usesRemaining--;
            }
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER,
                        "Use of auth token {0} OK; isBeingReused = {2}; remaining uses = {1,number,integer}",
                        new Object[] {token, Integer.valueOf(usesRemaining), Boolean.toString(isBeingReused)});
            }
            expiration += lifetime;
            return true;
        }

        private boolean isUsedUp(final long now) {
            return usesRemaining <= 0 || expiration <= now;
        }
    }

    /**
     * Creates a new limited use authentication token with the specified
     * lifetime (in ms).
     * @param subject the Subject to associate with this token when it is consumed
     * @param lifetime how long each use of the token extends its lifetime
     * @return auth token
     */
    public String createToken(final Subject subject, final long lifetime) {
        final byte[] newToken = new byte[TOKEN_SIZE];
        rng.nextBytes(newToken);
        final String token = toHex(newToken);
        liveTokens.put(token, new TokenInfo(subject, token, lifetime));
        logger.log(Level.FINER, "Auth token {0} created", token);
        return token;
    }
    
    /**
     * Creates a new limited use authentication token with the default
     * lifetime.
     * @return auth token
     */
    public String createToken() {
        return createToken(DEFAULT_TOKEN_LIFETIME);
    }
    
    /**
     * Creates a new limited use authentication token with the given Subject
     * and the default lifetime.
     * @param subject the Subject to associated with this token when it is consumed
     * @return 
     */
    public String createToken(final Subject subject) {
        return createToken(subject, DEFAULT_TOKEN_LIFETIME);
    }
    
    /**
     * Creates a new limited use authentication token with the specified
     * lifetime but no Subject.
     * @param lifetime how long each use of the token extends its lifetime
     * @return 
     */
    public String createToken(final long lifetime) {
        return createToken (new Subject(), lifetime);
    }

    /**
     * Locates the Subject for the specified token (if any) without consuming 
     * the token.  
     * <p>
     * Use this method only from authentication logic that needs to find the
     * token.  Later command processing will consume the token if it is present.
     * This avoids having to force the special admin LoginModule to run even if
     * username/password authentication works.
     * 
     * @param token the token to find
     * @return Subject for the token; null if the token does not exist;
     */
    public Subject findToken(final String token) {
        final TokenInfo ti = findTokenInfo(token, System.currentTimeMillis());
        return (ti != null ? ti.subject : null);
    }
    
    private TokenInfo findTokenInfo(final String token, final long now) {
        final int firstReuseMarker = token.indexOf(REUSE_TOKEN_MARKER);
        final String tokenAsRecorded = (isReusedToken(token) ? token.substring(0, firstReuseMarker) : token);
        
        final TokenInfo ti = liveTokens.get(tokenAsRecorded);
        if (ti == null) {
            logger.log(Level.WARNING, CULoggerInfo.useNonexistentToken,
                    logger.isLoggable(Level.FINER) ? tokenAsRecorded : SUPPRESSED_TOKEN_OUTPUT);
            return null;
        }
        return (ti.isOKTouse(now) ? ti : null);
    }
    
    /**
     * Records the use of an authentication token by an admin request.
     * <p>
     * Just to make it easier for callers, the token value can have any number
     * of trailing reuse markers.  This simplifies the code in RemoteAdminCommand
     * which actually sends two requests for each command: one to retrieve
     * metadata and one to execute the command.  It might be that the command
     * itself might be reusing the token, in which case it will already have
     * appened a reuse marker to it.  Then the code which sends the metadata
     * request can freely append the marker again without having to check if
     * it is already present.
     *
     * @param token the token consumed, with 0 or more cppies of the reuse marker appended
     * @return the Subject stored with the token when it was created; null if none was provided
     */
    public Subject consumeToken(final String token) {
        Subject result = null;
        final long now = System.currentTimeMillis();
        final TokenInfo ti = findTokenInfo(token, now);
        if (ti != null) {
            if (ti.use(isReusedToken(token), now)) {
                /*
                 * We found the token info for this token and it is still valid,
                 * so prepare to return the stored Subject.
                 */
                result = ti.subject;
            }
        }
        retireExpiredTokens(now);

        return result;
    }

    private boolean isReusedToken(final String token) {
        return token.indexOf(REUSE_TOKEN_MARKER) != -1;
    }
    
    public Subject subject(final String token) {
        final TokenInfo ti = liveTokens.get(token);
        return (ti != null) ? ti.subject : null;
    }
    
    public static String markTokenForReuse(final String token) {
        return token + REUSE_TOKEN_MARKER;
    }
    
    private synchronized void retireExpiredTokens(final long now) {
        for (Iterator<Map.Entry<String,TokenInfo>> it = liveTokens.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<String,TokenInfo> entry = it.next();
            if (entry.getValue().isUsedUp(now)) {
                logger.log(Level.FINER, "Auth token {0} being retired during scan", entry.getValue().token);
                it.remove();
            }
        }
    }

    /**
     * Convert the byte array to a hex string.
     */
    private static String toHex(byte[] b) {
        char[] bc = new char[b.length * 2];
        for (int i = 0, j = 0; i < b.length; i++) {
            byte bb = b[i];
            bc[j++] = hex[(bb >> 4) & 0xF];
            bc[j++] = hex[bb & 0xF];
        }
        return new String(bc);
    }
}
