/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.monitoring;

import org.glassfish.webservices.SOAPMessageContext;


/**
 * This interface permits implementors to register a global message listener
 * which will be notified for all the web services requests and responses
 * on installed and enabled Web Services. Each invocation will be notified
 * through founr callbacks (preProcessRequest, processRequest, processResponse,
 * postProcessResponse).
 *
 * @author Jerome Dochez
 */
public interface GlobalMessageListener {

    /**
     * Callback when a web service request entered the web service container
     * and before any system processing is done.
     * @param endpoint is the endpoint the web service request is targeted to
     * @return a message ID to trace the request in the subsequent callbacks
     * or null if this invocation should not be traced further.
     */
    public String preProcessRequest(Endpoint endpoint);

    /**
     * Callback when a 1.X web service request is about the be delivered to the
     * Web Service Implementation Bean.
     * @param mid message ID returned by preProcessRequest call
     * @param ctx the jaxrpc message trace, transport dependent
     */
    public void processRequest(String mid, com.sun.xml.rpc.spi.runtime.SOAPMessageContext ctx, TransportInfo info);

    /**
     * Callback when a 1.X web service response was returned by the Web Service
     * Implementation Bean
     * @param mid message ID returned by the preProcessRequest call
     * @param ctx jaxrpc message trace, transport dependent.
     */
    public void processResponse(String mid, com.sun.xml.rpc.spi.runtime.SOAPMessageContext ctx);
   
    /**
     * Callback when a 2.X web service request is about the be delivered to the
     * Web Service Implementation Bean.
     * @param mid message ID returned by preProcessRequest call
     * @param ctx the jaxrpc message trace, transport dependent
     */
    public void processRequest(String mid, SOAPMessageContext ctx, TransportInfo info);

    /**
     * Callback when a 2.X web service response was returned by the Web Service
     * Implementation Bean
     * @param mid message ID returned by the preProcessRequest call
     * @param ctx jaxrpc message trace, transport dependent.
     */
    public void processResponse(String mid, SOAPMessageContext ctx);

    /**
     * Callback when a web service response has finished being processed
     * by the container and was sent back to the client
     * @param mid returned by the preProcessRequest call
     * @param info the response transport dependent information
     */
    public void postProcessResponse(String mid, TransportInfo info);

}

