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
package org.glassfish.cluster.ssh.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class SSHUtilRedactTest {

    private static final String MASK = "********";

    @Test
    public void redactsValueFollowingSensitiveOption() {
        List<String> cmd = List.of("nadmin", "start-local-instance", "--windowspassword", "s3cret", "inst1");
        assertEquals(List.of("nadmin", "start-local-instance", "--windowspassword", MASK, "inst1"),
                SSHUtil.redactCommand(cmd));
    }

    @Test
    public void redactsEqualsForm() {
        List<String> cmd = List.of("nadmin", "--windowspassword=s3cret", "inst1");
        assertEquals(List.of("nadmin", "--windowspassword=" + MASK, "inst1"), SSHUtil.redactCommand(cmd));
    }

    @Test
    public void redactsValuesThatQuotingCannotHandle() {
        // Space plus both quote characters: FileUtils.quoteString cannot quote this safely,
        // so redaction must not depend on how the value is quoted.
        String nasty = "a'b\"c d e";
        List<String> redacted = SSHUtil.redactCommand(List.of("--windowspassword", nasty));
        assertEquals(List.of("--windowspassword", MASK), redacted);
        for (String arg : redacted) {
            assertFalse(arg.contains("c d"));
        }
    }

    @Test
    public void redactsValueThatLooksLikeAnOption() {
        List<String> cmd = List.of("--windowspassword", "--sync", "inst1");
        assertEquals(List.of("--windowspassword", MASK, "inst1"), SSHUtil.redactCommand(cmd));
    }

    @Test
    public void doesNotMatchOptionAsSubstring() {
        List<String> cmd = List.of("foo--windowspassword", "bar", "--windowspasswordfile", "x");
        assertEquals(cmd, SSHUtil.redactCommand(cmd));
    }

    @Test
    public void trailingSensitiveOptionWithoutValueIsKept() {
        List<String> cmd = List.of("nadmin", "--windowspassword");
        assertEquals(cmd, SSHUtil.redactCommand(cmd));
    }

    @Test
    public void doesNotModifyInput() {
        List<String> cmd = new ArrayList<>(Arrays.asList("--windowspassword", "s3cret"));
        SSHUtil.redactCommand(cmd);
        assertEquals(List.of("--windowspassword", "s3cret"), cmd);
    }

    @Test
    public void handlesNullAndEmpty() {
        assertNull(SSHUtil.redactCommand(null));
        assertEquals(List.of(), SSHUtil.redactCommand(List.of()));
        assertEquals(Arrays.asList("a", null), SSHUtil.redactCommand(Arrays.asList("a", null)));
    }
}
