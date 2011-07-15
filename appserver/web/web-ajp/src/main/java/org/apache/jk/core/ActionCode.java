/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.jk.core;


/**
 * Enumerated class containing the adapter event codes.
 *
 * Actions represent callbacks from the servlet container to the coyote
 * connector.
 *
 * Actions are implemented by ProtocolHandler, using the ActionHook interface.
 *
 * @see ProtocolHandler
 * @see ActionHook
 *
 * @author Remy Maucherat
 */
public final class ActionCode {


    // -------------------------------------------------------------- Constants


    public static final ActionCode ACTION_ACK = new ActionCode(1);


    public static final ActionCode ACTION_CLOSE = new ActionCode(2);


    public static final ActionCode ACTION_COMMIT = new ActionCode(3);


    /**
     * A flush() operation originated by the client ( i.e. a flush() on
     * the servlet output stream or writer, called by a servlet ).
     *
     * Argument is the Response.
     */
    public static final ActionCode ACTION_CLIENT_FLUSH = new ActionCode(4);

    
    public static final ActionCode ACTION_CUSTOM = new ActionCode(5);


    public static final ActionCode ACTION_RESET = new ActionCode(6);


    public static final ActionCode ACTION_START = new ActionCode(7);


    public static final ActionCode ACTION_STOP = new ActionCode(8);


    public static final ActionCode ACTION_WEBAPP = new ActionCode(9);

    /** Hook called after request, but before recycling. Can be used
        for logging, to update counters, custom cleanup - the request
        is still visible
    */
    public static final ActionCode ACTION_POST_REQUEST = new ActionCode(10);

    /**
     * Callback for lazy evaluation - extract the remote host name.
     */
    public static final ActionCode ACTION_REQ_HOST_ATTRIBUTE = 
        new ActionCode(11);


    /**
     * Callback for lazy evaluation - extract the SSL-related attributes.
     */
    public static final ActionCode ACTION_REQ_HOST_ADDR_ATTRIBUTE = new ActionCode(12);

    /**
     * Callback for lazy evaluation - extract the SSL-related attributes.
     */
    public static final ActionCode ACTION_REQ_SSL_ATTRIBUTE = new ActionCode(13);


    /** Chain for request creation. Called each time a new request is created
        ( requests are recycled ).
     */
    public static final ActionCode ACTION_NEW_REQUEST = new ActionCode(14);


    /**
     * Callback for lazy evaluation - extract the SSL-certificate 
     * (including forcing a re-handshake if necessary)
     */
    public static final ActionCode ACTION_REQ_SSL_CERTIFICATE = new ActionCode(15);

    /**
     * Callback for lazy evaluation - socket remote port.
     **/
    public static final ActionCode ACTION_REQ_REMOTEPORT_ATTRIBUTE = new ActionCode(16);


    /**
     * Callback for lazy evaluation - socket local port.
     **/
    public static final ActionCode ACTION_REQ_LOCALPORT_ATTRIBUTE = new ActionCode(17);


    /**
     * Callback for lazy evaluation - local address.
     **/
    public static final ActionCode ACTION_REQ_LOCAL_ADDR_ATTRIBUTE = new ActionCode(18);


    /**
     * Callback for lazy evaluation - local address.
     **/
    public static final ActionCode ACTION_REQ_LOCAL_NAME_ATTRIBUTE = new ActionCode(19);

    /**
     * Callback for setting FORM auth body replay
     */
    public static final ActionCode ACTION_REQ_SET_BODY_REPLAY = new ActionCode(20);
 
    /**
     * Callback used to reset suspended connection.
     */
    public static final ActionCode CANCEL_SUSPENDED_RESPONSE =  new ActionCode(21);
    
    /**
     * Callback to reset the {@link Response} suspended timeout.
     */
    public static final ActionCode RESET_SUSPEND_TIMEOUT = new ActionCode(22);
    
    public static ActionCode ACTION_FINISH_RESPONSE  = new ActionCode(23);
    
    public static ActionCode ACTION_DISCARD_UPSTREAM_WRITE  = new ActionCode(24);    

    // ----------------------------------------------------------- Constructors
    int code;

    /**
     * Private constructor.
     */
    private ActionCode(int code) {
        this.code=code;
    }

    /** Action id, usable in switches and table indexes
     */
    public int getCode() {
        return code;
    }


}
