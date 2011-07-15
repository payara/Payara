/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

/* CVS information
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/server/servlet/RemoteJmxConnectorServlet.java,v 1.4 2005/12/25 04:26:43 tcfujii Exp $
 * $Revision: 1.4 $
 * $Date: 2005/12/25 04:26:43 $
 */

package com.sun.enterprise.admin.jmx.remote.server.servlet;

import java.io.*;
import java.util.logging.Logger;

import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.management.remote.message.Message;
import javax.management.remote.message.MBeanServerRequestMessage;
import javax.management.remote.message.MBeanServerResponseMessage;

/* BEGIN -- S1WS_MOD */
import com.sun.enterprise.admin.jmx.remote.DefaultConfiguration;
import com.sun.enterprise.admin.jmx.remote.streams.*;
import com.sun.enterprise.admin.jmx.remote.server.MBeanServerRequestHandler;
/* END -- S1WS_MOD */

/** The crux of server side implementation of JSR 160 over HTTP. This is the
 * single servlet that provides the entire support. The configuration is 
 * provided by the standard deployment descriptor in web.xml. The most notable
 * support is that of the security-constraint. The algorithm that
 * {@link #doGet} of this Servlet employs is pretty straightforward:
 * <ul>
 *  <li> Read the object from the ObjectInputStream backed by its own InputStream </li>
 *  <li> Carefully deserialize the Object depending upon the static classpath and other classes known. In other words, no dynamic class loading is used.</li>
 *  <li> Take care of the versioning information </li>
 *  <li> Invoke a handler to return a result of MBeanServerConnection invocation </li>
 *  <li> Configure the HTTP Response Headers </li>
 *  <li> Write the result to the ObjectOutputStream backed by its own OutputStream </li>
 * </ul>
 *
 * @author  Kedar Mhaswade
 * @version 1.0
 */
public class RemoteJmxConnectorServlet extends HttpServlet {
    
    private MBeanServerRequestHandler requestHandler;
    
    private final String BINARY_DATA_TYPE = "application/octet-stream";
/* BEGIN -- S1WS_MOD */
    private static final Logger logger = Logger.getLogger(
        DefaultConfiguration.JMXCONNECTOR_LOGGER);/*, 
        DefaultConfiguration.LOGGER_RESOURCE_BUNDLE_NAME );*/

/* END -- S1WS_MOD */
    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
/* BEGIN -- S1WS_MOD */
//        requestHandler = new MBeanServerRequestHandler();
        requestHandler = new MBeanServerRequestHandler(config);
/* END -- S1WS_MOD */
    }
    
    /** Destroys the servlet.
     */
    public void destroy() {
/* BEGIN -- S1WS_MOD */
        requestHandler.getNotificationManager().close();
/* END -- S1WS_MOD */
    }
    
    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.trim().equals(DefaultConfiguration.NOTIF_MGR_PATHINFO)) {
                requestHandler.getNotificationManager().getNotifications(request, response);
                return;
            }
/* END -- S1WS_MOD */
            final Message requestMessage = readRequestMessage(request);
            final Message responseMessage = requestHandler.handle
                ((MBeanServerRequestMessage)requestMessage);
/* BEGIN -- S1WS_MOD */
            drainInputStream(); // Since, in.close only reads byte by byte.
            JMXInbandStream.setIncomingStream(null);
            MBeanServerResponseMessage tempMsg =
                (MBeanServerResponseMessage) responseMessage;
            StreamMBeanServerResponseMessage resmsg =
                new StreamMBeanServerResponseMessage(
                                tempMsg.getMessageId(),
                                tempMsg.getWrappedResult(),
                                tempMsg.isException());
            InputStream in = JMXInbandStream.getOutgoingStream();
            if (in != null) {
                resmsg.setStreamAvailable(true);
            }
            OutputStream out = response.getOutputStream();
            sendResponseMessage(response, resmsg, out);
            if (in != null)
                sendStream(in, out, getContentLength(resmsg));
