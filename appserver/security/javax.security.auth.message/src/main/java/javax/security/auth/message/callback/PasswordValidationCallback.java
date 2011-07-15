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

package javax.security.auth.message.callback;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import java.util.Arrays;

/**
 * Callback for PasswordValidation. 
 * This callback may be used by an authentication module 
 * to employ the password validation facilities of its containing runtime.
 * This Callback would typically be called by a <code>ServerAuthModule</code>
 * during <code>validateRequest</code> processing.
 *
 * @version %I%, %G%
 */
public class PasswordValidationCallback implements Callback {

    private Subject subject;
    private String username;
    private char[] password;
    private boolean result = false;

    /**
     * Create a PasswordValidationCallback.
     *
     * @param subject The subject for authentication
     *
     * @param username The username to authenticate
     *
     * @param password tTe user's password, which may be null.
     */
    public PasswordValidationCallback(
            Subject subject, String username, char[] password) {
        this.subject = subject;
	this.username = username;
	if (password != null) {
	    this.password = (char[])password.clone();
	}
    }

    /**
     * Get the subject.
     *
     * @return The subject.
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * Get the username.
     *
     * @return The username.
     */
    public String getUsername() {
	return username;
    }

    /**
     * Get the password.
     *
     * <p> Note that this method returns a reference to the password.
     * If a clone of the array is created it is the caller's
     * responsibility to zero out the password information after
     * it is no longer needed.
     *
     * @return The password, which may be null.
     */
    public char[] getPassword() {
	return password;
    }

    /**
     * Clear the password.
     */
    public void clearPassword() {
	if (password != null) {
	    Arrays.fill(password, ' ');
	}
    }

    /**
     * Set the authentication result.
     *
     * @param result True if authentication succeeded, false otherwise
     */
    public void setResult(boolean result) {
	this.result = result;
    }

    /**
     * Get the authentication result.
     *
     * @return True if authentication succeeded, false otherwise
     */
    public boolean getResult() {
	return result;
    }
}
