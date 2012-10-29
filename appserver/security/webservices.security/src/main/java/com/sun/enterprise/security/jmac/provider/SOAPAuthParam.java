/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.jmac.provider;

import com.sun.enterprise.security.jauth.AuthParam;
import java.util.Map;
import java.util.HashMap;
import javax.xml.soap.*;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;

/**
  * SOAP authentication parameter.
  *
  * <p> An instance of SOAPAuthParam may be created with a null response object
  * (for example during a call to
  * <code>ServerAuthContext.validateRequest</code>).
  * If so, a response object may be created as necessary (by modules),
  * and set into the SOAPAuthParam via the <code>setResponse</code> method.
  *
  * <p> An instance of SOAPAuthParam may also be created with a null
  * request object (for example during a call to
  * <code>ServerAuthContext.secureResponse</code>).
  *
  * @version 1.12, 06/08/04
  */
public class SOAPAuthParam implements AuthParam {
    
    private HashMap infoMap;

    private boolean requestInPacket;
    private boolean responseInPacket;

    private SOAPMessage request;
    private SOAPMessage response;
    
    private static Exception classLoadingException = checkForPacket();

    private static final String REQ_PACKET = "REQ_PACKET";
    private static final String RES_PACKET = "RES_PACKET";

    private static boolean REQUEST_PACKET = true;
    private static boolean RESPONSE_PACKET = false;

    private static Exception checkForPacket() {
	try { 
	    if (Class.forName("com.sun.xml.ws.api.message.Packet") != null &&
		Class.forName("com.sun.xml.ws.api.message.Messages") != null) {
                return null;
	    }
	} catch (Exception e) {
	    // silently disables packet support
            return e;
	}
        return null;
    }

     /**
      * Create a SOAPAuthParam.
      *
      * @param request the SOAP request object, which may be null.
      * @param response the SOAP response object, which may be null.
      */
     public SOAPAuthParam(SOAPMessage request, SOAPMessage response) {
	 this.infoMap = null;
         this.request = request;
         this.response = response;
     }
    
     /**
      * Create a SOAPAuthParam (using Packets)
      *
      * @param request the request Packet, which may be null.
      * @param response the response Packet, which may be null.
      * @param dummy int serves only to disambiguate constructors
      */
     public SOAPAuthParam(Object request, Object response, int dummy) {
	 if (classLoadingException != null) {
	     throw new RuntimeException(classLoadingException);
	 } 
	 if ((request == null || request instanceof Packet) &&
	     (response == null || response instanceof Packet)) {
	     this.infoMap = new HashMap();
	     this.infoMap.put(REQ_PACKET,request);
	     this.infoMap.put(RES_PACKET,response);
	     this.requestInPacket = (request == null ? false : true);
	     this.responseInPacket = (response == null ? false : true);
	 } else {
	     throw new RuntimeException("argument is not packet");
	 }

     }

     /**
      * Get the SOAP request object.
      *
      * @return the SOAP request object, which may be null.
      */
     public Map getMap() {
	 if (this.infoMap == null) {
	     this.infoMap = new HashMap();
	 }
         return this.infoMap;
     }

     /**
      * Get the SOAP request object.
      *
      * @return the SOAP request object, which may be null.
      */
     public SOAPMessage getRequest() {

	 if (this.request == null) {

	     Object p = getPacket(REQUEST_PACKET,true);

	     if (p != null && this.requestInPacket) {

		 // if packet is not null, get SOAP from packet
		 // requestInPacket set to false as side-effect
		 // since packet has been consumed.

		 this.request = getSOAPFromPacket(REQUEST_PACKET,p);
	     }
	 }

         return this.request;
     }

     /**
      * Get the SOAP response object.
      *
      * @return the SOAP response object, which may be null.
      */
     public SOAPMessage getResponse() {

	 if (this.response == null) {

	     Object p = getPacket(RESPONSE_PACKET,false);

	     if (p != null && this.responseInPacket) {

		 // if packet is not null, get SOAP from packet
		 // responseInPacket set to false as side-effect
		 // since packet has been consumed.

		 this.response = getSOAPFromPacket(RESPONSE_PACKET,p);
	     }
	 }

         return this.response;
     }

     /**
      * Set the SOAP request object.
      *
      * @param request the SOAP response object.
      */
     public void setRequest(SOAPMessage request) {
	 Object p = getPacket(REQUEST_PACKET,false);
         if (p != null) {
	     this.requestInPacket = putSOAPInPacket(request,p);
	 }
	 this.request = request;
     }

