/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.common.servlet;

import java.io.InputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.glassfish.admingui.common.util.RestUtil;

/**
 *
 * @author andriy.zhdanov
 */
public class LogViewerContentSource  implements DownloadServlet.ContentSource {

     /**
     *  <p> This method returns a unique string used to identify this
     *      {@link DownloadServlet#getContentSource(String)}.  This string must be
     *      specified in order to select the appropriate
     *      {@link DownloadServlet#getContentSource(String)} when using the
     *      {@link DownloadServlet}.</p>
     */
    public String getId() {
        return "LogViewer";                                 // NOI18N
    }

    /**
     *  <p> This method is responsible for generating the content and
     *      returning an InputStream to that content.  It is also
     *      responsible for setting any attribute values in the
     *      {@link DownloadServlet#CONTENT_SOURCES}, such as {@link DownloadServlet#EXTENSION} or
     *      {@link DownloadServlet#CONTENT_TYPE}.</p>
     */
    public InputStream getInputStream(DownloadServlet.Context ctx) {
        // Set the extension so it can be mapped to a MIME type
        ctx.setAttribute(DownloadServlet.EXTENSION, "asc");

        HttpServletRequest request = (HttpServletRequest) ctx.getServletRequest();
        //http://localhost:4848/management/domain/view-log/?start=180427&instanceName=server
        String restUrl = request.getParameter("restUrl");
        String start = request.getParameter("start");
        String instanceName = request.getParameter("instanceName");

        // Create the tmpFile
        InputStream tmpFile = null;
        try {
            String endpoint = restUrl + "/view-log/";
            Map<String, Object> attrsMap = new HashMap<String, Object>();
            attrsMap.put("start", start);
            attrsMap.put("instanceName", instanceName);
            Response cr = RestUtil.getRequestFromServlet(request, endpoint, attrsMap);
            Map<String, String> headers = new HashMap<String, String>();
            String xTextAppendNextHeader = cr.getHeaderString("X-Text-Append-Next");
            if (!xTextAppendNextHeader.isEmpty()) {
                StringTokenizer tokenizer = new StringTokenizer(xTextAppendNextHeader, ",");
                if (tokenizer.hasMoreElements()) {
                    headers.put("X-Text-Append-Next", tokenizer.nextToken());
                }
            }
            ctx.setAttribute(DownloadServlet.HEADERS, headers);
            tmpFile = cr.readEntity(InputStream.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        // Return an InputStream to the tmpFile
        return tmpFile;
    }

    /**
     *  <p> This method may be used to clean up any temporary resources.  It
     *      will be invoked after the <code>InputStream</code> has been
     *      completely read.</p>
     */
    public void cleanUp(DownloadServlet.Context ctx) {
        // Nothing to do
    }

    /**
     *  <p> This method is responsible for returning the last modified date of
     *      the content, or -1 if not applicable.  This information will be
     *      used for caching.  This implementation always returns -1.</p>
     *
     *  @return -1
     */
    public long getLastModified(DownloadServlet.Context context) {
        return -1;
    }
}
