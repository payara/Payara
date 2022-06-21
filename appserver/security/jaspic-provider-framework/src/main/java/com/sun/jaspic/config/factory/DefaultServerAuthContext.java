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

import jakarta.security.auth.message.config.ServerAuthContext;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.AuthStatus;
import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.AuthException;
import javax.security.auth.Subject;

public class DefaultServerAuthContext implements ServerAuthContext {

    private final ServerAuthModule serverAuthModule;

    public DefaultServerAuthContext(CallbackHandler handler, ServerAuthModule serverAuthModule) throws AuthException {
        this.serverAuthModule = serverAuthModule;
        serverAuthModule.initialize(null, null, handler, java.util.Collections.<String, Object> emptyMap());
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        return serverAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return serverAuthModule.secureResponse(messageInfo, serviceSubject);
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        serverAuthModule.cleanSubject(messageInfo, subject);
    }
}
