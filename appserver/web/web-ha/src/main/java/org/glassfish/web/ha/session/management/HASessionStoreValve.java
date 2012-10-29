/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

/*
 * HASessionStoreValve.java
 *
 * Created on June 27, 2002, 6:42 PM
 */

package org.glassfish.web.ha.session.management;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.catalina.valves.ValveBase;
import org.apache.catalina.Globals;
import org.apache.catalina.Manager;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.glassfish.ha.common.HACookieManager;

/**
 *
 * @author  lwhite
 * @author Rajiv Mordani
 */
public class HASessionStoreValve extends ValveBase {

    /**
     * The logger to use for logging ALL web container related messages.
     */
    private static final Logger _logger 
        = HAStoreBase._logger;
    
    /** Creates a new instance of HASessionStoreValve */
    public HASessionStoreValve() {
        super();           
    }

    /**
     * invoke call-back; nothing to do on the way in
     * @param request
     * @param response
     */    
    public int invoke(org.apache.catalina.Request request, org.apache.catalina.Response response) throws java.io.IOException, javax.servlet.ServletException {
        //FIXME this is for 7.0PE style valves
        //left here if the same optimization is done to the valve architecture
        String sessionId = null;
        ReplicationWebEventPersistentManager manager;
        StandardContext  context;


        HttpServletRequest httpServletrequest = (HttpServletRequest)request.getRequest();
        HttpSession session = httpServletrequest.getSession(false);
        if (session != null) {
            sessionId = session.getId();
      
            if (sessionId != null) {
                context = (StandardContext) request.getContext();
                manager = (ReplicationWebEventPersistentManager)context.getManager();


                String oldJreplicaValue = null;

                Cookie[] cookies = httpServletrequest.getCookies();
                if (cookies != null) {
                    for (Cookie cookie: cookies) {
                        if (cookie.getName().equalsIgnoreCase(Globals.JREPLICA_COOKIE_NAME)) {
                            oldJreplicaValue = cookie.getValue();
                        }
                    }
                    String replica = manager.getReplicaFromPredictor(sessionId, oldJreplicaValue);
                    if (replica != null) {
                        Session sess = request.getSessionInternal(false);
                        if (sess != null) {
                            sess.setNote(Globals.JREPLICA_SESSION_NOTE, replica);
                        }
                    }
                }
            }
        }


        return INVOKE_NEXT;
        // return 0;
    }
    
    /**
     * A post-request processing implementation that does the valveSave.
     * @param request
     * @param response
     */
    public void postInvoke(Request request, Response response)
        throws IOException, ServletException {
        //FIXME this is for 7.0PE style valves
        //left here if the same optimization is done to the valve architecture
        doPostInvoke(request, response);
    }
       
    
    /**
     * A post-request processing implementation that does the valveSave.
     * @param request
     * @param response
     */
    private void doPostInvoke(Request request, Response response)
        throws IOException, ServletException {
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("IN HASessionStoreValve>>postInvoke()");
        }
        String sessionId = null;
        Session session;
        StandardContext context;
        Manager manager;
        HttpServletRequest hreq = 
            (HttpServletRequest) request.getRequest();
        HttpSession hsess = hreq.getSession(false);
        if (hsess != null) {
            sessionId = hsess.getId();
            if(_logger.isLoggable(Level.FINEST)) {
                _logger.finest("IN HASessionStoreValve:postInvoke:sessionId=" +
                               sessionId);
            }
        }
        if (sessionId != null) {
            context = (StandardContext) request.getContext();
            manager = context.getManager();
            session = manager.findSession(sessionId);
            if(_logger.isLoggable(Level.FINEST)) {
                _logger.finest("IN HASessionStoreValve:postInvoke:session=" +
                               session);
            }
            if (session != null) {
                WebEventPersistentManager pMgr = 
                        (WebEventPersistentManager) manager;
                pMgr.doValveSave(session);
            }
        }
        HACookieManager.reset();
    }    
    
}