/* END -- S1WS_MOD */
        }
        catch (ClassNotFoundException ce) {
            final String message = "Detailed Message: Error in calling in Servlet:processRequest, actual exception is attached";
            throw new ServletException (message, ce);
        }
    }

/* BEGIN -- S1WS_MOD */
    private void drainInputStream() {
        InputStream in = JMXInbandStream.getInputStream();
        if (in != null) {
            byte[] bytes = new byte[8192];
            try {
                while (in.read(bytes) != -1)
                    ;
            } catch (IOException ignore) {
                // XXX: Log it
            }
        }
    }

    private void sendStream(InputStream in, OutputStream out, int padLen)
            throws IOException {
        OutputStream o = new JMXChunkedOutputStream(out);
        byte[] bytes = new byte[8192];
        int len = 0;
        while ( (len = in.read(bytes)) != -1) {
            o.write(bytes, 0, len);
        }
        JMXInbandStream.setOutputStream(null, 0);
        o.flush();
        ((JMXChunkedOutputStream)o).writeEOF(padLen);
    }
/* END -- S1WS_MOD */
    
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    private Message readRequestMessage(HttpServletRequest request) throws IOException, ClassNotFoundException {
/* BEGIN -- S1WS_MOD */
        JMXInbandStream.setOutputStream(null, 0);
        InputStream in = request.getInputStream();
//        final ObjectInputStream ois = new ObjectInputStream(
//			new BufferedInputStream(request.getInputStream()));
        final ObjectInputStream ois = new ObjectInputStream(in);
//			new BufferedInputStream(in));
/* END -- S1WS_MOD */
        final MBeanServerRequestMessage m = 
			(MBeanServerRequestMessage) ois.readObject();
/* BEGIN -- S1WS_MOD */
        StreamMBeanServerRequestMessage streamm =
                (StreamMBeanServerRequestMessage) m;
        if (streamm.isStreamAvailable()) {
            JMXInbandStream.setIncomingStream(
                new JMXChunkedInputStream(in));
        }
/* END -- S1WS_MOD */
		logger.fine("Method id is: " + m.getMethodId());
		return ( m );
    }
    
/* BEGIN -- S1WS_MOD */
//    private void sendResponseMessage(HttpServletResponse response, Message message) throws IOException {
    private void sendResponseMessage(HttpServletResponse response, Message message, OutputStream out) throws IOException {
/* END -- S1WS_MOD */
        configureResponse(response, message);
/* BEGIN -- S1WS_MOD */
//        final ObjectOutputStream oos = new ObjectOutputStream(
//			new BufferedOutputStream(response.getOutputStream()));
        final ObjectOutputStream oos = new ObjectOutputStream(
			new BufferedOutputStream(out));
/* END -- S1WS_MOD */
        oos.writeObject(message);
		oos.flush();
		//oos.close();
    }
    
    private void configureResponse(HttpServletResponse response, Message message) {
        response.setContentType(BINARY_DATA_TYPE);
/* BEGIN -- S1WS_MOD */
        int reslen = getContentLength(message);
        if (JMXInbandStream.getOutgoingStream() != null) {
            int len = (int) JMXInbandStream.getOutgoingStreamLength();
            int chunks = (int) ((len/8192) + 2);
            reslen += len + (chunks * 4);
        }
        response.setContentLength(reslen /*getContentLength(message)*/);
/* END -- S1WS_MOD */
		response.setHeader("Connection", "Keep-Alive"); 
        response.setStatus(HttpServletResponse.SC_OK);
    }
    /** Returns the size of given seialized object in bytes.
        The size is calculated from the underlying ByteArrayOutputStream
        backing an ObjectStream, onto which the Object is written.
    */
    private int getContentLength(Serializable serObject) {
        int size = 0;
        ObjectOutputStream oos = null;

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(serObject);
            size = baos.size();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
/* BEGIN -- S1WS_MOD */
/*
            try {
                if (oos != null) {
                    oos.close();
                }
            }
            catch (Exception e){}
*/
/* END -- S1WS_MOD */
        }
        return size;
    }
}
