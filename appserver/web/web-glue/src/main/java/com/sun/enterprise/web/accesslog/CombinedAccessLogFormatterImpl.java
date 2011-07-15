/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web.accesslog;

import org.apache.catalina.Request;
import org.apache.catalina.Response;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.nio.CharBuffer;

/**
 * Access log formatter using the <i>combined</i> access log format from
 * Apache.
 */
public class CombinedAccessLogFormatterImpl extends CommonAccessLogFormatterImpl {

    /**
     * Appends an access log entry line, with info obtained from the given
     * request and response objects, to the given CharBuffer.
     *
     * @param request The request object from which to obtain access log info
     * @param response The response object from which to obtain access log info
     * @param charBuffer The CharBuffer to which to append access log info
     */
    public void appendLogEntry(Request request,
                               Response response,
                               CharBuffer charBuffer) {

        super.appendLogEntry(request, response, charBuffer);

        ServletRequest req = request.getRequest();
        HttpServletRequest hreq = (HttpServletRequest) req;

        appendReferer(charBuffer, hreq);
        charBuffer.put(SPACE);

        appendUserAgent(charBuffer, hreq);
    }


    /*
     * Appends the value of the 'referer' header of the given request to
     * the given char buffer.
     */
    private void appendReferer(CharBuffer cb, HttpServletRequest hreq) {
        cb.put("\"");
        String referer = hreq.getHeader("referer");
        if (referer == null) {
            referer = NULL_VALUE;
        }
        cb.put(referer);
        cb.put("\"");
    }


    /*
     * Appends the value of the 'user-agent' header of the given request to
     * the given char buffer.
     */
    private void appendUserAgent(CharBuffer cb, HttpServletRequest hreq) {
        cb.put("\"");
        String ua = hreq.getHeader("user-agent");
        if (ua == null) {
            ua = NULL_VALUE;
        }
        cb.put(ua);
        cb.put("\"");
    }

}
