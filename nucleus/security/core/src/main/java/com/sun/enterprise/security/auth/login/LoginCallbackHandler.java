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

package com.sun.enterprise.security.auth.login;

import java.io.*;
import javax.security.auth.callback.*;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.security.TextLoginDialog;
import com.sun.enterprise.security.GUILoginDialog;

/**
 * This is the default callback handler provided by the application
 * client container. The container tries to use the application specified 
 * callback handler (if provided). If there is no callback handler or if
 * the handler cannot be instantiated then this default handler is used.
 *
 * Note: User-defined Callback Handlers which intend to indicate cancel
 * status must extend this class and set the ThreadLocal cancelStatus.
 */
public class LoginCallbackHandler implements CallbackHandler 
{
    private boolean isGUI;
    private static final LocalStringManagerImpl localStrings =
    new LocalStringManagerImpl(LoginCallbackHandler.class);
    protected ThreadLocal<Boolean> cancelStatus = new ThreadLocal<Boolean>();

    /**
     * Check whether the authentication was cancelled by the user.
     * @return boolean indicating whether the authentication was cancelled.
     */
    public boolean getCancelStatus() {
        boolean cancelled = cancelStatus.get();
        cancelStatus.set(false);
        return cancelled;
    }

    public LoginCallbackHandler() {
        this(true);
    }

    public LoginCallbackHandler(boolean gui) {
        isGUI = gui;
        cancelStatus.set(false);
    }

    /**
     * This is the callback method called when authentication data is
     * required. It either pops up a dialog box to request authentication
     * data or use text input.
     * @param the callback object instances supported by the login module.
     */
    public void handle(Callback[] callbacks) throws IOException,
					UnsupportedCallbackException
    {
        if(isGUI) {
            String user = localStrings.getLocalString("login.user", "user");
	    new GUILoginDialog(user, callbacks);
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    cancelStatus.set(((NameCallback) callbacks[i]).getName() == null);
                    break;
                }
            }
        } else {
	    new TextLoginDialog(callbacks);
        }
    }
}

