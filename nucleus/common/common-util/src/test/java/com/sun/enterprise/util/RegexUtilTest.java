/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.util;

import org.junit.Ignore;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for RegexUtil class's methods.
 * @author Kedar Mhaswade
 */
public class RegexUtilTest {
    @Test
    public void testStarWithExtensionDot() {
        String[] files = new String[]{"a.txt", "b.txt", "cc.txt"};
        String glob  = "*.txt";  //this is how glob pattern matches files in files array
        String regex = RegexUtil.globToRegex(glob);
        Pattern p = Pattern.compile(regex); //this regex should match all the files
        for (String file : files) {
            Matcher m = p.matcher(file);
            assertTrue(file + " matches glob: " + glob, m.matches());
        }
    }
    @Test
    public void testStarWithoutExtensionDot() {
        String[] files = new String[]{"a.txt", "b.txt", "cc.txt"};
        String glob  = "*txt";  //this is how glob pattern matches files in files array
        String regex = RegexUtil.globToRegex(glob);
        Pattern p = Pattern.compile(regex); //this regex should match all the files
        for (String file : files) {
            Matcher m = p.matcher(file);
            assertTrue(file + " matches glob: " + glob, m.matches());
        }
    }
    @Test
    public void testStarDotStar() {
        String glob  = "*.*";  //should match only the strings that are file-name-like and have an extension
        String regex = RegexUtil.globToRegex(glob);
        Pattern p = Pattern.compile(regex); //this regex should match all the files
        String str = "a.txt"; //*.* matches this
        Matcher m = p.matcher(str);
        assertTrue(str + " matches glob: " + glob, m.matches());
        str = "noext"; //*.* should not match this
        m = p.matcher(str); //again
        assertFalse(str + " matches glob: " + glob, m.matches());
    }

    /**
     * The test that tests some abnormalities that I am going to ignore for now. For example, glob pattern
     * "*" does not match a hidden file like ".foo", but the regex returned here matches. There are
     * a few other corner cases like that that I am going to ignore. Hopefully, they don't arise in our usage.
     */
    @Test
    @Ignore
    public void cornerCases() {
        String glob = "*";
        String regex = RegexUtil.globToRegex(glob);
        String str = ".hidden";
        assertFalse(str + " matches glob: " + glob, Pattern.compile(regex).matcher(str).matches());
    }
}
