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

package javax.security.auth.message;

import java.util.Map;

/**
 * A message processing runtime uses this interface to pass messages and
 * message processing state to authentication contexts for processing by 
 * authentication modules.
 * <p>
 * This interface encapsulates a request message object and 
 * a response message object for a message exchange.
 * This interface may also be used to associate additional context
 * in the form of key/value pairs, with the encapsulated messages.
 * <p>
 * Every implementation of this interface should provide a zero argument
 * constructor, and a constructor which takes a single Map argument.
 * Additional constructors may also be provided.
 * <p>
 *
 * @version %I%, %G%
 * @see Map
 */

public interface MessageInfo { 

    /**
     * Get the request message object from this MessageInfo.
     *
     * @return An object representing the request message,
     * or null if no request message is set within the MessageInfo.
     */

    public Object getRequestMessage();

    /**
     * Get the response message object from this MessageInfo.
     *
     * @return an object representing the response message,
     * or null if no response message is set within the MessageInfo.
     */

    public Object getResponseMessage();

    /**
     * Set the request message object in this MessageInfo.
     * @param request An object representing the request message.
     */

    public void setRequestMessage(Object request);

    /**
     * Set the response message object in this MessageInfo.
     * @param response An object representing the response message.
     */
    public void setResponseMessage(Object response);

    /**
     * Get (a reference to) the Map object of this MessageInfo. 
     * Operations performed on the acquired Map must effect the 
     * Map within the MessageInfo.
     * 
     * @return the Map object of this MessageInfo.
     * This method never returns null. If a Map has not 
     * been associated with the MessageInfo, this method 
     * instantiates a Map, associates it with this MessageInfo,
     * and then returns it.
     */
    public Map getMap();

}



