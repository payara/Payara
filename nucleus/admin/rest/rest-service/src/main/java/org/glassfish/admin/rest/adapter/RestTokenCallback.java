/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.adapter;

import javax.inject.Inject;
import javax.security.auth.Subject;
import org.glassfish.admin.rest.SessionManager;
import org.glassfish.common.util.admin.AdminAuthCallback;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * Authentication callback which processes ReST tokens.
 * <p>
 * Because it uses injection be sure to create instances of this using
 * hk2.
 * 
 * @author tjquinn
 */
@Service
@PerLookup
public class RestTokenCallback implements AdminAuthCallback.RequestBasedCallback {
    private static final String COOKIE_REST_TOKEN = "gfresttoken";
    private static final String HEADER_X_AUTH_TOKEN = "X-Auth-Token";
    
    private String restToken = null;;
    private String remoteAddr = null;
    
    @Inject
    private SessionManager sessionManager;
    
    @Override
    public void set(final String restToken) {
        this.restToken = restToken;
    }
    
    @Override
    public String get() {
        return restToken;
    }
    
    @Override
    public void setRequest(final Object data) {
        if (! (data instanceof Request)) {
            return;
        }
        final Request req = (Request) data;
        this.remoteAddr = req.getRemoteAddr();
        this.restToken = restToken(req);
    }
    
    @Override
    public Subject getSubject() {
        return sessionManager.authenticate(restToken, remoteAddr);
    }
    
    private String restToken(final Request request) {
        final Cookie[] cookies = request.getCookies();
        String result = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_REST_TOKEN.equals(cookie.getName())) {
                    result = cookie.getValue();
                }
            }
        }
        
        if (result == null) {
            result = request.getHeader(HEADER_X_AUTH_TOKEN);
        }
        return result;
    }
}
