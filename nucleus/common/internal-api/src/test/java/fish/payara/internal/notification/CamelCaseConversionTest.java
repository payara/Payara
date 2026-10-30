/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.internal.notification;

import static fish.payara.internal.notification.NotifierUtils.convertToCamelCase;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CamelCaseConversionTest {

    @Test
    public void if_empty_expect_same() {
        assertEquals(null, convertToCamelCase(null));
        assertEquals("", convertToCamelCase(""));
    }

    @Test
    public void if_non_alphanumeric_string_expect_empty() {
        assertEquals("", convertToCamelCase("!\"£$%|^&*():;[]{}'@#~/?\\|"));
    }

    @Test
    public void if_lower_case_expect_same() {
        assertEquals("accountid", convertToCamelCase("accountid"));
    }

    @Test
    public void if_upper_case_expect_lower_case() {
        assertEquals("accountid", convertToCamelCase("ACCOUNTID"));
    }

    @Test
    public void if_numbers_expect_same() {
        assertEquals("a123", convertToCamelCase("a123"));
        assertEquals("a123b", convertToCamelCase("a123b"));
        assertEquals("123", convertToCamelCase("123"));
    }

    @Test
    public void if_leading_non_alphanumeric_characters_expect_removed() {
        assertEquals("abc", convertToCamelCase(" abc"));
        assertEquals("abc", convertToCamelCase("  abc"));
        assertEquals("abc", convertToCamelCase("-abc"));
        assertEquals("abc", convertToCamelCase("_-abc"));
        assertEquals("abc", convertToCamelCase(" -abc"));
    }

    @Test
    public void if_trailing_non_alphanumeric_characters_expect_removed() {
        assertEquals("abc", convertToCamelCase("abc "));
        assertEquals("abc", convertToCamelCase("abc  "));
        assertEquals("abc", convertToCamelCase("abc-"));
        assertEquals("abc", convertToCamelCase("abc-_"));
        assertEquals("abc", convertToCamelCase("abc- "));
    }

    @Test
    public void if_internal_non_alphanumeric_characters_expect_next_character_capitalised() {
        assertEquals("aBc", convertToCamelCase("a_bc"));
        assertEquals("aBc", convertToCamelCase("a-_bc"));
        assertEquals("aBc", convertToCamelCase("a bc"));
        assertEquals("aBc", convertToCamelCase("a  bc"));
    }

}
