/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.catalina.connector;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.Context;

/**
 * Implementation of WebConnection for Servlet 3.1
 *
 * @author Amy Roh
 * @author Shing Wai Chan
 * @version $Revision: 1.23 $ $Date: 2007/07/09 20:46:45 $
 */
public class WebConnectionImpl implements WebConnection {

    private ServletInputStream inputStream;

    private ServletOutputStream outputStream;

    private Request request;

    private Response response;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    // ----------------------------------------------------------- Constructor

    public WebConnectionImpl(ServletInputStream inputStream, ServletOutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    /**
     * Returns an input stream for this web connection.
     *
     * @return a ServletInputStream for reading binary data
     *
     * @exception java.io.IOException if an I/O error occurs
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return inputStream;
    }

    /**
     * Returns an output stream for this web connection.
     *
     * @return a ServletOutputStream for writing binary data
     *
     * @exception IOException if an I/O error occurs
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException{
        return outputStream;
    }

    @Override
    public void close() throws Exception {
        // Make sure we run close logic only once
        if (isClosed.compareAndSet(false, true)) {
            if ((request != null) && (request.isUpgrade())) {
                Context context = request.getContext();
                HttpUpgradeHandler httpUpgradeHandler =
                        request.getHttpUpgradeHandler();
                Exception exception = null;
                try {
                    try {
                        context.fireContainerEvent(
                            ContainerEvent.BEFORE_UPGRADE_HANDLER_DESTROYED,
                            httpUpgradeHandler);
                        httpUpgradeHandler.destroy();
                    } finally {
                        context.fireContainerEvent(
                            ContainerEvent.AFTER_UPGRADE_HANDLER_DESTROYED,
                            httpUpgradeHandler);
                    }
                    request.setUpgrade(false);
                    if (response != null) {
                        response.setUpgrade(false);
                    }
                } finally {
                    try {
                        inputStream.close();
                    } catch(Exception ex) {
                        exception = ex;
                    }
                    try {
                        outputStream.close();
                    } catch(Exception ex) {
                        exception = ex;
                    }
                    context.fireContainerEvent(
                        ContainerEvent.PRE_DESTROY, httpUpgradeHandler);
                    request.resumeAfterService();
                }
                if (exception != null) {
                    throw exception;
                }
            }
        }
    }

    public void setRequest(Request req) {
        request = req;
    }

    public void setResponse(Response res) {
        response = res;
    }

}
