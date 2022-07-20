/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.jaspic.config.factory;

import java.util.Collections;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.ServerAuth;
import jakarta.security.auth.message.config.ServerAuthContext;
import jakarta.security.auth.message.module.ServerAuthModule;

/**
 * The Server Authentication Context is an extra (required) indirection between the Application Server and the actual Server
 * Authentication Module (SAM). This can be used to encapsulate any number of SAMs and either select one at run-time, invoke
 * them all in order, etc.
 * <p>
 * Since this simple example only has a single SAM, we delegate directly to that one. Note that this {@link ServerAuthContext}
 * and the {@link ServerAuthModule} (SAM) share a common base interface: {@link ServerAuth}.
 * 
 * @author Arjan Tijms
 */
public class DefaultServerAuthContext implements ServerAuthContext {

    private final ServerAuthModule sam;

    public DefaultServerAuthContext(CallbackHandler handler, ServerAuthModule sam) throws AuthException {
        this.sam = sam;
        this.sam.initialize(null, null, handler, Collections.emptyMap());
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
        throws AuthException {
        return sam.validateRequest(messageInfo, clientSubject, serviceSubject);
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return sam.secureResponse(messageInfo, serviceSubject);
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        sam.cleanSubject(messageInfo, subject);
    }

}
