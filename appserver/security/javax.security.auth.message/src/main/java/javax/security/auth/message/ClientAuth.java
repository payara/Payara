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
import javax.security.auth.Subject;

/**
 * An implementation of this interface is used to secure service request 
 * messages, and validate received service response messages.
 * 
 * @version %I%, %G%
 * @see MessageInfo
 * @see Subject
 */
public interface ClientAuth {

    /**
     * Secure a service request message before sending it to the service.
     * <p>
     * This method is called to transform the request message acquired 
     * by calling getRequestMessage (on messageInfo) into the 
     * mechanism-specific form to be sent by the runtime.
     * <p> This method conveys the outcome of its message processing either
     * by returning an AuthStatus value or by throwing an AuthException.
     *
     * @param messageInfo A contextual object that encapsulates the
     *          client request and server response objects, and that may be 
     *          used to save state across a sequence of calls made to the 
     *          methods of this interface for the purpose of completing a 
     *          secure message exchange.
     *
     * @param clientSubject A Subject that represents the source of the 
     *          service 
     *          request, or null. It may be used by the method implementation
     *          as the source of Principals or credentials to be used to 
     *          secure the request. If the Subject is not null, the method
     *          implementation may add additional Principals or credentials 
     *          (pertaining to the source of the service request) to the 
     *          Subject.
     *
     * @return An AuthStatus object representing the completion status of
     *          the processing performed by the method.
     *          The AuthStatus values that may be returned by this method 
     *          are defined as follows:
     * <ul>
     * <li> AuthStatus.SUCCESS when the application request message 
     * was successfully secured. The secured request message may be
     * obtained by calling getRequestMessage on messageInfo. 
     *
     * <li> AuthStatus.SEND_CONTINUE to indicate that the application
     * request message (within messageInfo) was replaced with a security 
     * message that should elicit a security-specific response from the peer
     * security system. This status value also indicates that the application 
     * message has not yet been secured.
     *
     * This status value serves to inform the calling runtime that
     * (to successfully complete the message exchange) it will
     * need to be capable of continuing the message dialog by conducting 
     * at least one additional request/response exchange after having
     * received the security-specific response elicited by sending the 
     * security message.
     *
     * When this status value is returned, the corresponding 
     * invocation of <code>validateResponse</code> must be able 
     * to obtain the original application request message.
     *
     * <li> AuthStatus.FAILURE to indicate that a failure occured 
     * while securing the request message, and that an appropriate
     * failure response message is available by calling getResponseMessage 
     * on messageInfo.
     * </ul>
     *
     * @exception AuthException When the message processing failed without
     *          establishing a failure response message (in messageInfo).
     */
    AuthStatus secureRequest(MessageInfo messageInfo, Subject clientSubject)
	throws AuthException;

    /**
     * Validate a received service response.
     * <p> 
     * This method is called to transform the mechanism-specific response
     * message acquired by calling getResponseMessage (on messageInfo) 
     * into the validated application message to be returned to the 
     * message processing runtime. If the response message is a 
     * (mechanism-specific) meta-message, the method implementation must 
     * attempt to transform the meta-message into the next 
     * mechanism-specific request message to be sent by the runtime.
     * <p> This method conveys the outcome of its message processing either
     * by returning an AuthStatus value or by throwing an AuthException.
     *
     * @param messageInfo A contextual object that encapsulates the
     *          client request and server response objects, and that may be 
     *          used to save state across a sequence of calls made to the 
     *          methods of this interface for the purpose of completing a 
     *          secure message exchange.
     *
     * @param clientSubject A Subject that represents the recipient of the 
     *          service response, or null. It may be used by the method 
     *          implementation as the source of Principals or credentials to
     *          be used to validate the response. If the Subject is not null, 
     *          the method implementation may add additional Principals or 
     *          credentials (pertaining to the recipient of the service 
     *          request) to the Subject.
     *
     * @param serviceSubject A Subject that represents the source of the 
     *          service response, or null.  If the Subject is not null, 
     *          the method implementation may add additional Principals or 
     *          credentials (pertaining to the source of the service 
     *          response) to the Subject.
     *
     * @return An AuthStatus object representing the completion status of
     *          the processing performed by the method.
     *          The AuthStatus values that may be returned by this method 
     *          are defined as follows:
     * <ul>
     * <li> AuthStatus.SUCCESS when the application response 
     * message was successfully validated. The validated
     * message is available by calling getResponseMessage on 
     * messageInfo.
     *
     * <li> AuthStatus.SEND_CONTINUE to indicate that response validation 
     * is incomplete, and that a continuation request was returned
     * as the request message within messageInfo.
     * 
     * This status value serves to inform the calling runtime that
     * (to successfully complete the message exchange) it will
     * need to be capable of continuing the message dialog by conducting 
     * at least one additional request/response exchange.
     *
     * <li> AuthStatus.FAILURE to indicate that validation of the response
     * failed, and that a failure response message has been established 
     * in messageInfo. 
     * </ul>
     *
     * @exception AuthException When the message processing failed without
     *          establishing a failure response message (in messageInfo).
     */
    AuthStatus validateResponse(MessageInfo messageInfo, 
				Subject clientSubject, Subject serviceSubject)
	throws AuthException;

    /**
     * Remove implementation specific principals and credentials from the 
     * subject.
     *
     * @param messageInfo A contextual object that encapsulates the
     *          client request and server response objects, and that may be 
     *          used to save state across a sequence of calls made to the 
     *          methods of this interface for the purpose of completing a 
     *          secure message exchange.
     *
     * @param subject The Subject instance from which the Principals and 
     *          credentials are to be removed.
     *
     * @exception AuthException If an error occurs during the Subject 
     *          processing.
     */

    void cleanSubject(MessageInfo messageInfo, Subject subject) 
	throws AuthException;
}





