/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * PESessionLocker.java
 *
 * Created on January 18, 2006, 4:46 PM
 */

package com.sun.enterprise.web;

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionLocker;
import org.apache.catalina.session.BaseSessionLocker;
import org.apache.catalina.session.StandardSession;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;


/**
 *
 * @author lwhite
 */
public class PESessionLocker extends BaseSessionLocker {
    
    /** Creates a new instance of PESessionLocker */
    public PESessionLocker() {
    }
    
    /** Creates a new instance of PESessionLocker */
    public PESessionLocker(Context ctx) {
        this();
        _context = ctx;
    }    
    
    /** 
     * lock the session associated with this request
     * this will be a foreground lock
     * checks for background lock to clear
     * and does a decay poll loop to wait until
     * it is clear; after 5 times it takes control for 
     * the foreground
     * @param request
     */     
    public boolean lockSession(ServletRequest request) throws ServletException {
        boolean result = false;
        Session sess = this.getSession(request);
        //now lock the session
        if(sess != null) {
            long pollTime = 200L;
            int maxNumberOfRetries = 7;
            int tryNumber = 0;
            boolean keepTrying = true;
            boolean lockResult = false;
            //try to lock up to maxNumberOfRetries times
            //poll and wait starting with 200 ms
            while(keepTrying) {
                lockResult = sess.lockForeground();
                if(lockResult) {
                    keepTrying = false;
                    result = true;
                    break;
                }
                tryNumber++;
                if(tryNumber < maxNumberOfRetries) {
                    pollTime = pollTime * 2L;
                    threadSleep(pollTime);
                } else {
                    //tried to wait and lock maxNumberOfRetries times; throw an exception
                    //throw new ServletException("unable to acquire session lock");
                    //instead of above; unlock the background so we can take over
                    if (sess instanceof StandardSession) {
                        ((StandardSession)sess).unlockBackground();
                    }
                }              
            }
        }
        return result;
    }
    
    private Session getSession(ServletRequest request) {
        javax.servlet.http.HttpServletRequest httpReq = 
            (javax.servlet.http.HttpServletRequest) request;
        javax.servlet.http.HttpSession httpSess = httpReq.getSession(false);
        if(httpSess == null) {
            return null;
        }
        String id = httpSess.getId();
        Manager mgr = _context.getManager();
        Session sess = null;
        try {
            sess = mgr.findSession(id);
        } catch (java.io.IOException ex) {}

        return sess;
    }     
    
    protected void threadSleep(long sleepTime) {

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            ;
        }

    } 
    
    /** 
     * unlock the session associated with this request
     * @param request
     */     
    public void unlockSession(ServletRequest request) {
        Session sess = this.getSession(request);
        //now unlock the session
        if(sess != null) {
            sess.unlockForeground();
        }        
    }    
    
}
