/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.server.core.jws.servedcontent;

import com.sun.logging.LogDomains;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.glassfish.appclient.server.core.AppClientDeployerHelper;
import org.glassfish.appclient.server.core.jws.RestrictedContentAdapter;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

/**
 * Represents static content that is fixed in location and content over
 * time.
 *
 * @author tjquinn
 */
public class FixedContent extends Content.Adapter implements StaticContent {

    private final File file;
    
    private static final Logger logger = Logger.getLogger(AppClientDeployerHelper.ACC_MAIN_LOGGER,
            AppClientDeployerHelper.LOG_MESSAGE_RESOURCE);

    public FixedContent(final File file) {
        this.file = file;
    }
    
    public FixedContent() {
        this.file = null;
    }

    @Override
    public File file() throws IOException {
        return file;
    }
    
    @Override
    public void process(String relativeURIString, Request gReq, Response gResp) throws IOException {
       /*
        * The client's cache is obsolete.  Be sure to set the
        * time header values.
        */
       gResp.setDateHeader(RestrictedContentAdapter.LAST_MODIFIED_HEADER_NAME, file().lastModified());
       gResp.setDateHeader(RestrictedContentAdapter.DATE_HEADER_NAME, System.currentTimeMillis());
        /*
        * Delegate to the Grizzly implementation.
        */
       StaticHttpHandler.sendFile(gResp, file());
       final int status = gResp.getStatus();
        if (status != HttpServletResponse.SC_OK) {
            logger.log(Level.FINE, "Could not serve content for {0} - status = {1}", new Object[]{relativeURIString, status});
        } else {
            logger.log(Level.FINE, "Served fixed content for {0}:{1}", new Object[]{gReq.getMethod(), toString()});
        }
    }

    @Override
    public String toString() {
        return "FixedContent: " + file.getAbsolutePath();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FixedContent other = (FixedContent) obj;
        if (this.file != other.file && (this.file == null || !this.file.equals(other.file))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.file != null ? this.file.hashCode() : 0);
        return hash;
    }

    

}