     /**
      * Set the SOAP response object.
      *
      * @param response the SOAP response object.
      */
     public void setResponse(SOAPMessage response) {

	 // XXX previously, i.e. before wsit,
	 // if a response had already been set (it is non-null),
	 // this method would return with doing anything
	 // The original response would not be overwritten.
	 // that is no longer the case.

	 Object p = getPacket(RESPONSE_PACKET,false);
         if (p != null) {
	     this.responseInPacket = putSOAPInPacket(response,p);
	 }
	 this.response = response;
     }

     /**
      * Return the request Packet.
      *
      * @return the request Packet, which may be null.
      */
     public Object getRequestPacket() {
	 if (classLoadingException != null) {
	     throw new RuntimeException(classLoadingException);
	 }
	 return getPacket(REQUEST_PACKET,true);
     }

     /**
      * Return the response Packet.
      *
      * @return the response Packet, which may be null.
      */
     public Object getResponsePacket() {
	 if (classLoadingException != null) {
	     throw new RuntimeException(classLoadingException);
	 }
	 return getPacket(RESPONSE_PACKET,true);
     }

     /**
      * Set the request Packet.
      *
      * <p> has the side effect of resetting the SOAP request message.
      *
      * @param packet the request Packet
      */
     public void setRequestPacket(Object p) {
	 if (classLoadingException != null) {
	     throw new RuntimeException(classLoadingException);
	 }
	 if (p == null || p instanceof Packet) {
	     getMap().put(REQ_PACKET,p);
	     this.requestInPacket = (p == null ? false : true);
	     this.request = null;
	 } else {
	     throw new RuntimeException("argument is not packet");
	 }
     }

     /**
      * Set the response Packet.
      *
      * <p> has the side effect of resetting the SOAP response message.
      *
      * @param packet the response Packet
      */
     public void setResponsePacket(Object p) {
	 if (classLoadingException != null) {
	     throw new RuntimeException(classLoadingException);
	 }
	 if (p == null || p instanceof Packet) {
	     getMap().put(RES_PACKET,p);
	     this.responseInPacket = (p == null ? false : true);
	     this.response = null;
	 } else {
	     throw new RuntimeException("argument is not packet");
	 }
     }

     /**
      * Return the request Packet.
      *
      * @return the request Packet, which may be null.
      */
     private Object getPacket(boolean isRequestPacket, boolean putDesired) {

	 Object p = (this.infoMap == null ? 
		     null : this.infoMap.get
		     (isRequestPacket ? REQ_PACKET : RES_PACKET));

	 if (putDesired) {

	     SOAPMessage m = (isRequestPacket ? this.request : this.response);

	     if (p != null && m != null) {

		 // if SOAP request message has been read from packet
		 // we may need to set it back in the packet before
		 // returning the revised packet

		 if (isRequestPacket) {
		     if (!this.requestInPacket) {
			 this.requestInPacket = putSOAPInPacket(m,p);
		     }
		 } else {
		     if (!this.responseInPacket) {
			 this.responseInPacket = putSOAPInPacket(m,p);
		     }
		 }
	     }

	 }
	 return p;
     }

     private SOAPMessage getSOAPFromPacket(boolean isRequestPacket,Object p) {
	 if (classLoadingException != null) {
	     throw new RuntimeException(classLoadingException);
	 }
	 SOAPMessage s = null;
	 if (p instanceof Packet) {
	     Message m = ((Packet) p).getMessage();	
	     if (m != null) {
		 try {
		     s = m.readAsSOAPMessage();
		 }catch (Exception e) {
		     throw new RuntimeException(e);
		 }
	     } 
	 }

	 if (s != null) {
	     // System.out.println("SOAPAuthParam.getSOAPFromPacket:");
	     // printSOAP(s);
	     if (isRequestPacket) {
		 this.requestInPacket = false;
	     } else {
		 this.responseInPacket = false;
	     }
	 }
	 return s;
     }

     private boolean putSOAPInPacket(SOAPMessage m, Object p) {
	 if (m == null) {
	     ((Packet)p).setMessage(null);
	 } else {
	     Message msg = Messages.create(m);
	     ((Packet)p).setMessage(msg);
	 }
	 return true;
     }

     public static void printSOAP(SOAPMessage s) {
	try {
	    if (s != null) {
		s.writeTo(System.out);
		// System.out.println("\n");
	    } else {
		// System.out.println("SOAPMessage is empty");
	    } 
	} catch (Exception e) {
	    // System.out.println("SOAPAuthParam.printSOAP exception!");
	}
     }


}












