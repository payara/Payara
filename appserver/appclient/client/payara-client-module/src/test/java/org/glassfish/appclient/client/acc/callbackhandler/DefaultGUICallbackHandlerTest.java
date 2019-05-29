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

package org.glassfish.appclient.client.acc.callbackhandler;

import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tjquinn
 */
public class DefaultGUICallbackHandlerTest {

    public DefaultGUICallbackHandlerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * The following test is here just to give example code for driving
     * the callback mechanism.
     *
     * @throws java.lang.Exception
     */
    @Ignore
    @Test
    public void testHandle() throws Exception {
        run();
    }

    private void run() throws IOException, UnsupportedCallbackException {
        CallbackHandler ch = new DefaultGUICallbackHandler();
        ChoiceCallback choiceCB = new ChoiceCallback(
                    "Choose one",
                    new String[] {
                        "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh"},
                    0,
                    false);
        ConfirmationCallback confirmationCB = new ConfirmationCallback(
                    "Decide",
                    ConfirmationCallback.INFORMATION,
                    ConfirmationCallback.OK_CANCEL_OPTION,
                    ConfirmationCallback.OK);

        NameCallback nameCB = new NameCallback("Username", "who");
        PasswordCallback passwordCB = new PasswordCallback("Password", false);
        TextInputCallback textInCB = new TextInputCallback("Enter something interesting", "Good stuff to start with...");
        TextOutputCallback textOutCB = new TextOutputCallback(TextOutputCallback.WARNING,
                "Some fascinating text of great interest to the user goes here");
        LanguageCallback langCB = new LanguageCallback();
        Callback [] callbacks = new Callback[] {
            choiceCB, confirmationCB, nameCB, passwordCB, textInCB, textOutCB, langCB
        };

        ch.handle(callbacks);

        System.out.println("ChoiceCallback choice(s):");
        for (int index : choiceCB.getSelectedIndexes()) {
            if (index > 0) {
                System.out.println("  " + choiceCB.getChoices()[index]);
            } else {
                System.out.println("  Selection not made");
            }
        }


        System.out.print("ConfirmationCallback result: ");
        if (confirmationCB.getOptions() == null) {
            System.out.println(confirmationResultToString(confirmationCB.getSelectedIndex()));
        } else {
            System.out.println(confirmationCB.getOptions()[confirmationCB.getSelectedIndex()]);
        }

        System.out.println("NameCallback result: " + nameCB.getName());
        System.out.println("PasswordCallback result: " + new String(passwordCB.getPassword()));
        System.out.println("TextInputCallback result: " + textInCB.getText());
        System.out.println("LanguageCallback result: " + langCB.getLocale().getDisplayName());
    }

    private String confirmationResultToString(int result) {
        if (result == ConfirmationCallback.OK) {
            return "OK";
        }
        if (result == ConfirmationCallback.NO) {
            return "NO";
        }
        if (result == ConfirmationCallback.YES) {
            return "YES";
        }
        if (result == ConfirmationCallback.CANCEL) {
            return "CANCEL";
        }
        return "???";
    }}
