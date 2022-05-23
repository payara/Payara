/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2007, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package com.sun.ts.tests.common.webclient.handler;

/**
 * <PRE>
 * The HandlerManager is responsible for returning the appropriate handler
 * instance based on the provided value.
 */
public class HandlerFactory {

  /**
   * Content-Type handler name.
   */
  private static final String CONTENT_TYPE = "content-type";

  /**
   * Location handler name.
   */
  private static final String LOCATION = "location";

  /**
   * Set-Cookie handler name.
   */
  private static final String SET_COOKIE = "set-cookie";

  /**
   * ALLOW handler name.
   */
  private static final String ALLOW = "allow";

  /**
   * www-authenticate handler name.
   */
  private static final String WWW_AUTH = "www-authenticate";
  /*
   * public methods
   * ========================================================================
   */

  /**
   * Constructs a new HandlerManager instance
   */
  private HandlerFactory() {
  }

  /**
   * Returns the appropriate handler instance based on provided discriminate (a
   * header name).
   *
   * @param handlerName
   *          handler instance to obtain.
   */
  public static Handler getHandler(String handlerName) {
    if (CONTENT_TYPE.equals(handlerName.toLowerCase())) {
      return ContentTypeHandler.getInstance();
    } else if (LOCATION.equals(handlerName.toLowerCase())) {
      return LocationHandler.getInstance();
    } else if (SET_COOKIE.equals(handlerName.toLowerCase())) {
      return SetCookieHandler.getInstance();
    } else if (WWW_AUTH.equals(handlerName.toLowerCase())) {
      return WWWAuthenticateHandler.getInstance();
    } else if (ALLOW.equals(handlerName.toLowerCase())) {
      return ALLOWHandler.getInstance();
    } else {
      return DefaultHandler.getInstance();
    }
  }
}
