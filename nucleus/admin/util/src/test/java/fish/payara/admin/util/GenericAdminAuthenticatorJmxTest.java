/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.admin.util;

import com.sun.enterprise.admin.util.GenericAdminAuthenticator;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Tests that the JMX authentication entry point rejects credentials that could be used to
 * spoof the origin of a connection. None of these cases may reach the injected services.
 */
public class GenericAdminAuthenticatorJmxTest {

    private final GenericAdminAuthenticator authenticator = new GenericAdminAuthenticator();

    private void assertRejected(Object credentials) {
        try {
            authenticator.authenticate(credentials);
            fail("Credentials should have been rejected: " + credentials);
        } catch (SecurityException expected) {
            // expected
        }
    }

    @Test
    public void nullCredentialsAreRejected() {
        assertRejected(null);
    }

    @Test
    public void nonArrayCredentialsAreRejected() {
        assertRejected("");
        assertRejected("admin");
        assertRejected(new Object());
    }

    @Test
    public void emptyAndSingleElementArraysAreRejected() {
        assertRejected(new String[0]);
        assertRejected(new String[] {"admin"});
    }

    @Test
    public void arraysWithHostElementAreRejected() {
        assertRejected(new String[] {"admin", "password", "localhost"});
        assertRejected(new String[] {"", "", "localhost"});
    }

    @Test
    public void nullElementsAreRejected() {
        assertRejected(new String[] {null, "password"});
        assertRejected(new String[] {"admin", null});
    }

    @Test
    public void unknownClientHostIsRejected() {
        // Outside of an RMI call there is no client host, so authentication must fail closed.
        assertRejected(new String[] {"admin", "password"});
        assertRejected(new String[] {"", ""});
    }
}
