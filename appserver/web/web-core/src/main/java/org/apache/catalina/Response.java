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

package org.apache.catalina;


import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;


/**
 * A <b>Response</b> is the Catalina-internal facade for a
 * <code>ServletResponse</code> that is to be produced,
 * based on the processing of a corresponding <code>Request</code>.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2005/12/08 01:27:19 $
 */

public interface Response {


    // ------------------------------------------------------------- Properties


    /**
     * Return the Connector through which this Response is returned.
     */
    public Connector getConnector();


    /**
     * Set the Connector through which this Response is returned.
     *
     * @param connector The new connector
     */
    public void setConnector(Connector connector);


    /**
     * Return the number of bytes actually written to the output stream.
     */
    public int getContentCount();


    /**
     * Return the Context with which this Response is associated.
     */
    public Context getContext();


    /**
     * Set the Context with which this Response is associated.  This should
     * be called as soon as the appropriate Context is identified.
     *
     * @param context The associated Context
     */
    public void setContext(Context context);


    /**
     * Set the application commit flag.
     * 
     * @param appCommitted The new application committed flag value
     */
    public void setAppCommitted(boolean appCommitted);


    /**
     * Application commit flag accessor.
     */
    public boolean isAppCommitted();


    /**
     * Return the "processing inside an include" flag.
     */
    public boolean getIncluded();


    /**
     * Set the "processing inside an include" flag.
     *
     * @param included <code>true</code> if we are currently inside a
     *  RequestDispatcher.include(), else <code>false</code>
     */
    public void setIncluded(boolean included);


    /**
     * Return descriptive information about this Response implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo();


    /**
     * Return the Request with which this Response is associated.
     */
    public Request getRequest();


    /**
     * Set the Request with which this Response is associated.
     *
     * @param request The new associated request
     */
    public void setRequest(Request request);


    /**
     * Return the <code>ServletResponse</code> for which this object
     * is the facade.
     */
    public ServletResponse getResponse();


    /**
     * Return the output stream associated with this Response.
     */
    public OutputStream getStream();


    /**
     * Set the output stream associated with this Response.
     *
     * @param stream The new output stream
     */
    public void setStream(OutputStream stream);


    /**
     * Set the suspended flag.
     * 
     * @param suspended The new suspended flag value
     */
    public void setSuspended(boolean suspended);


    /**
     * Suspended flag accessor.
     */
    public boolean isSuspended();


    /**
     * Set the error flag.
     */
    public void setError();


    /**
     * Error flag accessor.
     */
    public boolean isError();


    // BEGIN S1AS 4878272
    /**
     * Sets detail error message.
     *
     * @param message detail error message
     */
    public void setDetailMessage(String message);


    /**
     * Gets detail error message.
     *
     * @return the detail error message
     */
    public String getDetailMessage();
    // END S1AS 4878272


    // --------------------------------------------------------- Public Methods


    /**
     * Create and return a ServletOutputStream to write the content
     * associated with this Response.
     *
     * @exception IOException if an input/output error occurs
     */
    public ServletOutputStream createOutputStream() throws IOException;


    /**
     * Perform whatever actions are required to flush and close the output
     * stream or writer, in a single operation.
     *
     * @exception IOException if an input/output error occurs
     */
    public void finishResponse() throws IOException;


    /**
     * Return the content length that was set or calculated for this Response.
     */
    public int getContentLength();


    /**
     * Return the content type that was set or calculated for this response,
     * or <code>null</code> if no content type was set.
     */
    public String getContentType();


    /**
     * Return a PrintWriter that can be used to render error messages,
     * regardless of whether a stream or writer has already been acquired.
     *
     * @return Writer which can be used for error reports. If the response is
     * not an error report returned using sendError or triggered by an
     * unexpected exception thrown during the servlet processing
     * (and only in that case), null will be returned if the response stream
     * has already been used.
     *
     * @exception IOException if an input/output error occurs
     */
    public PrintWriter getReporter() throws IOException;


    /**
     * Release all object references, and initialize instance variables, in
     * preparation for reuse of this object.
     */
    public void recycle();


    /**
     * Reset the data buffer but not any status or header information.
     */
    public void resetBuffer();

    
    /**
     * Reset the data buffer and the using Writer/Stream flags but not any
     * status or header information.
     */
    public void resetBuffer(boolean resetWriterStreamFlags);
    
    
    /**
     * Send an acknowledgment of a request.
     * 
     * @exception IOException if an input/output error occurs
     */
    public void sendAcknowledgement()
        throws IOException;


    /**
     * Apply URL Encoding to the given URL without adding session identifier
     * et al associated to this response.
     *
     * @param url URL to be encoded
     */
    public String encode(String url);
}
