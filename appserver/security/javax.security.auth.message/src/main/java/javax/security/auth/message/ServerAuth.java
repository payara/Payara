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
 * An implementation of this interface is used to validate received service 
 * request messages, and to secure service response messages.
 *
 * @version %I%, %G%
 * @see MessageInfo
 * @see Subject
 */
public interface ServerAuth {

    /**
     * Authenticate a received service request.
     *
     * This method is called to transform the mechanism-specific request 
     * message acquired by calling getRequestMessage (on messageInfo) 
     * into the validated application message to be returned to the message 
     * processing runtime. 
     * If the received message is a (mechanism-specific) meta-message, 
     * the method implementation must attempt to transform the meta-message 
     * into a corresponding mechanism-specific response message, or to the
     * validated application request message.
     * The runtime will bind a validated application message into the
     * the corresponding service invocation.
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
     *          request.  It is used by the method implementation to store
     *		Principals and credentials validated in the request.
     *
     * @param serviceSubject A Subject that represents the recipient of the
     *		service request, or null.  It may be used by the method 
     *          implementation as the source of Principals or credentials to
     *          be used to validate the request. If the Subject is not null, 
     *          the method implementation may add additional Principals or 
     *          credentials (pertaining to the recipient of the service 
     *          request) to the Subject.
     *
     * @return An AuthStatus object representing the completion status of
     *          the processing performed by the method.
     *          The AuthStatus values that may be returned by this method 
     *          are defined as follows:
     *
     * <ul>
     * <li> AuthStatus.SUCCESS when the application request message
     * was successfully validated. The validated request message is
     * available by calling getRequestMessage on messageInfo.
     *
     * <li> AuthStatus.SEND_SUCCESS to indicate that validation/processing
     * of the request message successfully produced the secured application 
     * response message (in messageInfo). The secured response message is 
     * available by calling getResponseMessage on messageInfo.
     *
     * <li> AuthStatus.SEND_CONTINUE to indicate that message validation is
     * incomplete, and that a preliminary response was returned as the
     * response message in messageInfo.
     *
     * When this status value is returned to challenge an 
     * application request message, the challenged request must be saved 
     * by the authentication module such that it can be recovered
     * when the module's validateRequest message is called to process
     * the request returned for the challenge.
     *
     * <li> AuthStatus.SEND_FAILURE to indicate that message validation failed
     * and that an appropriate failure response message is available by
     * calling getResponseMessage on messageInfo.
     * </ul>
     *
     * @exception AuthException When the message processing failed without
     *          establishing a failure response message (in messageInfo).
     */
    AuthStatus validateRequest(MessageInfo messageInfo,
			       Subject clientSubject,
			       Subject serviceSubject) throws AuthException;

    /**
     * Secure a service response before sending it to the client.
     *
     * This method is called to transform the response message acquired by
     * calling getResponseMessage (on messageInfo) into the mechanism-specific
     * form to be sent by the runtime.
     * <p> This method conveys the outcome of its message processing either
     * by returning an AuthStatus value or by throwing an AuthException.
     *
     * @param messageInfo A contextual object that encapsulates the
     *          client request and server response objects, and that may be 
     *          used to save state across a sequence of calls made to the 
     *          methods of this interface for the purpose of completing a 
     *          secure message exchange.
     *
     * @param serviceSubject A Subject that represents the source of the 
     *          service
     *          response, or null. It may be used by the method implementation
     *          to retrieve Principals and credentials necessary to secure 
     *          the response. If the Subject is not null, 
     *          the method implementation may add additional Principals or 
     *          credentials (pertaining to the source of the service 
     *          response) to the Subject.
     *
     * @return An AuthStatus object representing the completion status of
     *          the processing performed by the method. 
     *          The AuthStatus values that may be returned by this method 
     *          are defined as follows:
     *
     * <ul>
     * <li> AuthStatus.SEND_SUCCESS when the application response 
     * message was successfully secured. The secured response message may be
     * obtained by calling getResponseMessage on messageInfo.
     *
     * <li> AuthStatus.SEND_CONTINUE to indicate that the application response 
     * message (within messageInfo) was replaced with a security message 
     * that should elicit a security-specific response (in the form of a 
     * request) from the peer.
     *
     * This status value serves to inform the calling runtime that
     * (to successfully complete the message exchange) it will
     * need to be capable of continuing the message dialog by processing
     * at least one additional request/response exchange (after having
     * sent the response message returned in messageInfo).
     *
     * When this status value is returned, the application response must 
     * be saved by the authentication module such that it can be recovered
     * when the module's validateRequest message is called to process
     * the elicited response.
     *
     * <li> AuthStatus.SEND_FAILURE to indicate that a failure occurred while
     * securing the response message and that an appropriate failure response
     * message is available by calling getResponseMeessage on messageInfo.
     * </ul>
     *
     * @exception AuthException When the message processing failed without
     *          establishing a failure response message (in messageInfo).
     */
    AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject)
	throws AuthException;

    /**
     * Remove method specific principals and credentials from the subject.
     *
     * @param messageInfo a contextual object that encapsulates the
     *          client request and server response objects, and that may be 
     *          used to save state across a sequence of calls made to the 
     *          methods of this interface for the purpose of completing a 
     *          secure message exchange.
     *
     * @param subject the Subject instance from which the Principals and 
     *          credentials are to be removed.
     *
     * @exception AuthException If an error occurs during the Subject 
     *          processing.
     */

    void cleanSubject(MessageInfo messageInfo, Subject subject)
	throws AuthException;

}








