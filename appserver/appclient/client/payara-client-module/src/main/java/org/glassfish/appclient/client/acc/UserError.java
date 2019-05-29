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

package org.glassfish.appclient.client.acc;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;

/**
 *Represents any user error, such as an invalid combination of command options,
 *or specifying a non-existent JAR file.
 *<p>
 *Such errors should be user-correctable, provided the message is clear.
 *So no stack traces should be displayed when UserErrors are thrown.
 *
 * @author tjquinn
 */
public class UserError extends Throwable {

    /** Allows user to turn on stack traces for user errors - normally off */
    private static final String SHOW_STACK_TRACES_PROPERTY_NAME = 
            UserError.class.getPackage().getName() + ".showUserErrorStackTraces";
    
    /**
     * Creates a new UserError instance having formatted the message with the 
     * arguments provided.
     *@param message the message string, presumably containing argument placeholders
     *@param args 0 or more arguments for substitution for the placeholders in the message string
     *@return new UserError with message formatted as requested
     */
    public static UserError formatUserError(String message, String... args) {
        String formattedMessage = MessageFormat.format(message, (Object[]) args);
        UserError ue = new UserError(formattedMessage);
        return ue;
    }

    /** xmlMessage implementation showed the usage message after the error */
    private String usage = null;

    public UserError(String message) {
        super(message);
    }

    public UserError(String message, Throwable cause) {
        super(message, cause);
    }

    public UserError(Throwable cause) {
        super(cause);
    }

    /**
     *Sets whether or not the usage message should be displayed after the
     *error message is displayed to the user.
     *@param showUsage the new setting 
     */
    public void setUsage(String usage) {
        this.usage = usage;
    }

    /**
     *Displays the user error message, and any messages along the exception
     *chain, if any, and then exits.  If showUsage has been set to true, then
     *the usage message is displayed before exiting.
     *<p>
     *Only the messages, and not the stack traces, are shown because these are
     *user errors that should be user-correctable.  Stack traces are too
     *alarming and of minimal use to the user as he or she tries to understand
     *and fix the error.
     */
    public void displayAndExit() {
        display(System.err);
        System.exit(1);
    }

    private void display(final PrintStream ps) {
        for (Throwable t = this; t != null; t = t.getCause()) {
            ps.println(t.toString());
        }
        if (usage != null) {
            ps.println(usage);
        }
        if (Boolean.getBoolean(SHOW_STACK_TRACES_PROPERTY_NAME)) {
            printStackTrace(ps);
        }
    }

    public String messageForGUIDisplay() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        display(ps);
        return os.toString();
    }

}
